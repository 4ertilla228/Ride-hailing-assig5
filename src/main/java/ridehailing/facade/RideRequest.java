package main.java.ridehailing.facade;

public class RideRequest {
    private final String rideId;
    private final String riderId;
    private final String pickupLocation;
    private final String dropoffLocation;
    private final double distanceKm;
    private final double surgeMultiplier;
    private final double maxRiderBudget;
    private final String paymentToken;

    public RideRequest(String rideId, String riderId, String pickupLocation, String dropoffLocation,
                       double distanceKm, double surgeMultiplier, double maxRiderBudget, String paymentToken) {
        this.rideId = rideId;
        this.riderId = riderId;
        this.pickupLocation = pickupLocation;
        this.dropoffLocation = dropoffLocation;
        this.distanceKm = distanceKm;
        this.surgeMultiplier = surgeMultiplier;
        this.maxRiderBudget = maxRiderBudget;
        this.paymentToken = paymentToken;
    }

    public String getRideId() { return rideId; }
    public String getRiderId() { return riderId; }
    public String getPickupLocation() { return pickupLocation; }
    public String getDropoffLocation() { return dropoffLocation; }
    public double getDistanceKm() { return distanceKm; }
    public double getSurgeMultiplier() { return surgeMultiplier; }
    public double getMaxRiderBudget() { return maxRiderBudget; }
    public String getPaymentToken() { return paymentToken; }
}