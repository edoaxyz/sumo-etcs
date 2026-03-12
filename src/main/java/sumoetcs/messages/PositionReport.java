package sumoetcs.messages;

import java.util.List;

import sumoetcs.Train;

public class PositionReport extends Message {
    public PositionReport(IMessageUser sender, IMessageUser recipient, double backPosition, double frontPosition,
            List<String> occupiedEdges, List<String> nextEdges, boolean extendNext) {
        super(sender, recipient);
        this.backPosition = backPosition;
        this.frontPosition = frontPosition;
        this.occupiedEdges = occupiedEdges;
        this.nextEdges = nextEdges;
        this.extendNext = extendNext;
    }

    public double getBackPosition() {
        return backPosition;
    }

    public double getFrontPosition() {
        return frontPosition;
    }

    public List<String> getNextEdges() {
        return nextEdges;
    }

    public List<String> getOccupiedEdges() {
        return occupiedEdges;
    }

    public Train getTrain() {
        return (Train) this.sender;
    }

    public boolean getExtendNext() {
        return extendNext;
    }

    private double backPosition;
    private double frontPosition;
    private List<String> occupiedEdges;
    private List<String> nextEdges;
    private boolean extendNext;
}
