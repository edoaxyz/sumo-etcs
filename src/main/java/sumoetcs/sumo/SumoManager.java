package sumoetcs.sumo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import org.eclipse.sumo.libsumo.Simulation;
import org.eclipse.sumo.libsumo.StringVector;
import org.eclipse.sumo.libsumo.Vehicle;

public class SumoManager {
    private class TriggerInfo implements Comparable<TriggerInfo> {
        private IStepTrigger trigger;
        private int triggerIn;
        private int triggerAt;
        private boolean once;

        @Override
        public int compareTo(TriggerInfo o) {
            return triggerAt - o.triggerAt;
        }

        @Override
        public int hashCode() {
            return trigger.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null)
                return false;
            if (this == obj)
                return true;
            if (getClass() != obj.getClass())
                return false;
            TriggerInfo other = (TriggerInfo) obj;
            if (trigger != other.trigger)
                return false;
            return true;
        }

        public TriggerInfo(IStepTrigger trigger, int triggerIn, boolean once) {
            this.trigger = trigger;
            this.triggerIn = triggerIn;
            this.triggerAt = currentTime + triggerIn;
            this.once = once;
        }

        public boolean trigger() {
            if (currentTime >= this.triggerAt) {
                this.trigger.nextStep(currentTime);
                return true;
            }
            return false;
        }

        public TriggerInfo getNext() {
            if (once)
                return null;
            return new TriggerInfo(this.trigger, this.triggerIn, this.once);
        }
    }

    public static final Set<String> ALLOWED_CLASSES = new HashSet<>(
            Arrays.asList("rail", "rail_fast", "rail_urban", "rail_electric"));

    public SumoManager(String configPath, String outputPath) {
        Simulation.preloadLibraries();
        Simulation.start(new StringVector(new String[] { "sumo", "-c", configPath, "--start",
                "--time-to-teleport", "-1", "--railsignal-moving-block", "--fcd-output", outputPath,
                "--fcd-output.distance", "--fcd-output.params", "dyn_lastEOA"}));
        currentTime = (int) (Simulation.getTime() * 1000.);
        endTime = (int) (Simulation.getEndTime() * 1000.);
    }

    public void runAll() {
        while (currentTime < endTime || Simulation.getPendingVehicles().size() > 0 || Vehicle.getIDList().size() > 0) {
            nextStep();
        }
        Simulation.close();
    }

    public void nextStep() {
        Simulation.step();
        currentTime = (int) (Simulation.getTime() * 1000);
        List<TriggerInfo> toReAdd = new LinkedList<>();
        while (!stepTriggers.isEmpty() && stepTriggers.peek().trigger() == true) {
            TriggerInfo trigger = stepTriggers.poll().getNext();
            if (trigger != null)
                toReAdd.add(trigger);
        }
        stepTriggers.addAll(toReAdd);
    }

    public void stepSubscribe(IStepTrigger o, boolean once) {
        TriggerInfo t = new TriggerInfo(o, -1, once);
        stepTriggers.add(t);
    }

    public void stepSubscribe(IStepTrigger o, boolean once, int priority) {
        TriggerInfo t = new TriggerInfo(o, -1 * priority, once);
        stepTriggers.add(t);
    }

    public void stepSubscribeIn(IStepTrigger o, int in, boolean once) {
        TriggerInfo t = new TriggerInfo(o, in, once);
        stepTriggers.add(t);
    }

    public void stepSubscribeAt(IStepTrigger o, int at, boolean once) {
        TriggerInfo t = new TriggerInfo(o, at - currentTime, once);
        stepTriggers.add(t);
    }

    public void stepUnsubscribe(IStepTrigger o) {
        stepTriggers.remove(new TriggerInfo(o, currentTime, false));
    }

    public Integer getCurrentTime() {
        return currentTime;
    }

    public Integer getEndTime() {
        return endTime;
    }

    private int currentTime = 0;
    private int endTime;
    private PriorityQueue<TriggerInfo> stepTriggers = new PriorityQueue<>();
}
