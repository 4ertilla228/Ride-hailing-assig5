package main.java.ridehailing.subsystemC;

public class PaymentAndRatingLogic {

    //  проверка карты и списание средств
    public void processPayment(String paymentToken, double amount) {
        if (paymentToken == null || paymentToken.isBlank()) {
            throw new IllegalArgumentException("Payment token is missing");
        }
        if (paymentToken.startsWith("DECLINED") || paymentToken.equalsIgnoreCase("INSUFFICIENT_FUNDS")) {
            throw new IllegalStateException("Payment processor rejected card: " + paymentToken);
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Payment amount must be positive");
        }
    }

    // создание билета оценки поездки и начисление бонусов
    public String initializeRatingPrompt(String rideId, String riderId) {
        return "RATE-" + rideId + "-BY-" + riderId;
    }
}