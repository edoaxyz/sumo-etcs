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

        this.sumoManager = sumoManager;
        sumoManager.stepSubscribe(this, true);
        sumoManager.stepSubscribeIn(this, positionReportInterval, false);
    }

    @Override
    public void receive(Message message) {
        if (message instanceof MovementAuthority) {
            MovementAuthority ma = (MovementAuthority)message;
            if (ma.getPositionEOA() == Double.POSITIVE_INFINITY) {
                if (lastEOAPos >= 0) {
                    Vehicle.setStop(id, lastEOAEdge, lastEOAPos, 0, 0);
                    lastEOAEdge = "";
                    lastEOAPos = -1;
                }
            } else {
                if (lastEOAPos >= 0) Vehicle.setStop(id, lastEOAEdge, lastEOAPos, 0, 0);
                Vehicle.setStop(id, ma.getEdgeIdEOA(), ma.getPositionEOA(), 0, Simulation.getEndTime());
                lastEOAEdge = ma.getEdgeIdEOA();
                lastEOAPos = ma.getPositionEOA();
            } 
        }
    }

    @Override
    public int generateDelay(Message message) {
        return (int)Math.max(new Random().nextGaussian(delayOutMean, delayOutStd), 0);
    }

    @Override
    public void nextStep(int currentTime) {
        sendPositionReport();
    }

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
        double frontPos = Vehicle.getLanePosition(id);
        double relativePos = Vehicle.getLanePosition(id) - Vehicle.getLength(id);
        int index = Vehicle.getRouteIndex(id);
        StringVector route = Vehicle.getRoute(id);
        List<String> nextEdges = route.subList(index, route.size());
        List<String> occupiedEdges = new LinkedList<>(List.of(route.get(index)));
        while (relativePos < 0) {
            // Vehicle is not entirely on the edge, so we'll get the back one
            var edgeId = route.get(index - 1);
            occupiedEdges.add(0, edgeId);
            relativePos = Lane.getLength(edgeId+"_0") + relativePos;
            index--;
        }
        Message m = new PositionReport(this, this.rbc, relativePos, frontPos, occupiedEdges, nextEdges);
        m.send(sumoManager);
    }

    private String id;
    private String typeId;
    private RBC rbc;
    private double length;

    private SumoManager sumoManager;
    private String lastEOAEdge = "";
    private double lastEOAPos = -1;
    
    private int positionReportInterval;
    private float delayInMean;
    private float delayInStd;
    private float delayOutMean;
    private float delayOutStd;

}
