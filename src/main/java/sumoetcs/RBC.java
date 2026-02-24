package sumoetcs;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.eclipse.sumo.libsumo.Vehicle;

import sumoetcs.interlocking.Net;
import sumoetcs.interlocking.Occupation;
import sumoetcs.interlocking.Segment;
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
        this.net.load();
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
            Segment nextSegment = this.net.getSegmentFromEdges(prMess.getBackPosition(), -1, prMess.getNextEdges());
            sendMovementAuthority(prMess.getTrain(), nextSegment);
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

    public void sendMovementAuthority(Train train) {
        sendMovementAuthority(train, null);
    }

    public void sendMovementAuthority(Train train, Segment segment) {
        if (!trains.containsKey(train.getId())) return;
        Occupation occ = occupations.get(train);
        if (occ == null) {
            occ = new Occupation(train, segment);
            occupations.put(train, occ);
        } else {
            occ.requestNextSegment(segment);
        }

        // TODO: add startEdge to MA?
        // var startEdge = net.toEdge(occ.getSegment(), occ.getStartPositionInTrack(occ.getFirstTrack()));
        var endEdge = net.toEdge(occ.getSegment().getLastTrack(), occ.getSegment().getEndPosition());
        MovementAuthority maMessage = new MovementAuthority(this, train, endEdge.getKey(), endEdge.getValue());
        maMessage.send(sumoManager);
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
            if (occupations.containsKey(t))
                occupations.remove(t).free();
        }
    }

    private Map<String, Train> trains = new HashMap<>();
    private Map<Train, Occupation> occupations = new HashMap<>();
    private SumoManager sumoManager;
    private Net net;
}
