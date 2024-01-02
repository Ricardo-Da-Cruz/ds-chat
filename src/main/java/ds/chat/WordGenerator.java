package ds.chat;

import ds.poisson.PoissonProcess;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

public class WordGenerator implements Runnable{

    private final String[] words = new String[10000];

    private final ArrayList<String> list;

    private final FileWriter file;

    WordGenerator(ArrayList<String> list, FileWriter file) {
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
                synchronized (list){
                    list.add(words[(int) (Math.random() * words.length)] + " " + InetAddress.getLocalHost());
                }
                file.write("\n" + list.get(list.size() - 1));
                file.flush();
                System.out.println("added word:" + words[(int) (Math.random() * words.length)] + "\n");
            } catch (InterruptedException | IOException e) {
                System.out.println("thread interrupted");
                e.printStackTrace(System.out);
            }
        }
    }
}
