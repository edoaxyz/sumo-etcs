package sumoetcs;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.eclipse.sumo.libtraci.Lane;
import org.eclipse.sumo.libtraci.Simulation;
import org.eclipse.sumo.libtraci.StringVector;
import org.eclipse.sumo.libtraci.Vehicle;
import org.eclipse.sumo.libtraci.VehicleType;

import sumoetcs.messages.IMessageUser;
import sumoetcs.messages.Message;
import sumoetcs.messages.MovementAuthority;
import sumoetcs.messages.PositionReport;
import sumoetcs.sumo.IStepTrigger;
import sumoetcs.sumo.SumoManager;

public class Train implements IStepTrigger, IMessageUser {

    public Train(String id, RBC rbc, SumoManager sumoManager) {
        this.id = id;
        this.typeId = Vehicle.getTypeID(id);
        this.rbc = rbc;
        this.length = Vehicle.getLength(id);

        this.positionReportInterval = Integer.parseInt(VehicleType.getParameter(typeId, "positionReportInterval"));
        this.delayInMean = Float.parseFloat(VehicleType.getParameter(typeId, "delayInMean"));
        this.delayInStd = Float.parseFloat(VehicleType.getParameter(typeId, "delayInStd"));
        this.delayOutMean = Float.parseFloat(VehicleType.getParameter(typeId, "delayOutMean"));
        this.delayOutStd = Float.parseFloat(VehicleType.getParameter(typeId, "delayOutStd"));
        this.retry = VehicleType.getParameter(typeId, "retry").equals("true");

        Vehicle.setSpeed(id, 0);
        this.sumoManager = sumoManager;
        sumoManager.stepSubscribe(this, true);
        sumoManager.stepSubscribeIn(this, positionReportInterval, false);
    }

    @Override
    public void receive(Message message) {
        if (message instanceof MovementAuthority) {
            MovementAuthority ma = (MovementAuthority) message;
            if (lastEOA != null) {
                if (ma.getTime() <= lastEOA.getTime())
                    return;
                if (lastEOA.getPositionEOA() != Double.POSITIVE_INFINITY && lastEOA.getPositionEOA() >= 0)
                    Vehicle.setStop(id, lastEOA.getEdgeIdEOA(), lastEOA.getPositionEOA(), 0, 0);
            } else {
                if (ma.getPositionEOA() > length || !ma.getEdgeIdEOA().equals(Vehicle.getRoadID(id)))
                    Vehicle.setSpeed(id, -1);
                else
                    return;
            }
            if (ma.getPositionEOA() != Double.POSITIVE_INFINITY)
                Vehicle.setStop(id, ma.getEdgeIdEOA(), ma.getPositionEOA(), 0, Simulation.getEndTime());
            lastEOA = ma;
        }
    }

    @Override
    public int generateDelay(Message message) {
        return (int) Math.max(new Random().nextGaussian(delayOutMean, delayOutStd), 0);
    }

    @Override
    public void nextStep(int currentTime) {
        sendPositionReport();
    }

    @Override
    public boolean canSend(Message message) {
        if (!connected && retry) queue.add(message); 
        return connected;
    };

    @Override
    public boolean canReceive(Message message) {
        return connected;
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

    public void free() {
        sumoManager.stepUnsubscribe(this);
    }

    private void sendPositionReport() {
        int index = Vehicle.getRouteIndex(id);
        StringVector route = Vehicle.getRoute(id);
        double frontPos = Vehicle.getLaneID(id).startsWith(":") ? Lane.getLength(route.get(index) + "_0")
                : Vehicle.getLanePosition(id);
        double endPos = frontPos - Vehicle.getLength(id);
        List<String> nextEdges = route.subList(index, route.size());
        List<String> occupiedEdges = new LinkedList<>(List.of(route.get(index)));
        while (endPos < 0) {
            // Vehicle is not entirely on the edge, so we'll get the back one
            var edgeId = route.get(index - 1);
            occupiedEdges.add(0, edgeId);
            endPos = Lane.getLength(edgeId + "_0") + endPos;
            index--;
        }
        Message m = new PositionReport(this, this.rbc, endPos, frontPos, occupiedEdges, nextEdges);
        m.send(sumoManager);
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

    private int positionReportInterval;
    private float delayInMean;
    private float delayInStd;
    private float delayOutMean;
    private float delayOutStd;
    private boolean retry;

}
