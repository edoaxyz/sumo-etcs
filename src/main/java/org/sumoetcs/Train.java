package org.sumoetcs;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import org.eclipse.sumo.libtraci.Edge;
import org.eclipse.sumo.libtraci.Lane;
import org.eclipse.sumo.libtraci.Simulation;
import org.eclipse.sumo.libtraci.StringVector;
import org.eclipse.sumo.libtraci.Vehicle;
import org.eclipse.sumo.libtraci.VehicleType;
import org.sumoetcs.messages.IMessageUser;
import org.sumoetcs.messages.Message;
import org.sumoetcs.messages.MovementAuthority;
import org.sumoetcs.messages.PositionReport;
import org.sumoetcs.sumo.IStepTrigger;
import org.sumoetcs.sumo.SumoManager;

public class Train implements IStepTrigger, IMessageUser {

    public Train(String id, RBC rbc, SumoManager sumoManager) {
        this.id = id;
        this.typeId = Vehicle.getTypeID(id);
        this.rbc = rbc;
        this.length = Vehicle.getLength(typeId);

        this.positionReportInterval = Integer.parseInt(VehicleType.getParameter(id, "positionReportInterval"));
        this.delayInMean = Float.parseFloat(VehicleType.getParameter(id, "delayInMean"));
        this.delayInStd = Float.parseFloat(VehicleType.getParameter(id, "delayInStd"));
        this.delayOutMean = Float.parseFloat(VehicleType.getParameter(id, "delayOutMean"));
        this.delayOutStd = Float.parseFloat(VehicleType.getParameter(id, "delayOutStd"));

        this.sumoManager = sumoManager;
        sumoManager.stepSubscribe(this, true);
        sumoManager.stepSubscribeIn(this, positionReportInterval, false);
    }

    @Override
    public void receive(Message message) {
        if (message instanceof MovementAuthority) {
            MovementAuthority ma = (MovementAuthority)message;
            Vehicle.setStop(id, ma.getEdgeIdEOA(), ma.getPositionEOA(), 0, 0);
            Vehicle.setStop(id, ma.getEdgeIdEOA(), ma.getPositionEOA(), 0, Simulation.getEndTime());
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

    private void sendPositionReport() {
        double frontPos = Vehicle.getLanePosition(id);
        double relativePos = Vehicle.getLanePosition(id) - Vehicle.getLength(typeId);
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
    
    private int positionReportInterval;
    private float delayInMean;
    private float delayInStd;
    private float delayOutMean;
    private float delayOutStd;


}
