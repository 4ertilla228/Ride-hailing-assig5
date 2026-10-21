package main.java.ridehailing.subsystemB;

public class PricingAndReceiptLogic {
    private static final double BASE_FEE = 500.0;
    private static final double PER_KM_RATE = 150.0;

    public double calculateFare(double distanceKm, double surgeMultiplier, double maxRiderBudget) {
        if (surgeMultiplier < 1.0 || surgeMultiplier > 5.0) {
            throw new IllegalArgumentException("Invalid surge multiplier: " + surgeMultiplier);
        }
        double rawFare = (BASE_FEE + (distanceKm * PER_KM_RATE)) * surgeMultiplier;
        double roundedFare = Math.round(rawFare * 100.0) / 100.0;

        if (roundedFare > maxRiderBudget) {
            throw new IllegalStateException(String.format(
                    "Pricing rejected: Calculated fare (%.2f) exceeds rider max budget (%.2f)",
                    roundedFare, maxRiderBudget));
        }
        return roundedFare;
    }

    // формирует фискальный код чека
    public String generateReceipt(String rideId, double totalFare) {
        return "RCPT-" + rideId + "-" + (int) totalFare;
    }
}