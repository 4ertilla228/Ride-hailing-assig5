package main.java.ridehailing;

import main.java.ridehailing.facade.*;
import main.java.ridehailing.subsystemA.*;
import main.java.ridehailing.subsystemB.*;
import main.java.ridehailing.subsystemC.*;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Scanner;

public class Main {
    private static final Path STORAGE_DIR = Paths.get("./ride_data");

    public static void main(String[] args) {
        RideBookingFacade bookingFacade = new RideBookingFacade(STORAGE_DIR);
        RideAdminFacade adminFacade = new RideAdminFacade(STORAGE_DIR, bookingFacade);
        Scanner scanner = new Scanner(System.in);

        System.out.println("====================================================");
        System.out.println("RIDE-HAILING FACADE LIVE DEMO (PERSISTENT CLI)");
        System.out.println("Storage directory: " + STORAGE_DIR.toAbsolutePath());
        System.out.println("====================================================");

        while (true) {
            System.out.println("\nSelect an action:");
            System.out.println("1. [Счастливый путь] Забронируйте действительную поездку (A -> B -> C пройдено успешно)");
            System.out.println("2. [Сбой в B] Цена скачка превышает бюджет пользователя (A фиксируется -> B терпит неудачу -> A компенсируется)");
            System.out.println("3. [Ошибка при C] Карта отклонена при оплате (фиксация A & B -> сбой C -> B, затем компенсация A)");
            System.out.println("4. [Сбой на] Недопустимом маршруте / в зоне нет водителей (сбой перед любой фиксацией)");
            System.out.println("5. [Имитация сбоя и перезапуск] Оставьте ride в PENDING_C на диске, затем восстановите через Admin Facade");
            System.out.println("6. [Проверить состояние диска] Просмотреть все сохраненные поездки по A, B, C");
            System.out.println("7. [Демонстрация прямой подсистемы] Докажите, что подсистемы A, B, C работают независимо друг от друга без использования фасада");
            System.out.println("8. [Сбросить хранилище] Очистить все CSV-файлы на диске");
            System.out.println("0. Exit");
            System.out.print("Vash sibor: ");

            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> runScenario(bookingFacade, adminFacade, new RideRequest(
                        "RIDE-101", "RIDER-ALAN", "Astana_Mega", "Astana_Expo",
                        8.0, 1.0, 5000.0, "VISA-4242"));

                case "2" -> runScenario(bookingFacade, adminFacade, new RideRequest(
                        "RIDE-202", "RIDER-ALAN", "Astana_Mega", "Astana_Airport",
                        15.0, 3.0, 2500.0, "VISA-4242")); // Cena = (500 + 2250)*3 = 8250 > 2500 budget!

                case "3" -> runScenario(bookingFacade, adminFacade, new RideRequest(
                        "RIDE-303", "RIDER-ALAN", "Astana_Station", "Astana_Center",
                        6.0, 1.2, 5000.0, "DECLINED_CARD_99")); // Fails at C!

                case "4" -> runScenario(bookingFacade, adminFacade, new RideRequest(
                        "RIDE-404", "RIDER-ALAN", "NO_DRIVERS_ZONE", "Astana_Center",
                        5.0, 1.0, 5000.0, "VISA-4242")); // Fails at A!

                case "5" -> simulateMidChainCrashAndRecover(adminFacade);

                case "6" -> {
                    List<RideChainState> all = adminFacade.listAllChainStates();
                    if (all.isEmpty()) {
                        System.out.println("No persisted rides on disk.");
                    } else {
                        for (RideChainState s : all) {
                            System.out.println(adminFacade.inspectRideDetails(s.getRideId()));
                            System.out.println("  DSL Signature: " + adminFacade.getCompactRideSignature(s.getRideId()));
                        }
                    }
                }

                case "7" -> runStandaloneSubsystemsDemo();

                case "8" -> {
                    adminFacade.resetAllStores();
                    System.out.println("All disk files in ./ride_data have been cleared.");
                }

                case "0" -> {
                    System.out.println("Выходит. Данные остаются сохраненными на диске в файле ./ride_data");
                    return;
                }
                default -> System.out.println("Unknown option.");
            }
        }
    }

    private static void runScenario(RideBookingFacade bookingFacade, RideAdminFacade adminFacade, RideRequest req) {
        try {
            RideChainState state = bookingFacade.bookRide(req);
            System.out.println("SUCCESS: " + state);
        } catch (RideBookingException e) {
            System.out.println("HANDLED EXCEPTION: " + e.getMessage());
            System.out.println("FINAL CHAIN STATUS: " + e.getFinalChainStatus());
        }
        System.out.println(adminFacade.inspectRideDetails(req.getRideId()));
        System.out.println("Compact State: " + adminFacade.getCompactRideSignature(req.getRideId()));
    }

    private static void simulateMidChainCrashAndRecover(RideAdminFacade adminFacade) {
        String crashRideId = "RIDE-CRASH-505";
        System.out.println("Simulating power loss right after A and B committed (status = PENDING_C on disk)...");
        DispatchGateway gwA = new DispatchGateway(STORAGE_DIR);
        BillingGateway gwB = new BillingGateway(STORAGE_DIR);
        ChainStateGateway gwChain = new ChainStateGateway(STORAGE_DIR);

        DispatchReservation res = new DispatchReservation(crashRideId, "RIDER-CRASH", "NONE", 10.0, 25, DispatchStatus.REQUESTED);
        res.assignDriver("DRV-777");
        gwA.save(res);

        FareInvoice inv = new FareInvoice(crashRideId, 2000.0, 1.0, "NONE", BillingStatus.DRAFT);
        inv.issueReceipt("RCPT-CRASH-2000");
        gwB.save(inv);

        gwChain.save(new RideChainState(crashRideId, "RIDER-CRASH", ChainStageStatus.PENDING_C, "Crashed before C"));

        System.out.println("Before recovery: " + adminFacade.getCompactRideSignature(crashRideId));
        System.out.println("Running AdminFacade.recoverInterruptedChainsAfterRestart()...");
        adminFacade.recoverInterruptedChainsAfterRestart();
        System.out.println("After recovery : " + adminFacade.getCompactRideSignature(crashRideId));
        System.out.println(adminFacade.inspectRideDetails(crashRideId));
    }

    private static void runStandaloneSubsystemsDemo() {
        System.out.println("--- Proving Subsystems A, B, C work directly without Facade ---");
        GpsAndDriverLogic logicA = new GpsAndDriverLogic();
        System.out.println("Direct A ETA calculation: " + logicA.calculateRouteEtaMinutes("PointA", "PointB", 4.0) + " min");

        PricingAndReceiptLogic logicB = new PricingAndReceiptLogic();
        System.out.println("Direct B Fare calculation: " + logicB.calculateFare(4.0, 1.0, 3000.0) + " KZT");

        PaymentAndRatingLogic logicC = new PaymentAndRatingLogic();
        System.out.println("Direct C Rating prompt: " + logicC.initializeRatingPrompt("STANDALONE-1", "USER-1"));
    }
}