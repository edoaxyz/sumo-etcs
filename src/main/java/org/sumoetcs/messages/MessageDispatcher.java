package org.sumoetcs.messages;

import java.util.PriorityQueue;

import org.sumoetcs.sumo.IStepTrigger;
import org.sumoetcs.sumo.SumoManager;

public class MessageDispatcher implements IStepTrigger {
    public MessageDispatcher(SumoManager sumoManager) {
        this.sumoManager = sumoManager;
    }

    public void send(Message message) {
        messages.add(message);
        sumoManager.stepSubscribeAt(this, message.getTimeReception(), true);
    }

    @Override
    public void nextStep(int currentTime) {
       while (messages.peek().getTimeReception() <= currentTime) {
            Message m = messages.poll();
            m.getRecipient().receive(m);
        }
    }

    private SumoManager sumoManager;
    private PriorityQueue<Message> messages = new PriorityQueue<>();
}
