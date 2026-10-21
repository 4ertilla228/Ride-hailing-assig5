package main.java.ridehailing.subsystemC;

public class SettlementRecord {
    private final String rideId;
    private final String paymentToken;
    private final double chargedAmount;
    private String ratingTicket;
    private SettlementStatus status;

    public SettlementRecord(String rideId, String paymentToken, double chargedAmount,
                            String ratingTicket, SettlementStatus status) {
        this.rideId = rideId;
        this.paymentToken = paymentToken;
        this.chargedAmount = chargedAmount;
        this.ratingTicket = ratingTicket;
        this.status = status;
    }

    public void confirmPaymentAndRating(String generatedRatingTicket) {
        if (this.status != SettlementStatus.PENDING) {
            throw new IllegalStateException("Cannot confirm settlement from status " + this.status);
        }
        this.ratingTicket = generatedRatingTicket;
        this.status = SettlementStatus.PAID_AND_RATED;
    }

    public void markFailed() {
        if (this.status != SettlementStatus.PENDING) {
            throw new IllegalStateException("Cannot mark failed from status " + this.status);
        }
        this.status = SettlementStatus.PAYMENT_FAILED;
    }

    public String getRideId() { return rideId; }
    public String getPaymentToken() { return paymentToken; }
    public double getChargedAmount() { return chargedAmount; }
    public String getRatingTicket() { return ratingTicket; }
    public SettlementStatus getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("Settlement[ride=%s, token=%s, amount=%.2f, rating=%s, status=%s]",
                rideId, paymentToken, chargedAmount, ratingTicket, status);
    }
}