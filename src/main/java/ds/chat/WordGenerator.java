package ds.chat;

import ds.poisson.PoissonProcess;
import org.javatuples.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.*;

public class WordGenerator implements Runnable{

    private final String[] words = new String[10000];

    private final TreeMap<Integer, Pair<String,Integer>> list;

    private final FileWriter file;

    private final Lamport clock = new Lamport();

    WordGenerator(TreeMap<Integer, Pair<String,Integer>> list, FileWriter file,Lamport clock) {
        try {
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

                String temp = words[(int) (Math.random() * words.length)] + " " + InetAddress.getLocalHost();
                synchronized (list){
                    list.put(clock.tick(), new Pair<>(temp,0));
                }

                file.write("\n" + temp);
                file.flush();
                System.out.println("added word:" + words[(int) (Math.random() * words.length)] + "\n");
            } catch (InterruptedException | IOException e) {
                System.out.println("thread interrupted");
                e.printStackTrace(System.out);
            }
        }
    }
}
