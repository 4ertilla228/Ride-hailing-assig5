package main.java.ridehailing.facade;

public class RideChainState {
    private final String rideId;
    private final String riderId;
    private ChainStageStatus status;
    private String note;

    public RideChainState(String rideId, String riderId, ChainStageStatus status, String note) {
        this.rideId = rideId;
        this.riderId = riderId;
        this.status = status;
        this.note = note;
    }

    public void updateStatus(ChainStageStatus newStatus, String newNote) {
        this.status = newStatus;
        this.note = newNote;
    }

    public String getRideId() { return rideId; }
    public String getRiderId() { return riderId; }
    public ChainStageStatus getStatus() { return status; }
    public String getNote() { return note; }

    @Override
    public String toString() {
        return String.format("ChainState[ride=%s, rider=%s, status=%s, note='%s']",
                rideId, riderId, status, note);
    }
}