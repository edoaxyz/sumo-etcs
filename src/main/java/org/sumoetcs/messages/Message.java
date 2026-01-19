package org.sumoetcs.messages;

import org.sumoetcs.sumo.IStepTrigger;
import org.sumoetcs.sumo.SumoManager;

public abstract class Message implements IStepTrigger {

    public Message(IMessageUser sender, IMessageUser recipient) {
        this.sender = sender;
        this.recipient = recipient;
    }

    public void send(SumoManager sumoManager) {
        this.delay = sender.generateDelay(this);
        sumoManager.stepSubscribeIn(this, this.delay, true);
    }

    public void nextStep(int currentTime) {
        recipient.receive(this);
    }

    public IMessageUser getSender() {
        return sender;
    }

    public IMessageUser getRecipient() {
        return recipient;
    }

    protected int delay;
    protected IMessageUser sender;
    protected IMessageUser recipient;
}
