package org.sumoetcs.interlocking;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.sumo.libtraci.Edge;
import org.eclipse.sumo.libtraci.Lane;
import org.sumoetcs.sumo.SumoManager;

public class Net {
    public class Track {
        Track(String firstEdge) {
            add(firstEdge);
            try {
                blockLength = Double.parseDouble(Edge.getParameter(firstEdge, "block-length"));
            } catch (NumberFormatException e) {}
        }

        private void add(String edgeId) {
            this.edges.add(edgeId);
            this.lengths.put(edgeId, length);
            this.orderedLengths.put(length, edgeId);
            length += Lane.getLength(edgeId + "_0");
        }

        private void merge(Track track) {
            this.edges.addAll(track.edges);
            this.length += track.length;
        }

        public List<String> getEdges() {
            return new LinkedList<>(edges);
        }

        public double getLength() {
            return length;
        }

        public double getBlockLength() {
            return blockLength;
        }

        private LinkedList<String> edges = new LinkedList<>();
        private Map<String, Double> lengths = new HashMap<>();
        private TreeMap<Double, String> orderedLengths = new TreeMap<>();
        private double length = 0;
        private double blockLength = -1;
    }

    public Net() {
    }

    public void load() {
        List<String> edgesToVisit = new LinkedList<>(Edge.getIDList());
        while (edgesToVisit.size() > 0) {
            String edgeId = edgesToVisit.removeFirst();
            if (tracks.containsKey(edgeId))
                continue;
            if (!SumoManager.ALLOWED_CLASSES.containsAll(Lane.getAllowed(edgeId + "_0")))
                continue;
            Track track = new Track(edgeId);
            tracks.put(edgeId, track);
            var nextEdges = Lane.getLinks(edgeId + "_0");
            while (nextEdges.size() == 1) {
                edgeId = Lane.getEdgeID(nextEdges.get(0).getApproachedLane());
                if (tracks.containsKey(edgeId))
                    break;
                track.add(edgeId);
                tracks.put(edgeId, track);
            }
            if (nextEdges.size() != 1) {
                Track toMerge = tracks.get(edgeId);
                track.merge(toMerge);
                for (var toMergeEdge : toMerge.edges) {
                    tracks.put(toMergeEdge, track);
                }
            }
        }
    }

    public Map<String, Track> getTracks() {
        return new HashMap<>(this.tracks);
    }

    public Entry<Track, Double> toTrack(String edgeId, double positionInEdge) {
        Track t = tracks.get(edgeId);
        return Map.entry(t, t.lengths.get(edgeId) + positionInEdge);
    }

    public Entry<String, Double> toEdge(Track track, double positionInTrack) {
        var edgeEntry = track.orderedLengths.floorEntry(positionInTrack);
        return Map.entry(edgeEntry.getValue(), positionInTrack - edgeEntry.getKey());
    }

    private Map<String, Track> tracks = new HashMap<>();
}
