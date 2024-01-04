package ds.chat;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class Peer implements Runnable {


    //contains the messages that have been
    private final List<String> words = new ArrayList<>();

    //contains the messages that have been received
    //The key is the clock value of the message
    private final TreeMap<ClockWithIP,MessageData> messages = new TreeMap<>();

    private final ServerSocket server = new ServerSocket(5000);

    private final InetAddress[] peers;

    private final FileWriter file;

    private final Lamport clock = new Lamport();

    class Connection implements Runnable {
        private final Socket socket;

        Connection(Socket socket) {
            this.socket = socket;
        }

        private void handleACK(String[] message) throws IOException {
            // Clock ACK (clock of word)
            int clockValue = Integer.parseInt(message[0]);
            int wordClock = Integer.parseInt(message[2]);
            ClockWithIP key = new ClockWithIP(wordClock, socket.getInetAddress().getAddress());

            synchronized (messages){
                clock.receiveMessage(clockValue);
                messages.get(key).addAck(clockValue);

                if (messages.get(key).getAcks() == peers.length && messages.firstKey().equals(key)){
                    while (messages.firstEntry().getValue().getAcks() == peers.length){
                        file.write("\n" + messages.firstEntry().getValue().getMessage());
                        file.flush();

                        words.add(messages.firstEntry().getValue().getMessage());

                        System.out.println("added word:" + messages.firstEntry().getValue().getMessage() + "\n");
                        messages.remove(messages.firstKey());
                    }
                }
            }
        }

        private void handleNewMessage(String[] message) throws IOException {
            // Clock (clock of word)
            int clockValue = Integer.parseInt(message[0]);
            ClockWithIP key = new ClockWithIP(clockValue, socket.getInetAddress().getAddress());

            synchronized (messages){
                clock.receiveMessage(clockValue);
                messages.put(key, new MessageData(message[1]));
            }

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            out.println(clock.getClock() + " ACK " + clockValue);

        }

        public void run() {
            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                String[] message = in.readLine().split(" ");

                if (message[1].equals("ACK")){
                    handleACK(message);
                } else {
                    handleNewMessage(message);
                }

            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

    Peer(InetAddress[] addresses) throws IOException {
        file = new FileWriter("log.txt");
        peers = addresses;

        new Thread(new WordGenerator(messages,file,clock)).start();
        new Thread(this).start();
    }

    @Override
    public void run() {
        while (true){
            try {
                Socket client = server.accept();
                new Thread(new Connection(client)).start();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
