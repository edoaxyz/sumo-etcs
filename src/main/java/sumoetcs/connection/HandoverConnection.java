package sumoetcs.connection;

import java.math.BigDecimal;

import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.models.pn.PetriStateFeature;
import org.oristool.models.pn.Priority;
import org.oristool.models.stpn.MarkingExpr;
import org.oristool.models.stpn.trees.StochasticTransitionFeature;
import org.oristool.petrinet.Marking;
import org.oristool.petrinet.PetriNet;
import org.oristool.petrinet.Place;
import org.oristool.petrinet.Transition;
import org.oristool.simulator.Sequencer;
import org.oristool.simulator.Sequencer.SequencerEvent;
import org.oristool.simulator.SequencerObserver;
import org.oristool.simulator.stpn.STPNSimulatorComponentsFactory;

import sumoetcs.sumo.IStepTrigger;
import sumoetcs.sumo.SumoManager;

public class HandoverConnection extends Connection implements IStepTrigger, SequencerObserver {

    public HandoverConnection(SumoManager sumoManager, String sumoTypeId) {
        super(sumoTypeId);
        this.sumoManager = sumoManager;
        prepareNet();
    } 

    @Override
    public boolean isActive() {
        return cellChanges == 0;
    }

    @Override
    public void nextStep(int currentTime) {
        cellChanges--;
        notifyObservers();
    }

    @Override
    public void update(SequencerEvent e) {
        int currentTime = 0;
        if (sequencer.getCurrentRunElapsedTime() != null)
            currentTime = (int)(sequencer.getCurrentRunElapsedTime().floatValue() * 1000.);
        if (e == SequencerEvent.FIRING_EXECUTED) {
            Marking marking = sequencer.getLastSuccession().getChild().getFeature(PetriStateFeature.class).getMarking();
            if (marking.getTokens("Reconnected") > 0) {
                sumoManager.stepSubscribeAt(this, currentTime, true);
            }
        }
        if (currentTime > sumoManager.getEndTime()) {
            sequencer.removeCurrentRunObserver(this);
            sequencer.removeObserver(this);
        }
    }

    public void updateCell(double x, double y) {
        String newCell = HandoverMap.getInstance().nextCell(x, y);
        if (newCell != null && !newCell.equals(cellID)) {
            cellID = newCell;
            cellChanges += 1;

            sequencer.addObserver(this);
            sequencer.addCurrentRunObserver(this);
            sequencer.simulate();
        }
    } 

    private void prepareNet() {
        net = new PetriNet();
        Marking marking = new Marking();

        //Generating Nodes
        Place Disconnected = net.addPlace("Disconnected");
        Place Reconnected = net.addPlace("Reconnected");
        Transition t0 = net.addTransition("t0");

        //Generating Connectors
        net.addPrecondition(Disconnected, t0);
        net.addPostcondition(t0, Reconnected);

        //Generating Properties
        marking.setTokens(Disconnected, 1);
        marking.setTokens(Reconnected, 0);
        t0.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(getTypeParameter("delayCellReconnection", "0.3")), MarkingExpr.from("1", net)));
        t0.addFeature(new Priority(0));

        sequencer = new Sequencer(net, marking, new STPNSimulatorComponentsFactory(), new AnalysisLogger() {
            @Override
            public void log(String message) { }

            @Override
            public void debug(String string) { }
        });

    }

    private PetriNet net;
    private Sequencer sequencer;

    private Integer cellChanges = 0;
    private String cellID;
    private SumoManager sumoManager;
}
