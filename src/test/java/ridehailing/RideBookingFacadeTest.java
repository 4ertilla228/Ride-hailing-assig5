package test.java.ridehailing;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import main.java.ridehailing.facade.*;
import main.java.ridehailing.subsystemA.*;
import main.java.ridehailing.subsystemB.*;
import main.java.ridehailing.subsystemC.*;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

public class RideBookingFacadeTest {

    @TempDir
    Path tempStorageDir; // гарантирует first

    private RideBookingFacade bookingFacade;
    private RideAdminFacade adminFacade;

    @BeforeEach
    void setUp() {
        bookingFacade = new RideBookingFacade(tempStorageDir);
        adminFacade = new RideAdminFacade(tempStorageDir, bookingFacade);
    }

    // domain specific language

    private RideRequest givenStandardRide(String rideId, double surge, double maxBudget, String cardToken) {
        return new RideRequest(rideId, "RIDER-01", "Downtown", "Airport", 10.0, surge, maxBudget, cardToken);
    }

    private void assertPersistedSignature(String rideId, String expectedSignature) {
        assertEquals(expectedSignature, adminFacade.getCompactRideSignature(rideId));
    }

    // тесты осн. сценариев и компенсации

    @Test
    @DisplayName("1. Happy Path: Full A -> B -> C chain commits and persists across all stores")
    void happyPathCompletesAllThreeStages() {
        RideRequest req = givenStandardRide("R-100", 1.0, 5000.0, "VISA-OK");

        RideChainState result = bookingFacade.bookRide(req);

        assertEquals(ChainStageStatus.COMPLETED, result.getStatus());
        assertPersistedSignature("R-100",
                "A:DRIVER_ASSIGNED|B:ISSUED|C:PAID_AND_RATED|CHAIN:COMPLETED");
    }

    @Test
    @DisplayName("2. Depth-1 Compensation: B fails after A committed -> Facade compensates A alone")
    void stepBFailureCompensatesSubsystemAOnly() {
        // Дистанция 10 км, surge 3.0 -> Цена (500 + 1500)*3 = 6000 > бюджета 2500
        RideRequest req = givenStandardRide("R-200", 3.0, 2500.0, "VISA-OK");

        RideBookingException ex = assertThrows(RideBookingException.class, () -> bookingFacade.bookRide(req));

        assertEquals(ChainStageStatus.COMPENSATED_A, ex.getFinalChainStatus());
        assertPersistedSignature("R-200",
                "A:CANCELLED|B:NONE|C:NONE|CHAIN:COMPENSATED_A");
    }

    @Test
    @DisplayName("3. Depth-2 Compensation: C fails after A and B committed -> Facade compensates B then A")
    void stepCFailureCompensatesSubsystemBThenSubsystemA() {
        RideRequest req = givenStandardRide("R-300", 1.0, 5000.0, "DECLINED_CARD");

        RideBookingException ex = assertThrows(RideBookingException.class, () -> bookingFacade.bookRide(req));

        assertEquals(ChainStageStatus.COMPENSATED_B_AND_A, ex.getFinalChainStatus());
        assertPersistedSignature("R-300",
                "A:CANCELLED|B:VOIDED|C:PAYMENT_FAILED|CHAIN:COMPENSATED_B_AND_A");
    }

    @Test
    @DisplayName("4. Exception at Step A: Fails before any subsystem commits")
    void stepAFailureLeavesSubsystemsUncommitted() {
        RideRequest invalidReq = new RideRequest(
                "R-400", "RIDER-01", "NO_DRIVERS_ZONE", "Airport", 10.0, 1.0, 5000.0, "VISA-OK");

        RideBookingException ex = assertThrows(RideBookingException.class, () -> bookingFacade.bookRide(invalidReq));

        assertEquals(ChainStageStatus.REJECTED_AT_A, ex.getFinalChainStatus());
        assertPersistedSignature("R-400",
                "A:NONE|B:NONE|C:NONE|CHAIN:REJECTED_AT_A");
    }

    @Test
    @DisplayName("5. Persistence Reload Test: Simulates application restart mid-chain (PENDING_C) and recovers from disk")
    void persistenceReloadMidChainRecoversCorrectlyOnNewFacadeInstance() {
        // 1. Записываем состояние напрямую на диск (имитация падения питания после шага B)
        DispatchGateway gwA = new DispatchGateway(tempStorageDir);
        BillingGateway gwB = new BillingGateway(tempStorageDir);
        ChainStateGateway gwChain = new ChainStateGateway(tempStorageDir);

        DispatchReservation res = new DispatchReservation("R-RELOAD", "RIDER-99", "NONE", 8.0, 20, DispatchStatus.REQUESTED);
        res.assignDriver("DRV-555");
        gwA.save(res);

        FareInvoice inv = new FareInvoice("R-RELOAD", 1700.0, 1.0, "NONE", BillingStatus.DRAFT);
        inv.issueReceipt("RCPT-R-RELOAD-1700");
        gwB.save(inv);

        gwChain.save(new RideChainState("R-RELOAD", "RIDER-99", ChainStageStatus.PENDING_C, "Interrupted before C"));

        // 2. Создаем совершенно НОВЫЕ экземпляры фасадов, указывающие на ту же папку на диске (симуляция перезапуска JVM)
        RideBookingFacade restartedBookingFacade = new RideBookingFacade(tempStorageDir);
        RideAdminFacade restartedAdminFacade = new RideAdminFacade(tempStorageDir, restartedBookingFacade);

        assertEquals("A:DRIVER_ASSIGNED|B:ISSUED|C:NONE|CHAIN:PENDING_C",
                restartedAdminFacade.getCompactRideSignature("R-RELOAD"));

        // 3. Восстанавливаем прерванные цепочки с диска
        restartedAdminFacade.recoverInterruptedChainsAfterRestart();

        assertEquals("A:CANCELLED|B:VOIDED|C:NONE|CHAIN:COMPENSATED_B_AND_A",
                restartedAdminFacade.getCompactRideSignature("R-RELOAD"));
    }

    @Test
    @DisplayName("6. Subsystems enforce legal state transitions independently of Facade")
    void subsystemsEnforceStateTransitionsAndWorkDirectly() {
        DispatchReservation res = new DispatchReservation("R-DIRECT", "U1", "NONE", 5.0, 12, DispatchStatus.REQUESTED);
        res.assignDriver("DRV-101");
        res.cancelReservation();
        // Повторное назначение водителя отмененной поездке должно выбрасывать IllegalStateException
        assertThrows(IllegalStateException.class, () -> res.assignDriver("DRV-202"));

        FareInvoice inv = new FareInvoice("R-DIRECT", 1250.0, 1.0, "NONE", BillingStatus.DRAFT);
        inv.issueReceipt("RCPT-1");
        inv.voidInvoice();
        assertThrows(IllegalStateException.class, () -> inv.issueReceipt("RCPT-2"));

        SettlementRecord rec = new SettlementRecord("R-DIRECT", "VISA", 1250.0, "NONE", SettlementStatus.PENDING);
        rec.confirmPaymentAndRating("RATE-1");
        assertThrows(IllegalStateException.class, rec::markFailed);
    }
}