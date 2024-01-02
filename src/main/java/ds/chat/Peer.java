package ds.chat;

import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class Peer {

    private final InetAddress[] addresses;

    private final ArrayList<String> words;

    private final Map<InetAddress,Integer> indexes;

    private final ServerSocket server;

    private final FileWriter file;

    Peer(InetAddress[] addresses) throws IOException {

        file = new FileWriter("log.txt");
        server = new ServerSocket(5000);
        this.addresses = addresses;
        words = new ArrayList<>();

        this.indexes = new HashMap<>();
        for (InetAddress address : addresses) {
            indexes.put(address, 0);
        }

        new Thread(new WordGenerator(words,file)).start();
        new Thread(() -> {
            while (true){
                try {
                    Socket client = server.accept();
                    updateValues(client, indexes.get(client.getInetAddress()));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }).start();
    }

}
