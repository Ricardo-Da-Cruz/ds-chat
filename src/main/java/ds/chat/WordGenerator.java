package ds.chat;

import ds.poisson.PoissonProcess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class WordGenerator implements Runnable{

    private final String[] words = new String[10000];

    private final TreeMap<ClockWithIP, MessageData> list;

    private final FileWriter file;

    private final Lamport clock;

    WordGenerator(TreeMap<ClockWithIP, MessageData> list, FileWriter file,Lamport clock) {
        try {
            this.clock = clock;
            this.file = file;
            this.list = list;
            Scanner wordsFile = new Scanner(new File("words.txt"));
            int numWords = 0;
            while (wordsFile.hasNext()) {
                words[numWords] = wordsFile.nextLine();
                numWords++;
            }
            System.out.println("Loaded " + numWords + " words.\n");

        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }


    @Override
    public void run() {
        PoissonProcess pp = new PoissonProcess(4, new Random((int) (Math.random() * 1000)));
        while (true) {
            double t = pp.timeForNextEvent() * 60.0 * 1000.0;

            try{
                Thread.sleep((int)t);

                String word = words[(int) (Math.random() * words.length)] + " " + InetAddress.getLocalHost();
                MessageData value = new MessageData(word);

                synchronized (list){
                    ClockWithIP key = new ClockWithIP(clock.tick(), InetAddress.getLocalHost().getAddress());
                    list.put(key, value);
                }

            } catch (InterruptedException | IOException e) {
                System.out.println("thread interrupted");
                e.printStackTrace(System.out);
            }
        }
    }
}
