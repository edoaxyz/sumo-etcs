package sumoetcs;

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

import sumoetcs.interlocking.Interlocking;
import sumoetcs.interlocking.Net;
import sumoetcs.interlocking.Track;
import sumoetcs.messages.IMessageUser;
import sumoetcs.messages.Message;
import sumoetcs.messages.MovementAuthority;
import sumoetcs.messages.PositionReport;
import sumoetcs.sumo.IStepTrigger;
import sumoetcs.sumo.SumoManager;

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
            if (!trains.containsKey(prMess.getTrain().getId())) return;
            Interlocking.Occupation occ = interlocking.getOccupation(prMess.getTrain());
            double startPos = 0, endPos = 0;
            var tracks = new LinkedList<Track>();
            var nextEdges = new LinkedList<String>(prMess.getNextEdges());
            for (var edge : prMess.getOccupiedEdges()) {
                var t = net.toTrack(edge, prMess.getOccupiedEdges().getFirst().equals(edge) ? prMess.getBackPosition() : 0);
                if (prMess.getOccupiedEdges().getFirst().equals(edge)) startPos = t.getValue();
                if (tracks.size() == 0 || t.getKey() != tracks.getLast()) tracks.add(t.getKey());
                if (prMess.getOccupiedEdges().getLast().equals(edge)) endPos = t.getValue() + prMess.getFrontPosition();
                if (nextEdges.getFirst().equals(edge)) nextEdges.removeFirst();
            }
            if (occ == null) {
                occ = interlocking.createOccupation(prMess.getTrain(), tracks, startPos, endPos);
            }
            for (var edge: nextEdges) {
                var t = net.toTrack(edge, -1);
                if (tracks.size() == 0 || tracks.getLast() != t.getKey()) {
                    tracks.add(t.getKey());
                }
                endPos = t.getValue();
            }
            occ.requestNextEOA(tracks, startPos, endPos);

            // TODO: add startEdge to MA?
            var startEdge = net.toEdge(occ.getFirstTrack(), occ.getStartPositionInTrack(occ.getFirstTrack()));
            var endEdge = net.toEdge(occ.getLastTrack(), occ.getEndPositionInTrack(occ.getLastTrack()));
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
        for (var id : Vehicle.getIDList()) {
            if (!trains.containsKey(id) && ALLOWED_CLASSES.contains(Vehicle.getVehicleClass(id))) {
                Train t = new Train(id, this, sumoManager);
                trains.put(id, t);
            }
            ids.remove(id);
        }

        for (var idToRemove : ids) {
            Train t = trains.remove(idToRemove);
            t.free();
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
