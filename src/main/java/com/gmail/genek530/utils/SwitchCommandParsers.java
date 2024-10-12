package com.gmail.genek530.utils;

import java.util.ArrayList;
import java.util.List;

public class SwitchCommandParsers {
    public static List<List<String>> BetterSpaceSpliter(List<String> data) {
        List<List<String>> accumulator = new ArrayList<>();

        for (String datum : data) {
            List<String> lineAccumulator = new ArrayList<>();

            String current = "";
            for (int i = 0; i < datum.length(); i++) {
                if (datum.charAt(i) == ' ') {
                    if (current.isEmpty()) continue;
                    lineAccumulator.add(current.strip());
                    current = "";
                    continue;
                }
                current += datum.charAt(i);
            }
            lineAccumulator.add(current.strip());
            if (!lineAccumulator.isEmpty()) {
                accumulator.add(lineAccumulator);
            }
        }
        return accumulator;
    }


    public static List<String> BetterSpaceSpliter(String datum) {
        List<String> accumulator = new ArrayList<>();

        String current = "";
        for (int i = 0; i < datum.length(); i++) {
            if (datum.charAt(i) == ' ') {
                if (current.isEmpty()) continue;
                accumulator.add(current.strip());
                current = "";
                continue;
            }
            current += datum.charAt(i);
        }
        accumulator.add(current.strip());
        return accumulator;
    }


    public static List<List<String>> parseColumnsRows(List<String> data, String columnRow, char splitColumnsBy){
        //this function might take calls from data that is dirty from top or bottom the middle of data should always be correct
        List<List<Integer>> columnIndex = new ArrayList<>();

        List<Integer> collector = new ArrayList<>();
        for (int i = 0; i < columnRow.length(); i++) {
            char at = columnRow.charAt(i);
            if(at == splitColumnsBy){
                collector.add(i);
                continue;
            }
            if(collector.isEmpty()){
                continue;
            }
            columnIndex.add(collector);
            collector = new ArrayList<>();
        }
        if(!collector.isEmpty()) columnIndex.add(collector);

        List<List<String>> parsedLine = new ArrayList<>();

        for (String unparsedLine : data) {

            List<String> parsedCollector = new ArrayList<>();

            for (List<Integer> index : columnIndex) {
                int lastIndex = index.get(index.size() - 1);
                if(lastIndex > unparsedLine.length()){
                    break;
                }
                StringBuilder builder = new StringBuilder();

                for (Integer i : index) {
                    if(i >= unparsedLine.length()) continue;
                    builder.append(unparsedLine.charAt(i));
                }
                parsedCollector.add(builder.toString().strip());
            }
            if(!parsedCollector.isEmpty()){
                parsedLine.add(parsedCollector);
            }
        }
        return parsedLine;
    }
}