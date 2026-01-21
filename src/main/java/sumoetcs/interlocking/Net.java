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
        var doNotMerge = new HashSet<String>();
        for (var edgeId : Edge.getIDList()) {
            if (edgeToTrack.containsKey(edgeId) || !SumoManager.ALLOWED_CLASSES.containsAll(Lane.getAllowed(edgeId + "_0")) || edgeId.startsWith(":"))
                continue;
            List<String> edges = new ArrayList<>();
            while (edgeId != null) {
                if (edgeToTrack.containsKey(edgeId)) {
                    if (edgeToTrack.get(edgeId).getFirst().equals(edgeId)) {
                        // Union (LINEAR COMPLEXITY!!)
                        if (doNotMerge.contains(edgeId)) break;
                        for (var toRemove : edgeToTrack.get(edgeId)) {
                            edgeToTrack.remove(toRemove);
                        }
                    } else {
                        // Split
                        var foundList = edgeToTrack.get(edgeId);
                        var splitIndex = foundList.indexOf(edgeId);
                        var split = List.of(new ArrayList<String>(foundList.subList(0, splitIndex)), new ArrayList<String>(foundList.subList(splitIndex, foundList.size())));
                        doNotMerge.add(split.get(1).getFirst());
                        for (var splitArray : split) {
                            for (var edge: splitArray) {
                                edgeToTrack.put(edge, splitArray);
                            }
                        }
                        break;
                    }
                }
                edges.add(edgeId);
                edgeToTrack.put(edgeId, edges);
                var nextEdges = Lane.getLinks(edgeId + "_0");
                edgeId = nextEdges.size() == 1 ? Lane.getEdgeID(nextEdges.get(0).getApproachedLane()) : null;
            }
        }
        for (var track: edgeToTrack.values()) {
            Track t = new Track(track);
            for (var edge: t.getEdges()) {
                if (!this.tracks.containsKey(edge)) this.tracks.put(edge, t);
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
