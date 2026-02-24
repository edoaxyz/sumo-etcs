package sumoetcs.connection;

import org.eclipse.sumo.libsumo.POI;

import com.github.davidmoten.rtree2.RTree;
import com.github.davidmoten.rtree2.geometry.Geometries;
import com.github.davidmoten.rtree2.geometry.Point;


public class HandoverMap {
    private HandoverMap() {
        initTree();
    }

    public static HandoverMap getInstance() {
        if (instance == null) instance = new HandoverMap();
        return instance;
    }

    public String nextCell(double x, double y) {
        var result = cellsTree.nearest(Geometries.point(x, y), Double.MAX_VALUE, 1).iterator();
        return result.hasNext()  ? result.next().value() : null;
    }

    private void initTree() {
        cellsTree = RTree.create();
        for (var poiID : POI.getIDList()) {
            if (POI.getType(poiID).equals("cellRBC")) {
                var position = POI.getPosition(poiID);
                cellsTree.add(poiID, Geometries.point(position.getX(), position.getY()));
            }
        }
    }

    private static HandoverMap instance = null;
    private RTree<String, Point> cellsTree;
}
