package sumoetcs.messages;

public interface IMessageUser {
    public void receive(Message message);

    public int generateDelay(Message message);

    public default boolean canSend(Message message) {
        return true;
    };

    public default boolean canReceive(Message message) {
        return true;
    };
}
