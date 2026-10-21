package main.java.ridehailing.facade;

public class RideBookingException extends RuntimeException {
    private final ChainStageStatus finalChainStatus;

    public RideBookingException(String message, ChainStageStatus finalChainStatus, Throwable cause) {
        super(message, cause);
        this.finalChainStatus = finalChainStatus;
    }

    public ChainStageStatus getFinalChainStatus() {
        return finalChainStatus;
    }
}