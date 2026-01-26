package sumoetcs.interlocking;

import java.util.LinkedList;
import java.util.List;

import sumoetcs.Train;

public class Occupation {
    public Occupation(Train train, Segment currentSegment) {
        this.train = train;
        this.currentSegment = currentSegment;
        requestNextSegment(currentSegment);
    }

    public void requestNextSegment(Segment segment) {
        this.desiredSegment = segment;
        // First remove my occupations
        var newTracks = new LinkedList<Track>();
        for (var track: this.currentSegment.getTracks()) {
            track.removeOccupation(this);
        }
        // Add requested tracks
        Occupation nextOccupation = null;
        for (var reqTrack : segment.getTracks()) {
            newTracks.add(reqTrack);
            nextOccupation = reqTrack.findCeilingOccupation(this);
            if (nextOccupation != null) break;
        }
        Double realEndPosition = nextOccupation != null
                ? nextOccupation.getSegment().getStartPositionInTrack(newTracks.getLast())
                : Double.POSITIVE_INFINITY;
        if (realEndPosition == 0) {
            newTracks.removeLast();
            realEndPosition = newTracks.getLast().getLength() - 0.01;
        }
        var newSegment = new Segment(segment.getStartPosition(), realEndPosition, newTracks);
        this.currentSegment = newSegment;
        for (var newTrack : this.currentSegment.getTracks()) {
            newTrack.addOccupation(this);
        }
    }

    public void free() {
        for (var track : this.currentSegment.getTracks()) {
            track.removeOccupation(this);
        }
    }

    public Segment getSegment() {
        return currentSegment;
    }

    @Override
    public String toString() {
        return train.getId();
    };

    private Train train;
    private Segment currentSegment;
    private Segment desiredSegment;
}