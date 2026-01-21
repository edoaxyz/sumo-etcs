package sumoetcs.interlocking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import sumoetcs.Train;

import java.util.TreeSet;

public class Interlocking {
    public class Occupation {
        private Occupation(Interlocking interlocking, Train train, List<Track> tracks, double startPos,
                double endPos) {
            this.interlocking = interlocking;
            this.train = train;
            this.tracks = new LinkedList<>(tracks);
            requestNextEOA(tracks, startPos, endPos);
            occupationByTrain.put(train, this);
        }

        public void requestNextEOA(List<Track> requestTracks, Double startPosition, Double endPosition) {
            // First remove my occupations 
            var newTracks = new LinkedList<Track>();
            for (var track : tracks) {
                interlocking.occupations.get(track).remove(this);
            }
            // Add requested tracks
            for (var reqTrack : requestTracks) {
                newTracks.add(reqTrack);
                if (interlocking.occupations.get(reqTrack).ceiling(this) != null) {
                    break;
                }
            }
            Occupation nextOccupation = interlocking.occupations.get(newTracks.getLast()).ceiling(this);
            Double realEndPosition = nextOccupation != null
                    ? Math.min(nextOccupation.getStartPositionInTrack(newTracks.getLast()), endPosition)
                    : Double.POSITIVE_INFINITY;
            if (realEndPosition == 0) {
                newTracks.removeLast();
                realEndPosition = newTracks.getLast().getLength() - 0.1;
            }
            this.tracks = newTracks;
            updateStartPosition(startPosition);
            updateEndPosition(realEndPosition);
            for (var newTrack : newTracks) {
                interlocking.occupations.get(newTrack).add(this);
            }
        }

        public void free() {
            for (var track : tracks) {
                interlocking.occupations.get(track).remove(this);
            }
            occupationByTrain.remove(train);
        }

        public Double getStartPositionInTrack(Track track) {
            if (track == tracks.getFirst())
                return startPosition;
            return 0.;
        }

        public Double getEndPositionInTrack(Track track) {
            if (track == tracks.getLast())
                return endPosition;
            return track.getLength();
        }

        public Track getFirstTrack() {
            return tracks.getFirst();
        }

        public Track getLastTrack() {
            return tracks.getLast();
        }

        private void updateStartPosition(double position) {
            Track t = tracks.getFirst();
            this.startPosition = t.getBlockLength() > 0 ? Math.floor(position / t.getBlockLength()) * t.getBlockLength()
                    : position;
        }

        private void updateEndPosition(double position) {
            Track t = tracks.getFirst();
            this.endPosition = t.getBlockLength() > 0 ? Math.ceil(position / t.getBlockLength()) * t.getBlockLength()
                    : position;
        }

        private Train train;
        private List<Track> tracks;
        private double startPosition;
        private double endPosition;
        private Interlocking interlocking;
    }

    public Interlocking(Net net) {
        this.net = net;
    }

    public void load() {
        for (var track : net.getTracks().values()) {
            occupations.put(track, new TreeSet<>(
                    (occ1, occ2) -> (int) ((occ1.getStartPositionInTrack(track) - occ2.getStartPositionInTrack(track))
                            * 1000000)));
        }
    }

    public Occupation getOccupation(Train t) {
        return occupationByTrain.get(t);
    }

    public Occupation createOccupation(Train train, List<Track> tracks, double startPos, double endPos) {
        return new Occupation(this, train, tracks, startPos, endPos);
    }

    private Map<Track, TreeSet<Occupation>> occupations = new HashMap<>();
    private Map<Train, Occupation> occupationByTrain = new HashMap<>();
    private Net net;

}
