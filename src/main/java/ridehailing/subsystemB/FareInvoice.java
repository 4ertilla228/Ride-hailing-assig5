package main.java.ridehailing.subsystemB;

public class FareInvoice {
    private final String rideId;
    private final double totalFare;
    private final double surgeMultiplier;
    private String receiptCode;
    private BillingStatus status;

    public FareInvoice(String rideId, double totalFare, double surgeMultiplier,
                       String receiptCode, BillingStatus status) {
        this.rideId = rideId;
        this.totalFare = totalFare;
        this.surgeMultiplier = surgeMultiplier;
        this.receiptCode = receiptCode;
        this.status = status;
    }

    public void issueReceipt(String generatedReceipt) {
        if (this.status != BillingStatus.DRAFT) {
            throw new IllegalStateException("Cannot issue receipt when invoice is in status " + this.status);
        }
        this.receiptCode = generatedReceipt;
        this.status = BillingStatus.ISSUED;
    }

    public void voidInvoice() {
        if (this.status != BillingStatus.ISSUED && this.status != BillingStatus.DRAFT) {
            throw new IllegalStateException("Cannot void invoice from status " + this.status);
        }
        this.status = BillingStatus.VOIDED;
    }

    public String getRideId() { return rideId; }
    public double getTotalFare() { return totalFare; }
    public double getSurgeMultiplier() { return surgeMultiplier; }
    public String getReceiptCode() { return receiptCode; }
    public BillingStatus getStatus() { return status; }

    @Override
    public String toString() {
        return String.format("Invoice[ride=%s, fare=%.2f, surge=%.1fx, receipt=%s, status=%s]",
                rideId, totalFare, surgeMultiplier, receiptCode, status);
    }
}