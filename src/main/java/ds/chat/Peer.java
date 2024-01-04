package ds.chat;

import org.javatuples.Pair;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;


public class Peer implements Runnable {


    //contains the messages that have been
    private final List<String> words = new ArrayList<>();

    //contains the messages that have been received
    private final TreeMap<Integer,Pair<String, Integer>> messages = new TreeMap<>();

    private final ServerSocket server = new ServerSocket(5000);

    private final InetAddress[] peers;

    private final FileWriter file;

    private Lamport clock = new Lamport();

    class Connection {
        private final Socket socket;
        private final int index;

        Connection(Socket socket, int index, InetAddress address, Peer peer) {
            this.socket = socket;
            this.index = index;
        }

        public void run() {
            try {
                while (true) {
                    int i = index;
                    synchronized (words) {
                        while (i < words.size()) {
                            socket.getOutputStream().write((words.get(i) + "\n").getBytes());
                            i++;
                        }
                    }
                    Thread.sleep(1000);
                }
            } catch (IOException | InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    Peer(InetAddress[] addresses) throws IOException {
        file = new FileWriter("log.txt");
        peers = addresses;

        new Thread(new WordGenerator(messages,file,clock)).start();
        new Thread(() -> {
            while (true){
                try {
                    Socket client = server.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream()));
                    PrintWriter out = new PrintWriter(client.getOutputStream(), true);

                    String[] message = in.readLine().split(",");

                    int clockValue = Integer.parseInt(message[0]);

                    String word = message[1];
                    if (word.equals("ACK")){
                        int wordClock = Integer.parseInt(message[2]);

                        synchronized (messages){
                            clock.receiveMessage(clockValue);
                            if (wordClock > clockValue){


                            }
                            auto temp = messages.get(wordClock);
                        }
                        if (messages.get(wordClock).getValue1() == peers.length || messages.firstKey() == clockValue){
                            while (messages.firstEntry().getValue().getValue1() == peers.length){
                                file.write("\n" + messages.firstEntry().getValue().getValue0());
                                file.flush();
                                messages.remove(messages.firstKey());
                            }
                        }
                    }else {
                        synchronized (messages){
                            clock.receiveMessage(clockValue);
                            messages.put(clockValue, new Pair<>(word, 0));
                        }

                        out.write(clock.getClock() + ",ACK," + clockValue + "\n");
                    }


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Thread.sleep(1000);
                synchronized (words) {
                    for (String word : words) {
                        System.out.println(word);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
