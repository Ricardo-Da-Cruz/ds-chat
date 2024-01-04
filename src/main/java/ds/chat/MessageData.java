package ds.chat;

public class MessageData {

    private final String message;

    private int acks = 0;

    private int maxAck = 0;

    public MessageData(String message) {
        this.message = message;
    }

    public void addAck(int ackClock) {
        acks++;
        maxAck = Math.max(maxAck, ackClock);
    }

    public int getAcks() {
        return acks;
    }

    public String getMessage() {
        return message;
    }

}
