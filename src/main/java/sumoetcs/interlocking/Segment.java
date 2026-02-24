package sumoetcs.interlocking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import sumoetcs.Consts;

public class Segment {
    public Segment(Double startPosition, Double endPosition, List<Track> tracks) {
        Track t = tracks.getFirst();
        this.startPos = t.getBlockLength() > 0 ? Math.floor(startPosition / t.getBlockLength()) * t.getBlockLength()
                : startPosition;
        this.endPos = Double.isFinite(endPosition) ? Math.min(t.getBlockLength() > 0 ? Math.ceil(endPosition / t.getBlockLength()) * t.getBlockLength()
                : endPosition, tracks.getLast().getLength() - 10e-6) : endPosition;
        this.tracks = new ArrayList<>(tracks);
        for (int i = 0; i < this.tracks.size(); i++) {
            trackIndexes.put(this.tracks.get(i), i);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (obj.getClass() != this.getClass())
            return false;
        final Segment other = (Segment) obj;
        if (!(startPos == other.startPos && endPos == other.endPos && tracks.size() == other.tracks.size()))
            return false;
        for (int i = 0; i < tracks.size(); i++) {
            if (!tracks.get(i).equals(other.tracks.get(i)))
                return false;
        }
        return true;
    }

    public Segment getHeadDiff(Segment other) {
        Segment longest = this, shortest = other;
        if (!trackIndexes.containsKey(other.tracks.getFirst())) {
            if (other.trackIndexes.containsKey(this.tracks.getFirst())) {
                longest = other;
                shortest = this;
            } else
                return null;
        }
        if (longest.trackIndexes.get(shortest.tracks.getFirst()) == 0 && longest.startPos > shortest.startPos) {
            var temp = longest;
            longest = shortest;
            shortest = temp;
        }
        var endPos = shortest.startPos;
        var tracks = longest.tracks.subList(0, longest.trackIndexes.get(shortest.tracks.getFirst()) + 1);
        if (endPos == 0) {
            tracks = tracks.subList(0, tracks.size() - 1);
            endPos = tracks.getLast().getLength();
        }
        if (startPos == endPos && tracks.size() == 1)
            return new Segment(0., 0., List.of());
        return new Segment(longest.startPos, endPos, tracks);
    }

    public Segment getTailDiff(Segment other) {
        Segment longest = this, shortest = other;
        if (!trackIndexes.containsKey(other.tracks.getLast())) {
            if (other.trackIndexes.containsKey(this.tracks.getLast())) {
                longest = other;
                shortest = this;
            } else
                return null;
        }
        if (longest.trackIndexes.get(shortest.tracks.getLast()) == (longest.tracks.size() - 1)
                && longest.endPos < shortest.endPos) {
            var temp = longest;
            longest = shortest;
            shortest = temp;
        }
        var startPos = shortest.endPos;
        var tracks = longest.tracks.subList(longest.trackIndexes.get(shortest.tracks.getLast()),
                longest.trackIndexes.size());
        if (startPos == tracks.getLast().getLength()) {
            if (tracks.size() > 1) {
                tracks = tracks.subList(1, tracks.size());
                startPos = 0.;
            } else {
                return new Segment(0., 0., List.of());
            }
        }
        return new Segment(startPos, longest.endPos, tracks);
    }

    public boolean tailEquals(Segment s) {
        return tracks.getFirst().equals(s.tracks.getFirst()) && Math.abs(s.startPos - startPos) < Consts.FLOAT_THRESHOLD;
    }

    public boolean headEquals(Segment s) {
        return tracks.getLast().equals(s.tracks.getLast()) && Math.abs(s.endPos - endPos) < Consts.FLOAT_THRESHOLD;
    }

    public double getStartPositionInTrack(Track track) {
        if (track == tracks.getFirst())
            return startPos;
        return 0.;
    }

    public double getEndPositionInTrack(Track track) {
        if (track == tracks.getLast())
            return endPos;
        return track.getLength();
    }

    public Track getFirstTrack() {
        return tracks.getFirst();
    }

    public Track getLastTrack() {
        return tracks.getLast();
    }

    public List<Track> getTracks() {
        return new ArrayList<>(tracks);
    }

    public double getStartPosition() {
        return startPos;
    }

    public double getEndPosition() {
        return endPos;
    }

    private Double startPos;
    private Double endPos;
    private List<Track> tracks;
    private Map<Track, Integer> trackIndexes = new HashMap<>();
}
