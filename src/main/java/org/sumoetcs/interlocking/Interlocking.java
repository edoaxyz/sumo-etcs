package org.sumoetcs.interlocking;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeSet;

import org.sumoetcs.Train;

public class Interlocking {
    public class Occupation {
        private Occupation(Interlocking interlocking, Train train, List<Net.Track> tracks, double startPos, double endPos) {
            this.interlocking = interlocking;
            this.train = train;
            this.tracks = new LinkedList<>(tracks);
            updateStart(tracks.getFirst(), startPos);
            updateEndPosition(endPos);
            occupationByTrain.put(train, this);
        }

        public void updateStart(Net.Track startingTrack, double startPos) {
            while (startingTrack != this.tracks.getFirst()) {
                interlocking.occupations.get(this.tracks.removeFirst()).remove(this);
            }
            updateStartPosition(startPos);
        }

        public void requestNextEOA(List<Net.Track> requestTracks, Double endPosition) {
            // First remove my occupations and build the new one with the previous until the
            // first requested is met
            var newTracks = new LinkedList<Net.Track>();
            for (var track : tracks) {
                interlocking.occupations.get(track).remove(this);
                if (newTracks.size() == 0 || requestTracks.getFirst() != newTracks.getFirst()) {
                    newTracks.add(track);
                }
            }
            newTracks.removeLast();
            // Add requested tracks
            for (var reqTrack : requestTracks) {
                newTracks.add(reqTrack);
                if (requestTracks.getLast() == reqTrack || interlocking.occupations.get(reqTrack).size() > 0) {
                    break;
                }
            }
            Double realEndPosition = Math.min(
                    interlocking.occupations.get(requestTracks.getLast()).first().getStartPositionInTrack(requestTracks.getLast()),
                    endPosition);
            if (realEndPosition == 0) {
                newTracks.removeLast();
                realEndPosition = newTracks.getLast().getLength();
            }
            this.tracks = newTracks;
            updateEndPosition(realEndPosition);
            for (var newTrack : newTracks) {
                interlocking.occupations.get(newTrack).add(this);
            }
        }

        public void free() {
            for (var track: tracks) {
                interlocking.occupations.get(track).remove(this);
            }
            occupationByTrain.remove(train);
        }

        public Double getStartPositionInTrack(Net.Track track) {
            if (track == tracks.getFirst())
                return startPosition;
            return 0.;
        }

        public Double getEndPositionInTrack(Net.Track track) {
            if (track == tracks.getLast())
                return endPosition;
            return track.getLength();
        }


        public Net.Track getFirstTrack() {
            return tracks.getFirst();
        }

        public Net.Track getLastTrack() {
            return tracks.getLast();
        }

        private void updateStartPosition(double position) {
            Net.Track t = tracks.getFirst();
            this.startPosition = t.getBlockLength() > 0 ? Math.floor(position / t.getBlockLength()) * t.getBlockLength()
                    : position;
        }

        private void updateEndPosition(double position) {
            Net.Track t = tracks.getFirst();
            this.endPosition = t.getBlockLength() > 0 ? Math.ceil(position / t.getBlockLength()) * t.getBlockLength()
                    : position;
        }

        private Train train;
        private List<Net.Track> tracks;
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

    public Occupation createOccupation(Train train, List<Net.Track> tracks, double startPos, double endPos) {
        return new Occupation(this, train, tracks, startPos, endPos);
    }

    private Map<Net.Track, TreeSet<Occupation>> occupations = new HashMap<>();
    private Map<Train, Occupation> occupationByTrain = new HashMap<>();
    private Net net;

}
