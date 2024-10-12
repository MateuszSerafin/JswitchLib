package com.gmail.genek530.utils;

import java.util.ArrayList;
import java.util.List;

public class AnnoyingCsvPortParser {

    private List<Integer> arr = new ArrayList<>();

    public AnnoyingCsvPortParser(String description){}

    public void addInt(int port){
        this.arr.add(port);
    }

    //ngl that one is stolen from chatgpt
    public String summarizeRange() {
        if (arr == null || arr.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        int start = arr.get(0);
        int end = arr.get(0);

        for (int i = 1; i < arr.size(); i++) {
            if (arr.get(i) == end + 1) {
                end = arr.get(i);
            } else {
                if (start == end) {
                    sb.append(start);
                } else {
                    sb.append(start).append("-").append(end);
                }
                //it should not break anything
                //tldr if csv comma is not great separator space is good enough
                sb.append(" ");
                start = arr.get(i);
                end = arr.get(i);
            }
        }
        if (start == end) {
            sb.append(start);
        } else {
            sb.append(start).append("-").append(end);
        }
        return sb.toString();
    }
}
