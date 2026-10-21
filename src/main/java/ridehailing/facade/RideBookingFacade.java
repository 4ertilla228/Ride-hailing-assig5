package main.java.ridehailing.facade;

import main.java.ridehailing.subsystemA.*;
import main.java.ridehailing.subsystemB.*;
import main.java.ridehailing.subsystemC.*;

import java.nio.file.Path;

/**
 Основной Фасад: управляет цепочкой A -> B -> C и выполняет автоматическую компенсацию
 на основе реально сохраненных на диске данных.
 */
public class RideBookingFacade {
    private final GpsAndDriverLogic logicA;
    private final DispatchGateway gatewayA;

    private final PricingAndReceiptLogic logicB;
    private final BillingGateway gatewayB;

    private final PaymentAndRatingLogic logicC;
    private final SettlementGateway gatewayC;

    private final ChainStateGateway chainGateway;

    public RideBookingFacade(Path storageDir) {
        this.logicA = new GpsAndDriverLogic();
        this.gatewayA = new DispatchGateway(storageDir);
        this.logicB = new PricingAndReceiptLogic();
        this.gatewayB = new BillingGateway(storageDir);
        this.logicC = new PaymentAndRatingLogic();
        this.gatewayC = new SettlementGateway(storageDir);
        this.chainGateway = new ChainStateGateway(storageDir);
    }

    /**
     Единая операция для клиента: проводит поездку через подсистемы A -> B -> C.
     */
    public RideChainState bookRide(RideRequest request) {
        String rideId = request.getRideId();

        //  ШАГ A: Прием и резервирование водителя
        try {
            int eta = logicA.calculateRouteEtaMinutes(
                    request.getPickupLocation(), request.getDropoffLocation(), request.getDistanceKm());
            String driverId = logicA.matchAvailableDriver(request.getPickupLocation(), request.getDistanceKm());

            DispatchReservation res = new DispatchReservation(
                    rideId, request.getRiderId(), "NONE", request.getDistanceKm(), eta, DispatchStatus.REQUESTED);
            res.assignDriver(driverId);
            gatewayA.save(res);

            chainGateway.save(new RideChainState(rideId, request.getRiderId(),
                    ChainStageStatus.PENDING_B, "Step A committed: Driver " + driverId + " assigned"));
        } catch (Exception e) {
            RideChainState rejected = new RideChainState(rideId, request.getRiderId(),
                    ChainStageStatus.REJECTED_AT_A, "Failed at Step A: " + e.getMessage());
            chainGateway.save(rejected);
            throw new RideBookingException("Step A failed before commit: " + e.getMessage(),
                    ChainStageStatus.REJECTED_AT_A, e);
        }

        //  ШАГ B: Расчет цены и выпуск чека
        double calculatedFare;
        try {
            DispatchReservation persistedA = gatewayA.findByRideId(rideId)
                    .orElseThrow(() -> new IllegalStateException("Persisted state A missing on disk"));

            calculatedFare = logicB.calculateFare(
                    persistedA.getDistanceKm(), request.getSurgeMultiplier(), request.getMaxRiderBudget());
            String receiptCode = logicB.generateReceipt(rideId, calculatedFare);

            FareInvoice invoice = new FareInvoice(
                    rideId, calculatedFare, request.getSurgeMultiplier(), "NONE", BillingStatus.DRAFT);
            invoice.issueReceipt(receiptCode);
            gatewayB.save(invoice);

            chainGateway.save(new RideChainState(rideId, request.getRiderId(),
                    ChainStageStatus.PENDING_C, "Step B committed: Invoice issued for " + calculatedFare));
        } catch (Exception e) {
            // Сбой на шаге B после коммита шага A -> Компенсируем ТОЛЬКО шаг A
            compensateFromDisk(rideId, e.getMessage());
            throw new RideBookingException("Step B failed; compensated A: " + e.getMessage(),
                    ChainStageStatus.COMPENSATED_A, e);
        }

        // ШАГ C: Оплата и инициализация рейтинга
        try {
            FareInvoice persistedB = gatewayB.findByRideId(rideId)
                    .orElseThrow(() -> new IllegalStateException("Persisted state B missing on disk"));

            SettlementRecord settlement = new SettlementRecord(
                    rideId, request.getPaymentToken(), persistedB.getTotalFare(), "NONE", SettlementStatus.PENDING);
            gatewayC.save(settlement);

            logicC.processPayment(request.getPaymentToken(), persistedB.getTotalFare());
            String ratingTicket = logicC.initializeRatingPrompt(rideId, request.getRiderId());

            settlement.confirmPaymentAndRating(ratingTicket);
            gatewayC.save(settlement);

            RideChainState completed = new RideChainState(rideId, request.getRiderId(),
                    ChainStageStatus.COMPLETED, "Ride completed successfully");
            chainGateway.save(completed);
            return completed;
        } catch (Exception e) {
            // Отмечаем сбой оплаты в C на диске и запускаем двухшаговый откат: сначала B, затем A
            gatewayC.findByRideId(rideId).ifPresent(rec -> {
                if (rec.getStatus() == SettlementStatus.PENDING) {
                    rec.markFailed();
                    gatewayC.save(rec);
                }
            });
            compensateFromDisk(rideId, e.getMessage());
            throw new RideBookingException("Step C failed; compensated B then A: " + e.getMessage(),
                    ChainStageStatus.COMPENSATED_B_AND_A, e);
        }
    }

    /**
     * Читает фактическое состояние с диска (пережившее возможный перезапуск)
     * и откатывает нужную глубину цепочки (только A или B -> A).
     */
    public RideChainState compensateFromDisk(String rideId, String reason) {
        RideChainState chainState = chainGateway.findByRideId(rideId)
                .orElseThrow(() -> new IllegalArgumentException("No persisted chain found for rideId: " + rideId));

        if (chainState.getStatus() == ChainStageStatus.PENDING_B) {
            // Компенсация глубины 1: Откатываем только A
            DispatchReservation resA = gatewayA.findByRideId(rideId)
                    .orElseThrow(() -> new IllegalStateException("Dispatch record missing for " + rideId));
            if (resA.getStatus() == DispatchStatus.DRIVER_ASSIGNED) {
                resA.cancelReservation();
                gatewayA.save(resA);
            }
            chainState.updateStatus(ChainStageStatus.COMPENSATED_A, "Rolled back A due to: " + reason);
            chainGateway.save(chainState);

        } else if (chainState.getStatus() == ChainStageStatus.PENDING_C
                || chainState.getStatus() == ChainStageStatus.COMPLETED) {
            // Компенсация глубины 2: Строго сначала B, затем A!
            FareInvoice invB = gatewayB.findByRideId(rideId)
                    .orElseThrow(() -> new IllegalStateException("Billing record missing for " + rideId));
            if (invB.getStatus() == BillingStatus.ISSUED) {
                invB.voidInvoice();
                gatewayB.save(invB);
            }

            DispatchReservation resA = gatewayA.findByRideId(rideId)
                    .orElseThrow(() -> new IllegalStateException("Dispatch record missing for " + rideId));
            if (resA.getStatus() == DispatchStatus.DRIVER_ASSIGNED) {
                resA.cancelReservation();
                gatewayA.save(resA);
            }
            chainState.updateStatus(ChainStageStatus.COMPENSATED_B_AND_A, "Rolled back B then A due to: " + reason);
            chainGateway.save(chainState);
        }
        return chainState;
    }
}