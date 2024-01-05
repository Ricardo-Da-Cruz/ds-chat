package ds.chat;

import ds.poisson.PoissonProcess;

import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;


public class Peer implements Runnable {


    //contains the messages that have been
    private final List<String> words = new ArrayList<>();

    //contains the messages that have been received
    //The key is the clock value of the message
    private final TreeMap<ClockWithIP,MessageData> messages = new TreeMap<>();

    private ServerSocket server;

    private final FileWriter file;

    private Socket[] sockets;

    private final Lamport clock = new Lamport();

    private final String[] wordsDic = new String[10000];

    private final InetAddress[] addresses;

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

                if (messages.get(key).getAcks() == sockets.length && messages.firstKey().equals(key)){
                    while (messages.firstEntry().getValue().getAcks() == sockets.length){
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
            synchronized (socket) {
                out.println(clock.getClock() + " ACK " + clockValue);
            }
        }

        public void run() {
            while (!socket.isClosed()){
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    String[] message = in.readLine().split(" ");

                    System.out.printf("received message: %s", String.join(" ",message));
                    System.out.printf("from: %s", socket.getInetAddress());

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
    }

    private void readFile(){
        try {
            Scanner wordsFile = new Scanner(new File("words.txt"));
            int numWords = 0;
            while (wordsFile.hasNext()) {
                wordsDic[numWords] = wordsFile.nextLine();
                numWords++;
            }
            System.out.printf("Loaded %d words.\n\n", numWords);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void Connect(){
        ArrayBlockingQueue<Socket> connections = new ArrayBlockingQueue<>(addresses.length);
        System.out.println(connections.remainingCapacity());

        new Thread(() -> {
            try {
                System.out.println("Listening for connections");
                server = new ServerSocket(5000);
                while (connections.size() < addresses.length - 1){
                    Socket client = server.accept();
                    connections.add(client);
                    new Thread(new Connection(client)).start();

                    System.out.println("Received connection from: " + client.getInetAddress() + " " + client.getPort());
                    System.out.println(connections.remainingCapacity());
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).start();

        try {
            System.out.println("Connecting to peers");
            System.out.println();

            for (int i = 0;!Arrays.equals(Arrays.copyOfRange(addresses[i].getAddress(),1,4),Arrays.copyOfRange(InetAddress.getLocalHost().getAddress(),1,4));i++) {
                System.out.println("Trying to connect to: " +  addresses[i].getHostAddress());
                Socket socket = new Socket(addresses[i], 5000);
                System.out.println("Connected to: " +  addresses[i].getHostAddress() + " " + socket.getLocalPort());

                connections.add(socket);
                new Thread(new Connection(socket)).start();

                System.out.println("a:" + connections.remainingCapacity());
            }
        }catch (IOException e) {
            System.out.println("Error connecting");
            throw new RuntimeException(e);
        }

        sockets = new Socket[connections.size()];

        for (int j = 0; j < sockets.length; j++){
            try {
                sockets[j] = connections.take();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }

    }

    Peer(InetAddress[] addresses) throws IOException {
        file = new FileWriter("log.txt");
        this.addresses = addresses;
    }

    @Override
    public void run() {
        System.out.println("reading files");
        Thread fileReader = new Thread(this::readFile);
        System.out.println("Starting Connections");
        Thread Connector = new Thread(this::Connect);

        Connector.start();
        fileReader.start();

        try {
            Connector.join();
            fileReader.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }


        PoissonProcess pp = new PoissonProcess(4, new Random((int) (Math.random() * 1000)));
        while (true) {
            double t = pp.timeForNextEvent() * 60.0 * 1000.0;

            try{
                Thread.sleep((int)t);

                String word = wordsDic[(int) (Math.random() * wordsDic.length)] + " " + InetAddress.getLocalHost();
                MessageData value = new MessageData(word);

                int clockV;

                synchronized (messages){
                    clockV = clock.tick();
                    ClockWithIP key = new ClockWithIP(clockV, InetAddress.getLocalHost().getAddress());
                    messages.put(key, value);
                }

                for (Socket socket : sockets){
                    synchronized (socket){
                        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
                        out.println(clockV + " " + word);
                    }
                }

            } catch (InterruptedException | IOException e) {
                System.out.println("thread interrupted");
                e.printStackTrace(System.out);
            }
        }
    }
}
