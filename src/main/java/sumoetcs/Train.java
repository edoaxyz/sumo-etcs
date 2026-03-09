package sumoetcs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.eclipse.sumo.libsumo.Lane;
import org.eclipse.sumo.libsumo.Simulation;
import org.eclipse.sumo.libsumo.StringVector;
import org.eclipse.sumo.libsumo.Vehicle;
import org.eclipse.sumo.libsumo.VehicleType;

import sumoetcs.connection.BurstedConnection;
import sumoetcs.connection.Connection;
import sumoetcs.connection.HandoverConnection;
import sumoetcs.connection.IConnectionObserver;
import sumoetcs.connection.LostConnection;
import sumoetcs.messages.IMessageUser;
import sumoetcs.messages.Message;
import sumoetcs.messages.MovementAuthority;
import sumoetcs.messages.PositionReport;
import sumoetcs.sumo.IStepTrigger;
import sumoetcs.sumo.SumoManager;

public class Train implements IStepTrigger, IMessageUser, IConnectionObserver {

    public Train(String id, RBC rbc, SumoManager sumoManager) {
        this.id = id;
        this.typeId = Vehicle.getTypeID(id);
        this.rbc = rbc;

        this.positionReportInterval = Integer.parseInt(VehicleType.getParameter(typeId, "positionReportInterval"));
        this.delayInMean = Float.parseFloat(VehicleType.getParameter(typeId, "delayInMean"));
        this.delayInStd = Float.parseFloat(VehicleType.getParameter(typeId, "delayInStd"));
        this.delayOutMean = Float.parseFloat(VehicleType.getParameter(typeId, "delayOutMean"));
        this.delayOutStd = Float.parseFloat(VehicleType.getParameter(typeId, "delayOutStd"));
        this.safetyMargin = Float.parseFloat(VehicleType.getParameter(typeId, "safetyMargin"));
        this.retry = VehicleType.getParameter(typeId, "retry").equals("true");

        this.length = Vehicle.getLength(id) + this.safetyMargin;
        Vehicle.setSpeed(id, 0);
        Vehicle.setMinGap(id, 0);
        this.sumoManager = sumoManager;
        connections = Arrays.asList(new BurstedConnection(sumoManager, typeId),
                new HandoverConnection(sumoManager, typeId), new LostConnection(sumoManager, typeId));
        for (var conn : connections) {
            conn.addObserver(this);
        }

        sendPositionReport();
        sumoManager.stepSubscribe(this, true);
        sumoManager.stepSubscribeIn(this, positionReportInterval, false);
        Vehicle.setParameter(id, "dyn_lastEOA", "0");
    }

    @Override
    public void receive(Message message) {
        if (message instanceof MovementAuthority) {
            MovementAuthority ma = (MovementAuthority) message;
            if (lastEOA != null) {
                if (ma.getTime() <= lastEOA.getTime())
                    return;
                if (lastEOA.getPositionEOA() != Double.POSITIVE_INFINITY && lastEOA.getPositionEOA() >= 0
                        && !ma.equalsPosition(lastEOA))
                    Vehicle.setStop(id, lastEOA.getEdgeIdEOA(), lastEOA.getPositionEOA(), 0, 0);
            } else {
                if (ma.getPositionEOA() > length || !ma.getEdgeIdEOA().equals(Vehicle.getRoadID(id)))
                    Vehicle.setSpeed(id, -1);
                else
                    return;
            }
            if (ma.getPositionEOA() != Double.POSITIVE_INFINITY && !ma.equalsPosition(lastEOA))
                Vehicle.setStop(id, ma.getEdgeIdEOA(), ma.getPositionEOA(), 0, Simulation.getEndTime());
            lastEOA = ma;
            Vehicle.setParameter(id, "dyn_lastEOA",
                    Double.isInfinite(lastEOA.getPositionEOA()) ? "Inf"
                            : Double.toString(
                                    Vehicle.getDrivingDistance(id, lastEOA.getEdgeIdEOA(), lastEOA.getPositionEOA())
                                            + Vehicle.getDistance(id)));
        }
    }

    @Override
    public void connectionChanged() {
        boolean isConnected = connections.stream().allMatch(x -> x.isActive());
        setConnected(isConnected);
    }

    @Override
    public int generateDelay(Message message) {
        return (int) Math.max(new Random().nextGaussian(delayOutMean, delayOutStd), 0);
    }

    @Override
    public void nextStep(int currentTime) {
        checkCell();
        sendPositionReport();
    }

    @Override
    public boolean canSend(Message message) {
        if (!connected && retry)
            queue.add(message);
        return connected;
    };

    @Override
    public boolean canReceive(Message message) {
        return connected;
    };

    @Override
    public String toString() {
        return id;
    };

    public float getDelayInMean() {
        return delayInMean;
    }

    public float getDelayInStd() {
        return delayInStd;
    }

    public double getLength() {
        return length;
    }

    public String getId() {
        return id;
    }

    public RBC getRbc() {
        return rbc;
    }

    public void free() {
        for (var conn : connections) {
            conn.removeObserver(this);
        }
        connected = false;
        sumoManager.stepUnsubscribe(this);
    }

    private void sendPositionReport() {
        int index = Vehicle.getRouteIndex(id);
        StringVector route = Vehicle.getRoute(id);
        double frontPos = Vehicle.getLaneID(id).startsWith(":") ? Lane.getLength(route.get(index) + "_0")
                : Vehicle.getLanePosition(id);
        double endPos = frontPos - Vehicle.getLength(id);
        int startIndex = index;
        while (endPos < 0 && startIndex > 0) {
            // Vehicle is not entirely on the edge, so we'll get the back one
            var edgeId = route.get(startIndex - 1);
            endPos = Lane.getLength(edgeId + "_0") + endPos;
            startIndex--;
        }
        Message m = new PositionReport(this, this.rbc, endPos, frontPos, route.subList(startIndex, index + 1),
                route.subList(startIndex, route.size()));
        m.send(sumoManager);
    }

    private void checkCell() {
        var position = Vehicle.getPosition(id);
        ((HandoverConnection) connections.get(1)).updateCell(position.getX(), position.getY());
    }

    private void setConnected(boolean connected) {
        this.connected = connected;
        while (this.connected && this.queue.size() > 0) {
            this.queue.removeFirst().send(sumoManager);
        }
    }

    private String id;
    private String typeId;
    private RBC rbc;
    private double length;

    private SumoManager sumoManager;
    private MovementAuthority lastEOA = null;
    private boolean connected = true;
    private List<Message> queue = new LinkedList<>();
    private List<Connection> connections;

    private int positionReportInterval;
    private float delayInMean;
    private float delayInStd;
    private float delayOutMean;
    private float delayOutStd;
    private boolean retry;
    private float safetyMargin;
}
