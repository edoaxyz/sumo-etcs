package sumoetcs.messages;

public interface IMessageUser {
    public void receive(Message message);
    public int generateDelay(Message message);
}
