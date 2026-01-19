package org.sumoetcs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.sumo.libtraci.Simulation;
import org.eclipse.sumo.libtraci.Vehicle;
import org.sumoetcs.interlocking.Interlocking;
import org.sumoetcs.interlocking.Net;
import org.sumoetcs.messages.IMessageUser;
import org.sumoetcs.messages.Message;
import org.sumoetcs.messages.MovementAuthority;
import org.sumoetcs.messages.PositionReport;
import org.sumoetcs.sumo.IStepTrigger;
import org.sumoetcs.sumo.SumoManager;

public class RBC implements IStepTrigger, IMessageUser {
    public static final Set<String> ALLOWED_CLASSES = new HashSet<>(
            Arrays.asList("rail", "rail_fast", "rail_urban", "rail_electric"));

    public RBC(SumoManager sumoManager) {
        this.sumoManager = sumoManager;
        this.sumoManager.stepSubscribe(this, false);

        this.net = new Net();
        this.interlocking = new Interlocking(net);
        this.net.load();
        this.interlocking.load();
    }

    @Override
    public void nextStep(int currentTime) {
        refreshTrains();
    }

    @Override
    public void receive(Message message) {
        if (message instanceof PositionReport) {
            PositionReport prMess = (PositionReport) message;
            Interlocking.Occupation occ = interlocking.getOccupation(prMess.getTrain());
            double startPos = 0, endPos = 0;
            var tracks = new LinkedList<Net.Track>();
            for (var edge : prMess.getOccupiedEdges()) {
                var t = net.toTrack(edge, prMess.getOccupiedEdges().getFirst().equals(edge) ? prMess.getBackPosition() : 0);
                if (prMess.getOccupiedEdges().getFirst().equals(edge)) startPos = t.getValue();
                if (t.getKey() != tracks.getLast()) tracks.add(t.getKey());
                if (prMess.getOccupiedEdges().getLast().equals(edge)) endPos = t.getValue() + prMess.getFrontPosition();
            }
            if (occ == null) {
                occ = interlocking.createOccupation(prMess.getTrain(), tracks, startPos, endPos);
            } else {
                occ.updateStart(tracks.getFirst(), startPos);
            }
            tracks = new LinkedList<>();
            for (var edge: prMess.getNextEdges()) {
                var t = net.toTrack(edge, 0);
                if (tracks.getLast() != t.getKey()) {
                    tracks.add(t.getKey());
                }
            }
            occ.requestNextEOA(tracks, 1.);

            // TODO: add startEdge to MA?
            var startEdge = net.toEdge(occ.getFirstTrack(), startPos);
            var endEdge = net.toEdge(occ.getLastTrack(), endPos);
            MovementAuthority maMessage = new MovementAuthority(this, prMess.getTrain(), endEdge.getKey(), endEdge.getValue());
            maMessage.send(sumoManager);
        }
    }

    @Override
    public int generateDelay(Message message) {
        if (message.getRecipient() instanceof Train) {
            Train t = (Train) message.getRecipient();
            return (int) new Random().nextGaussian(t.getDelayInMean(), t.getDelayInStd());
        }
        return 0;
    }

    private void refreshTrains() {
        Set<String> ids = new HashSet<>(trains.keySet());
        for (var id : Simulation.getLoadedIDList()) {
            if (!trains.containsKey(id) && ALLOWED_CLASSES.contains(Vehicle.getVehicleClass(Vehicle.getTypeID(id)))) {
                Train t = new Train(id, this, sumoManager);
                trains.put(id, t);
            }
            ids.remove(id);
        }

        for (var idToRemove : ids) {
            Train t = trains.remove(idToRemove);
            Interlocking.Occupation occ = interlocking.getOccupation(t);
            if (occ != null)
                occ.free();
        }
    }

    private Map<String, Train> trains = new HashMap<>();
    private SumoManager sumoManager;
    private Net net;
    private Interlocking interlocking;
}
