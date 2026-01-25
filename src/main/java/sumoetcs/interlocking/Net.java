package sumoetcs.interlocking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.sumo.libtraci.Edge;
import org.eclipse.sumo.libtraci.Lane;

import sumoetcs.sumo.SumoManager;

public class Net {

    public Net() {
    }

    public void load() {
        Map<String, List<String>> edgeToTrack = new HashMap<>();
        Map<String, List<List<String>>> previousInternal = new HashMap<>();
        var doNotMerge = new HashSet<String>();
        for (var edgeId : Edge.getIDList()) {
            if (edgeToTrack.containsKey(edgeId)
                    || !SumoManager.ALLOWED_CLASSES.containsAll(Lane.getAllowed(edgeId + "_0"))
                    || edgeId.startsWith(":"))
                continue;
            List<String> edges = new ArrayList<>();
            List<String> internalEdges = new ArrayList<>();
            while (edgeId != null) {
                if (edgeToTrack.containsKey(edgeId)) {
                    if (edgeToTrack.get(edgeId).getFirst().equals(edgeId)) {
                        // Union (LINEAR COMPLEXITY!!)
                        if (doNotMerge.contains(edgeId)) {
                            previousInternal.get(edgeId).add(internalEdges);
                            break;
                        }
                        for (var toRemove : edgeToTrack.get(edgeId)) {
                            edgeToTrack.remove(toRemove);
                        }
                    } else {
                        // Split
                        var foundList = edgeToTrack.get(edgeId);
                        var splitIndex = foundList.indexOf(edgeId);
                        var split = List.of(new ArrayList<String>(foundList.subList(0, splitIndex)),
                                new ArrayList<String>(foundList.subList(splitIndex, foundList.size())));
                        var firstEdge = split.get(1).getFirst();
                        previousInternal.putIfAbsent(firstEdge, new ArrayList<>());
                        previousInternal.get(firstEdge).add(internalEdges);
                        var startSplitIndex = splitIndex;
                        while (startSplitIndex > 1 && foundList.get(startSplitIndex - 1).startsWith(":"))
                            startSplitIndex--;
                        previousInternal.get(firstEdge)
                                .add(new ArrayList<>(foundList.subList(startSplitIndex, splitIndex)));
                        doNotMerge.add(firstEdge);
                        for (var splitArray : split) {
                            for (var edge : splitArray) {
                                edgeToTrack.put(edge, splitArray);
                            }
                        }
                        break;
                    }
                }
                if (!edgeId.startsWith(":"))
                    internalEdges = new ArrayList<>();
                edges.add(edgeId);
                edgeToTrack.put(edgeId, edges);
                var nextEdges = Lane.getLinks(edgeId + "_0");
                if (nextEdges.size() != 1)
                    break;
                edgeId = nextEdges.get(0).getApproachedInternal();
                if (edgeId.equals("")) {
                    edgeId = Lane.getEdgeID(nextEdges.get(0).getApproachedLane());
                } else if (edgeId != null) {
                    edgeId = Lane.getEdgeID(edgeId);
                    internalEdges.add(edgeId);
                }
                ;
            }
        }
        // Remove intermediate starting tracks
        for (var toEdge : previousInternal.values()) {
            for (var internalConnection : toEdge) {
                var edgeList = edgeToTrack.get(internalConnection.getFirst());
                edgeList.subList(edgeList.indexOf(internalConnection.getFirst()), edgeList.size()).clear();
            }
        }
        // Create final objects
        for (var track : edgeToTrack.values()) {
            if (!this.tracks.containsKey(track.getFirst())) {
                Track t = new Track(track, previousInternal.get(track.getFirst()));
                for (var edge : t.getEdges())
                    this.tracks.put(edge, t);
                for (var edge : t.getIncomingEdges())
                    this.tracks.put(edge, t);
            }
        }
    }

    public Map<String, Track> getTracks() {
        return new HashMap<>(this.tracks);
    }

    public Entry<Track, Double> toTrack(String edgeId, double positionInEdge) {
        Track t = tracks.get(edgeId);
        return Map.entry(t, t.getEdgePosition(edgeId, positionInEdge < 0) + positionInEdge);
    }

    public Entry<String, Double> toEdge(Track track, double positionInTrack) {
        String edge = track.getLowestEdge(positionInTrack);
        return Map.entry(edge, positionInTrack - track.getEdgePosition(edge, false));
    }

    private Map<String, Track> tracks = new HashMap<>();
}
