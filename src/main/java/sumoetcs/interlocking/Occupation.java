package sumoetcs.interlocking;

import java.util.LinkedHashSet;
import java.util.LinkedList;

import sumoetcs.Consts;
import sumoetcs.Train;

public class Occupation {
    public Occupation(Train train, Segment currentSegment) {
        this.train = train;
        this.currentSegment = currentSegment;
        requestNextSegment(currentSegment);
    }

    public void requestNextSegment() {
        requestNextSegment(null);
    }

    public void requestNextSegment(Segment segment) {
        if (segment == null) segment = this.desiredSegment;
        else this.desiredSegment = segment;
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
                : newTracks.getLast().getLength();
        if (realEndPosition == 0) {
            newTracks.removeLast();
            realEndPosition = newTracks.getLast().getLength() - Consts.FLOAT_THRESHOLD;
        }
        if (segment.getLastTrack().equals(newTracks.getLast())) {
            realEndPosition = Math.min(segment.getEndPosition(), realEndPosition);
        }
        var newSegment = new Segment(segment.getStartPosition(), realEndPosition, newTracks, true);
        boolean isTailDiff = !newSegment.tailEquals(this.currentSegment);
        this.currentSegment = newSegment;
        for (var newTrack : this.currentSegment.getTracks()) {
            newTrack.addOccupation(this);
        }
        if (isTailDiff) updateTailObservers();
        if (nextOccupation != null) nextOccupation.tailObservers.add(this);
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

    private void updateTailObservers() {
        var obs = new LinkedList<>(tailObservers);
        while (obs.size() > 0) {
            Train t = obs.removeFirst().train;
            t.getRbc().sendMovementAuthority(t);
        }
    }

    private Train train;
    private Segment currentSegment;
    private Segment desiredSegment;

    private LinkedHashSet<Occupation> tailObservers = new LinkedHashSet<>();
}