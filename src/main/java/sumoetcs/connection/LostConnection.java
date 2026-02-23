package sumoetcs.connection;

import java.math.BigDecimal;
import java.util.logging.Level;
import java.util.logging.Logger;

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

public class LostConnection extends Connection implements IStepTrigger, SequencerObserver {

    public LostConnection(SumoManager sumoManager, String sumoTypeId) {
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
            if ((marking.getTokens("Connected") > 0) != lastSubscription) {
                sumoManager.stepSubscribeAt(this, currentTime, true);
            }
            lastSubscription = marking.getTokens("Connected") > 0;
        }
        if (currentTime > sumoManager.getEndTime()) {
            sequencer.removeCurrentRunObserver(this);
            sequencer.removeObserver(this);
        }
    }

    private void prepareNet() {
        net = new PetriNet();
        Marking marking = new Marking();

        //Generating Nodes
        Place Connected = net.addPlace("Connected");
        Place EstFail = net.addPlace("EstFail");
        Place LossIndication = net.addPlace("LossIndication");
        Place Offline = net.addPlace("Offline");
        Place establish = net.addPlace("establish");
        Transition connect = net.addTransition("connect");
        Transition estp = net.addTransition("estp");
        Transition fail = net.addTransition("fail");
        Transition failp = net.addTransition("failp");
        Transition indicate = net.addTransition("indicate");
        Transition loss = net.addTransition("loss");

        //Generating Connectors
        net.addPrecondition(Connected, loss);
        net.addPostcondition(loss, LossIndication);
        net.addPrecondition(LossIndication, indicate);
        net.addPostcondition(indicate, Offline);
        net.addPrecondition(Offline, estp);
        net.addPrecondition(Offline, failp);
        net.addPostcondition(estp, establish);
        net.addPrecondition(establish, connect);
        net.addPostcondition(connect, Connected);
        net.addPostcondition(failp, EstFail);
        net.addPrecondition(EstFail, fail);
        net.addPostcondition(fail, Offline);

        //Generating Properties
        marking.setTokens(Connected, 1);
        marking.setTokens(EstFail, 0);
        marking.setTokens(LossIndication, 0);
        marking.setTokens(Offline, 0);
        marking.setTokens(establish, 0);
        connect.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from(getTypeParameter("expReconnect", "0.5991"), net)));
        estp.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(getTypeParameter("weightEstablish", "0.9999"), net)));
        estp.addFeature(new Priority(0));
        fail.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(getTypeParameter("waitFail", "10")), MarkingExpr.from("1", net)));
        fail.addFeature(new Priority(0));
        failp.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal("0"), MarkingExpr.from(getTypeParameter("weightFail", "0.0001"), net)));
        failp.addFeature(new Priority(0));
        indicate.addFeature(StochasticTransitionFeature.newDeterministicInstance(new BigDecimal(getTypeParameter("waitIndicate", "1")), MarkingExpr.from("1", net)));
        indicate.addFeature(new Priority(0));
        loss.addFeature(StochasticTransitionFeature.newExponentialInstance(new BigDecimal("1"), MarkingExpr.from(getTypeParameter("expLoss", "0.0000000277"), net)));

        sequencer = new Sequencer(net, marking, new STPNSimulatorComponentsFactory(), new AnalysisLogger() {
            @Override
            public void log(String message) { }

            @Override
            public void debug(String string) { }
        });

        isActive = marking.getTokens("Connected") > 0;
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
