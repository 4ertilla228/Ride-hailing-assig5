package main.java.ridehailing.facade;


import main.java.ridehailing.subsystemA.*;
import main.java.ridehailing.subsystemB.*;
import main.java.ridehailing.subsystemC.*;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Второй, специализированный Фасад для администратора, инспекции состояния на диске,
 * восстановления зависших после перезапуска цепочек и ручной компенсации.
 */
public class RideAdminFacade {
    private final DispatchGateway gatewayA;
    private final BillingGateway gatewayB;
    private final SettlementGateway gatewayC;
    private final ChainStateGateway chainGateway;
    private final RideBookingFacade bookingFacade;

    public RideAdminFacade(Path storageDir, RideBookingFacade bookingFacade) {
        this.gatewayA = new DispatchGateway(storageDir);
        this.gatewayB = new BillingGateway(storageDir);
        this.gatewayC = new SettlementGateway(storageDir);
        this.chainGateway = new ChainStateGateway(storageDir);
        this.bookingFacade = bookingFacade;
    }

    /**
     * Обнаруживает незавершенные цепочки (PENDING_B, PENDING_C) после сбоя/перезапуска программы
     * и автоматически откатывает их на основе данных с диска.
     */
    public List<RideChainState> recoverInterruptedChainsAfterRestart() {
        List<RideChainState> recovered = new ArrayList<>();
        for (RideChainState state : chainGateway.findAll()) {
            if (state.getStatus() == ChainStageStatus.PENDING_B
                    || state.getStatus() == ChainStageStatus.PENDING_C) {
                RideChainState result = bookingFacade.compensateFromDisk(
                        state.getRideId(), "System restart recovery of interrupted chain");
                recovered.add(result);
            }
        }
        return recovered;
    }

    /**
     * Формирует компактную строку состояния (в стиле Domain-Specific Language из Лекции 6)
     * Например: "A:DRIVER_ASSIGNED|B:ISSUED|C:PAID_AND_RATED|CHAIN:COMPLETED"
     */
    public String getCompactRideSignature(String rideId) {
        String a = gatewayA.findByRideId(rideId).map(r -> r.getStatus().name()).orElse("NONE");
        String b = gatewayB.findByRideId(rideId).map(r -> r.getStatus().name()).orElse("NONE");
        String c = gatewayC.findByRideId(rideId).map(r -> r.getStatus().name()).orElse("NONE");
        String chain = chainGateway.findByRideId(rideId).map(r -> r.getStatus().name()).orElse("NONE");
        return String.format("A:%s|B:%s|C:%s|CHAIN:%s", a, b, c, chain);
    }

    public String inspectRideDetails(String rideId) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== Inspection for Ride: ").append(rideId).append(" ===\n");
        sb.append("  Chain : ").append(chainGateway.findByRideId(rideId).map(Object::toString).orElse("Not found")).append("\n");
        sb.append("  Sub A : ").append(gatewayA.findByRideId(rideId).map(Object::toString).orElse("Not found")).append("\n");
        sb.append("  Sub B : ").append(gatewayB.findByRideId(rideId).map(Object::toString).orElse("Not found")).append("\n");
        sb.append("  Sub C : ").append(gatewayC.findByRideId(rideId).map(Object::toString).orElse("Not found")).append("\n");
        return sb.toString();
    }

    public List<RideChainState> listAllChainStates() {
        return chainGateway.findAll();
    }

    public RideChainState manualAdminRollback(String rideId, String adminReason) {
        return bookingFacade.compensateFromDisk(rideId, "Manual Admin Rollback: " + adminReason);
    }

    public void resetAllStores() {
        gatewayA.clearAll();
        gatewayB.clearAll();
        gatewayC.clearAll();
        chainGateway.clearAll();
    }
}