package main.java.ridehailing.subsystemA;

public class DispatchReservation {
    private final String rideId;
    private final String riderId;
    private String driverId;
    private final double distanceKm;
    private final int estimatedMinutes;
    private DispatchStatus status;

    public DispatchReservation(String rideId, String riderId, String driverId,
    double distanceKm, int estimatedMinutes, DispatchStatus status)
    {this.rideId = rideId;
        this.riderId = riderId;
        this.driverId = driverId;
        this.distanceKm = distanceKm;
        this.estimatedMinutes = estimatedMinutes;
        this.status = status;
    }

    // cтрогий контроль переходов состояний внутри самой сущности
    public void assignDriver(String matchedDriverId) {
        if (this.status != DispatchStatus.REQUESTED) {
            throw new IllegalStateException("Cannot assign driver when status is " + this.status);
        }
        this.driverId = matchedDriverId;
        this.status = DispatchStatus.DRIVER_ASSIGNED;
    }

    public void cancelReservation() {
        if (this.status != DispatchStatus.DRIVER_ASSIGNED && this.status != DispatchStatus.REQUESTED) {
            throw new IllegalStateException("Cannot cancel reservation from status " + this.status);
        }
        this.status = DispatchStatus.CANCELLED;
    }

    public String getRideId() { return rideId; }
    public String getRiderId() { return riderId; }
    public String getDriverId() { return driverId; }
    public double getDistanceKm() { return distanceKm; }
    public int getEstimatedMinutes() { return estimatedMinutes; }
    public DispatchStatus getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("Dispatch[ride=%s, rider=%s, driver=%s, dist=%.1fkm, eta=%dmin, status=%s]",
                rideId, riderId, driverId, distanceKm, estimatedMinutes, status);
    }
}