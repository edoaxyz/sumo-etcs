package sumoetcs.messages;

import sumoetcs.Train;

public class MovementAuthority extends Message {
    public MovementAuthority(IMessageUser sender, IMessageUser recipient, String edgeIdEOA, double positionEOA) {
        super(sender, recipient);
        this.edgeIdEOA = edgeIdEOA;
        this.positionEOA = positionEOA;
    }

    public boolean equalsPosition(MovementAuthority ma) {
        return ma != null && edgeIdEOA == ma.edgeIdEOA && positionEOA == ma.positionEOA;
    }

    public String getEdgeIdEOA() {
        return edgeIdEOA;
    }

    public double getPositionEOA() {
        return positionEOA;
    }

    public Train getTrain() {
        return (Train) this.sender;
    }

    private String edgeIdEOA;
    private double positionEOA;
}
