package sumoetcs.interlocking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.eclipse.sumo.libtraci.Edge;
import org.eclipse.sumo.libtraci.Lane;

public class Track {
    protected Track(List<String> edges, List<List<String>> incomingConnections) {
        this.edges = new ArrayList<>(edges);
        this.incomingConnections = new ArrayList<>();
        this.incomingInternalEdges = new HashSet<>();
        if (incomingConnections != null) this.incomingConnections.addAll(incomingConnections); 
        for (var inConn: this.incomingConnections) {
            this.incomingInternalEdges.addAll(inConn);
        }

        int i = 0;
        for (var edge : edges) {
            double edgeLength = Lane.getLength(edge + "_0");
            edgeIndexes.put(edge, i);
            lengths.put(edge, length);
            orderedLengths.put(length, edge);
            length += edgeLength;
            i++;
        }

        try {
            blockLength = Double.parseDouble(Edge.getParameter(edges.getFirst(), "block-length"));
        } catch (NumberFormatException e) {}
    }

    public List<String> getEdges() {
        return new ArrayList<>(edges);
    }

    public List<String> getIncomingEdges() {
        return new ArrayList<>(incomingInternalEdges);
    }

    public double getLength() {
        return length;
    }

    public double getBlockLength() {
        return blockLength;
    }

    public double getEdgePosition(String edgeId, boolean end) {
        if (this.incomingInternalEdges.contains(edgeId)) return 0;
        if (end) {
            int indexNextEdge = edgeIndexes.get(edgeId) + 1;
            if (edges.size() == indexNextEdge) {
                return length;
            }
            return lengths.get(edges.get(indexNextEdge));
        }
        return lengths.get(edgeId);
    }

    public String getLowestEdge(double positionInTrack) {
        return orderedLengths.floorEntry(positionInTrack).getValue();
    }

    private List<String> edges;
    private List<List<String>> incomingConnections;
    private Set<String> incomingInternalEdges;
    private Map<String, Integer> edgeIndexes = new HashMap<>();
    private Map<String, Double> lengths = new HashMap<>();
    private TreeMap<Double, String> orderedLengths = new TreeMap<>();
    private double length = 0;
    private double blockLength = -1;
}
