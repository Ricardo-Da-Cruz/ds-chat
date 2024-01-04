package ds.chat;

import ds.poisson.PoissonProcess;

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

    private final String[] wordsDic = new String[10000];

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

        new Thread(() -> {
            while (true){
                try {
                    Socket client = server.accept();
                    new Thread(new Connection(client)).start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
        new Thread(this).start();
    }

    @Override
    public void run() {
        try {
            Scanner wordsFile = new Scanner(new File("words.txt"));
            int numWords = 0;
            while (wordsFile.hasNext()) {
                wordsDic[numWords] = wordsFile.nextLine();
                numWords++;
            }

            System.out.println("Loaded " + numWords + " words.\n");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }

        PoissonProcess pp = new PoissonProcess(4, new Random((int) (Math.random() * 1000)));
        while (true) {
            double t = pp.timeForNextEvent() * 60.0 * 1000.0;

            try{
                Thread.sleep((int)t);

                String word = wordsDic[(int) (Math.random() * wordsDic.length)] + " " + InetAddress.getLocalHost();
                MessageData value = new MessageData(word);

                synchronized (messages){
                    ClockWithIP key = new ClockWithIP(clock.tick(), InetAddress.getLocalHost().getAddress());
                    messages.put(key, value);
                }

                for (InetAddress peer : peers) {
                    Socket socket = new Socket(peer, 5000);
                    PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(clock.getClock() + " " + word);
                    socket.close();
                }

            } catch (InterruptedException | IOException e) {
                System.out.println("thread interrupted");
                e.printStackTrace(System.out);
            }
        }
    }
}
