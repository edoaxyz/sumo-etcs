package sumoetcs.interlocking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.eclipse.sumo.libsumo.Edge;
import org.eclipse.sumo.libsumo.Lane;

import sumoetcs.Consts;

public class Track {
    protected Track(List<String> edges, List<List<String>> incomingConnections) {
        this.edges = new ArrayList<>(edges);
        this.incomingConnections = new ArrayList<>();
        this.incomingInternalEdges = new HashSet<>();
        if (incomingConnections != null)
            this.incomingConnections.addAll(incomingConnections);
        for (var inConn : this.incomingConnections) {
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
        } catch (NumberFormatException e) {
        }
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
        if (this.incomingInternalEdges.contains(edgeId))
            return 0;
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

    @Override
    public String toString() {
        return "[" + String.join(", ", edges) + "]";
    };

    protected void addOccupation(Occupation occ) {
        occupations.add(occ);
    }

    protected void removeOccupation(Occupation occ) {
        occupations.remove(occ);
    }

    protected Occupation findCeilingOccupation(Occupation occ) {
        var ceil = occupations.ceiling(occ);
        return ceil != occ ? ceil : null;
    }

    private List<String> edges;
    private List<List<String>> incomingConnections;
    private Set<String> incomingInternalEdges;
    private Map<String, Integer> edgeIndexes = new HashMap<>();
    private Map<String, Double> lengths = new HashMap<>();
    private TreeMap<Double, String> orderedLengths = new TreeMap<>();
    private double length = 0;
    private double blockLength = -1;

    private TreeSet<Occupation> occupations = new TreeSet<>(
            (occ1, occ2) -> {
                int diff = (int) ((occ1.getSegment().getStartPositionInTrack(this)
                        - occ2.getSegment().getStartPositionInTrack(this)) / Consts.FLOAT_THRESHOLD);
                if (diff == 0)
                    diff = occ1 == occ2 ? 0 : -1;
                return diff;
            });
}
