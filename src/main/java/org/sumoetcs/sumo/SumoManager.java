package org.sumoetcs.sumo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.PriorityQueue;
import java.util.Set;

import org.eclipse.sumo.libtraci.Simulation;
import org.eclipse.sumo.libtraci.StringVector;

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
            if (obj.equals(trigger))
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

    public SumoManager(String configPath) {
        Simulation.preloadLibraries();
        Simulation.load(new StringVector(new String[] { "sumo", "-c", configPath }));
        currentTime = (int) Simulation.getTime() * 1000;
    }

    public void nextStep() {
        Simulation.step();
        currentTime = (int) Simulation.getTime() * 1000;
        while (stepTriggers.peek().trigger() == true) {
            TriggerInfo trigger = stepTriggers.poll().getNext();
            if (trigger != null)
                stepTriggers.add(null);
        }
    }

    public void stepSubscribe(IStepTrigger o, boolean once) {
        TriggerInfo t = new TriggerInfo(o, -1, once);
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
        stepTriggers.remove(o);
    }

    public int getCurrentTime() {
        return currentTime;
    }

    private int currentTime = 0;
    private PriorityQueue<TriggerInfo> stepTriggers = new PriorityQueue<>();
}
