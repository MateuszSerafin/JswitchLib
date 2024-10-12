package com.gmail.genek530.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

public class ConnectionUtils {
    //it really breaks things
    static String ansiiRegex = "\\x1B(?:[@-Z\\-_]|\\[[0-?]*[ -/]*[@-~])";

    //i think i want to handle timeout as a regular exception this might change
    public static List<String> readTillMet(List<String> metWhat, InputStream readWhat, OutputStream writeConsole) throws Exception {
        return readTillMet(metWhat, readWhat, writeConsole, 30, new HashMap<>());
    }

    //i think i want to handle timeout as a regular exception this might change
    public static List<String> readTillMet(List<String> metWhat, InputStream readWhat, OutputStream writeConsole, Map<String, String> replaceWith) throws Exception{
        return readTillMet(metWhat, readWhat, writeConsole, 30, replaceWith);
    }

    public static List<String> readTillMet(List<String> metWhat, InputStream readWhat, OutputStream writeConsole, int timeout, Map<String, String> replaceWith) throws Exception {
        Future<List<String>> future = Executors.newSingleThreadExecutor().submit(new Callable<List<String>>() {
            @Override
            public List<String> call() throws Exception {
                List<String> toReturn = new ArrayList<>();
                StringBuilder line = new StringBuilder();

                while (true) {
                    int value = readWhat.read();
                    if (value == -1) {
                        Thread.sleep(100);
                        continue;
                    }
                    char from = (char) value;

                    if (from == '\n') {
                        String toBeAdded = line.toString().replaceAll(ansiiRegex, "");
                        toReturn.add(toBeAdded);

                        for (Map.Entry<String, String> stringStringEntry : replaceWith.entrySet()) {
                            if (line.toString().contains(stringStringEntry.getKey())) {
                                writeConsole.write(stringStringEntry.getValue().getBytes());
                                writeConsole.flush();
                            }
                        }

                        for (String s : metWhat) {
                            if (line.toString().toLowerCase().contains(s)) {
                                return toReturn;
                            }
                        }
                        line = new StringBuilder();
                        continue;
                    }
                    line.append((char) value);

                    for (String s : metWhat) {
                        if (line.toString().toLowerCase().contains(s)) {
                            for (Map.Entry<String, String> stringStringEntry : replaceWith.entrySet()) {
                                if (line.toString().contains(stringStringEntry.getKey())) {
                                    writeConsole.write(stringStringEntry.getValue().getBytes());
                                    writeConsole.flush();
                                }
                            }
                            String toBeAdded = line.toString().replaceAll(ansiiRegex, "");
                            toReturn.add(toBeAdded);
                            return toReturn;
                        }
                    }
                }
            }
        });

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.schedule(new Runnable() {
            @Override
            public void run() {
                future.cancel(true);
            }
        }, timeout, TimeUnit.SECONDS);

        try {
            return future.get();
        } catch (CancellationException e) {
            throw new Exception("Timeout reached");
        } catch (Exception e) {
            throw e;
        }
    }

    //useful for debug
    public static void peekAtData(InputStream readWhat) throws Exception {
        StringBuilder line = new StringBuilder();
        int value;
        while ((value = readWhat.read()) != -1) {
            char from = (char) value;
            System.out.println("RAW" + value + " :EQUALS:" + from + "\n");
        }
        System.out.println("if u see that peek data finished");
    }

    public static void flushReader(InputStream readConsole) throws IOException {
        while(readConsole.available() != 0){
            readConsole.read();
        }
    }
}