package sumoetcs.connection;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.oristool.analyzer.log.AnalysisLogger;
import org.oristool.models.pn.PetriStateFeature;
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

public class BurstedConnection extends Connection implements IStepTrigger, SequencerObserver {

    public BurstedConnection(SumoManager sumoManager, String sumoTypeId) {
        super(sumoTypeId);
        this.sumoManager = sumoManager;
        prepareNet();
    }

    @Override
    public boolean isActive() {
        return isActive;
    }

    @Override
    public void nextStep(int currentTime) {
        isActive = !isActive;
        notifyObservers();
    }

    @Override
    public void update(SequencerEvent e) {
        int currentTime = 0;
        if (sequencer.getCurrentRunElapsedTime() != null)
            currentTime = (int)(sequencer.getCurrentRunElapsedTime().floatValue() * 1000.);
        if (e == SequencerEvent.FIRING_EXECUTED) {
            Marking marking = sequencer.getLastSuccession().getChild().getFeature(PetriStateFeature.class).getMarking();
            if ((marking.getTokens("ok") > 0) != lastSubscription) {
                sumoManager.stepSubscribeAt(this, currentTime, true);
            }
            lastSubscription = marking.getTokens("ok") > 0;
        }
        if (currentTime > sumoManager.getEndTime()) {
            sequencer.removeCurrentRunObserver(this);
            sequencer.removeObserver(this);
        }
    }

    private void prepareNet() {
        net = new PetriNet();
        Marking marking = new Marking();

        Place ko = net.addPlace("ko");
        Place ok = net.addPlace("ok");
        Transition endBurst = net.addTransition("endBurst");
        Transition startBurst = net.addTransition("startBurst");

        // Generating Connectors
        net.addPrecondition(ok, startBurst);
        net.addPostcondition(startBurst, ko);
        net.addPrecondition(ko, endBurst);
        net.addPostcondition(endBurst, ok);

        // Generating Properties
        marking.setTokens(ko, 0);
        marking.setTokens(ok, 1);
        endBurst.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"),
                MarkingExpr.from(getTypeParameter("expEndBurst", "3.7446"), net)));
        startBurst.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"),
                MarkingExpr.from(getTypeParameter("expStartBurst", "0.002565"), net)));

        sequencer = new Sequencer(net, marking, new STPNSimulatorComponentsFactory(), new AnalysisLogger() {
            @Override
            public void log(String message) {
            }

            @Override
            public void debug(String string) {
            }
        });

        isActive = marking.getTokens("ok") > 0;
        sequencer.addObserver(this);
        sequencer.addCurrentRunObserver(this);
        try {
            sequencer.simulate();
        } catch (ArithmeticException e) {
            // Probably some null exponential rate has been set
            sequencer.removeCurrentRunObserver(this);
            sequencer.removeObserver(this);
        }
    }

    private PetriNet net;
    private Sequencer sequencer;
    private boolean lastSubscription = true;

    private boolean isActive;
    private SumoManager sumoManager;
}
