package org.sumoetcs.messages;

import java.util.List;

import org.sumoetcs.Train;

public class MovementAuthority extends Message {
    public MovementAuthority(IMessageUser sender, IMessageUser recipient, String edgeIdEOA, double positionEOA) {
        super(sender, recipient);
        this.edgeIdEOA = edgeIdEOA;
        this.positionEOA = positionEOA;
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
