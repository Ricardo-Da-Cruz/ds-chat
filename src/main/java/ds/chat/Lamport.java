package ds.chat;

public class Lamport {

    private int clock = 0;

    public synchronized int tick(){
        return ++clock;
    }

    public synchronized int receiveMessage(int senderClock){
        clock = Math.max(clock, senderClock) + 1;
        return clock;
    }

    public synchronized int getClock(){
        return clock;
    }
}
