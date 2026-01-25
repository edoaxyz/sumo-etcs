package sumoetcs.messages;

import sumoetcs.sumo.IStepTrigger;
import sumoetcs.sumo.SumoManager;

public abstract class Message implements IStepTrigger {

    public Message(IMessageUser sender, IMessageUser recipient) {
        this.sender = sender;
        this.recipient = recipient;
    }

    public void send(SumoManager sumoManager) {
        if (sender.canSend(this)) {
            this.delay = sender.generateDelay(this);
            this.time = sumoManager.getCurrentTime();
            sumoManager.stepSubscribeIn(this, this.delay, true);
        }
    }

    public void nextStep(int currentTime) {
        if (recipient.canReceive(this))
            recipient.receive(this);
    }

    public IMessageUser getSender() {
        return sender;
    }

    public IMessageUser getRecipient() {
        return recipient;
    }

    public int getTime() {
        return time;
    }

    protected int delay;
    protected int time;
    protected IMessageUser sender;
    protected IMessageUser recipient;
}
