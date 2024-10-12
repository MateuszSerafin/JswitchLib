package com.gmail.genek530.ssh.switchesconn.enddevice;

import com.gmail.genek530.utils.ConnectionUtils;
import com.gmail.genek530.ssh.switchesconn.in.InputDeviceInterface;
import com.gmail.genek530.utils.SwitchCommandParsers;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

//parsing is kinda shit but works
public class ArubaHPSwitch implements SwitchCommands {

    private LinkedHashMap<String, String> cachedUpDown;
    private LinkedHashMap<String, String> descriptions;
    private LinkedHashMap<Integer, String> cachedVlans;
    private LinkedHashMap<String, List<String>> loggingForInterface = new LinkedHashMap<>();
    private LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> taggedUntagged;

    //io
    private Map<String, String> replacable = new HashMap<>();
    private InputStream readConsole;
    private OutputStream writeConsole;
    //this contains last line so it knows where to stop reading
    private String stopReadingOnThat = null;

    public ArubaHPSwitch(InputDeviceInterface instance) throws Exception {
        replacable.put("-- MORE --, next page: Space, next line: Enter, quit: Control-C", " ");

        if(!instance.connectedTo().getModel().toLowerCase().contains("hp") && !instance.connectedTo().getModel().toLowerCase().contains("aruba") && !instance.connectedTo().getModel().toLowerCase().contains("procurve")) throw new Exception("Not a arubixonswitch");
        if(!instance.isAuthenticated()) throw new Exception("Should be authenticated before this is called might change in future");
        this.readConsole = instance.getReadConsole();
        this.writeConsole = instance.getWriteConsole();

        this.writeConsole.write("\n".getBytes());
        this.writeConsole.flush();

        List<String> data = ConnectionUtils.readTillMet(List.of(">", "#", "$"), readConsole, writeConsole);
        for (String datum : data) {
            if(datum.contains(">") | datum.contains("#") | datum.contains("$")){
                //no idea for huawei it works but for aruba it casues issues lol just replace it
                stopReadingOnThat = datum.replace(" ", "");
                break;
            }
        }
        if(stopReadingOnThat == null){
            throw new Exception("Couldn't detect next line failing");
        }
        System.out.println(stopReadingOnThat);
    }

    private List<String> sendCommandToSwitch(String command) throws Exception {
        ConnectionUtils.flushReader(this.readConsole);

        this.writeConsole.write((command + "\n").getBytes());
        this.writeConsole.flush();

        //aruba has weird characters first one is just hostname>command
        //then last is just hostname after the whole command output
        List<String> data = ConnectionUtils.readTillMet(List.of(this.stopReadingOnThat), readConsole, writeConsole, replacable);
        data = ConnectionUtils.readTillMet(List.of(this.stopReadingOnThat), readConsole, writeConsole, replacable);
        //old parsing is fragile it will die if last line is just empty new line
        data.remove(data.size() - 1);
        return data;
    }



    @Override
    public LinkedHashMap<String, String> getInterfaceUpDown() throws Exception {
        if(cachedUpDown != null){
            return cachedUpDown;
        }
        List<String> output = sendCommandToSwitch("show int br");
        boolean canStart = false;
        String configRow = "";

        //this can be poisoned with weird data at bottom or top data should be good
        List<String> configLines = new ArrayList<>();
        for (String s : output) {
            if(!canStart){
                if(s.contains("---")) {
                    canStart = true;
                    configRow = s;
                }
                continue;
            }
            configLines.add(s);
        }
        List<List<String>> parsed = SwitchCommandParsers.parseColumnsRows(configLines, configRow, '-');
        LinkedHashMap<String, String> collectorMap = new LinkedHashMap<>();
        //upper parsing managed to pickup somegarbage 28, 1000SX, No, Yes, Up, 1000FDx, NA, off, 0], [re.hi, hlight.ha, ngton# co, e.highl, ght.ha, tington#]]
        //parse it out here
        for (List<String> strings : parsed) {
            if(strings.size() < 4) continue;
            //for some reason int br has on sfp interfaces * but only on this command other commands don't show *
            String interfaceName = strings.get(0).replace("*", "");
            String enabled = strings.get(3);
            if(!enabled.contains("Yes") && !enabled.contains("No")){
                //problem with data
                continue;
            }
            String status = strings.get(4);
            if(!status.contains("Up") && !status.contains("Down")){
                throw new Exception("Issue with parsing up down  interface i know this is issue but don't know on which switches please give me it so i can check it");
            }
            collectorMap.put(interfaceName, status);
        }
        cachedUpDown = collectorMap;
        return collectorMap;
    }

    @Override
    public LinkedHashMap<String, String> getInteraceDescriptions() throws Exception {
        if(descriptions != null){
            return descriptions;
        }
        List<String> output = sendCommandToSwitch("show name");
        boolean canStart = false;
        String configRow = "";

        //this can be poisoned with weird data at bottom or top data should be good
        List<String> configLines = new ArrayList<>();

        for (String s : output) {
            if(!canStart){
                if(s.contains("---")) {
                    canStart = true;
                    configRow = s;
                }
                continue;
            }
            configLines.add(s);
        }
        List<List<String>> parsed = SwitchCommandParsers.parseColumnsRows(configLines, configRow, '-');
        LinkedHashMap<String, String> collectorMap = new LinkedHashMap<>();
        for (List<String> strings : parsed) {
            if(strings.size() != 3) continue;
            String interfaceName = strings.get(0);
            String description = strings.get(2);
            collectorMap.put(interfaceName, description);
        }
        descriptions = collectorMap;
        return collectorMap;
    }

    @Override
    public LinkedHashMap<Integer, String> getVlans() throws Exception {
        if(cachedVlans != null){
            return cachedVlans;
        }
        List<String> output = sendCommandToSwitch("show vlan");
        boolean canStart = false;
        String configRow = "";

        //this can be poisoned with weird data at bottom or top data should be good
        List<String> configLines = new ArrayList<>();

        for (String s : output) {
            if(!canStart){
                if(s.contains("---")) {
                    canStart = true;
                    configRow = s;
                }
                continue;
            }
            configLines.add(s);
        }
        List<List<String>> parsed = SwitchCommandParsers.parseColumnsRows(configLines, configRow, '-');
        LinkedHashMap<Integer, String> collectorMap = new LinkedHashMap<>();
        //upper parsing managed to pickup somegarbage 28, 1000SX, No, Yes, Up, 1000FDx, NA, off, 0], [re.hi, hlight.ha, ngton# co, e.highl, ght.ha, tington#]]
        //parse it out here
        for (List<String> strings : parsed) {
            if(strings.size() < 2) continue;
            String interfaceName = strings.get(0);
            Integer parsedInterger;
            try {
                parsedInterger = Integer.parseInt(interfaceName);
            } catch (Exception e){
                //problem with data
                continue;
            }
            String description = strings.get(1);
            collectorMap.put(parsedInterger, description);
        }
        cachedVlans = collectorMap;
        return collectorMap;
    }

    @Override
    public LinkedHashMap<String, List<String>> getLoggingForInterface() throws Exception {
        if(this.cachedUpDown == null) getInterfaceUpDown();
        if(!this.loggingForInterface.isEmpty()) return this.loggingForInterface;
        List<String> rawData = sendCommandToSwitch("show logging");

        for (Map.Entry<String, String> stringStringEntry : getInterfaceUpDown().entrySet()) {
            List<String> collector = new ArrayList<>();
            for (String s : rawData) {
                String[] splited = s.split(" ");
                boolean isInteresting = false;
                for (int i = 0; i < splited.length; i++) {
                    if(splited[i].contains("port")){
                        if(i + 1 >= splited.length){
                            continue;
                        }
                        if(splited[i+1].equals(stringStringEntry.getKey())){
                            isInteresting = true;
                        }
                    }
                    if(isInteresting){
                        collector.add(s.strip());
                    }
                }
                this.loggingForInterface.put(stringStringEntry.getKey(), collector);
            }

        }
        return this.loggingForInterface;
    }


    @Override
    public LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> getTaggedUntagged() throws Exception {
        if(cachedVlans == null) getVlans();
        if(taggedUntagged != null) return taggedUntagged;

        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> collector = new LinkedHashMap<>();

        for (Map.Entry<Integer, String> integerStringEntry : cachedVlans.entrySet()) {
            List<String> output = sendCommandToSwitch("show vlan " + integerStringEntry.getKey().toString() + "\n");

            boolean canStart = false;
            String configRow = "";

            //this can be poisoned with weird data at bottom or top data should be good
            List<String> configLines = new ArrayList<>();

            for (String s : output) {
                if(!canStart){
                    if(s.contains("---")) {
                        canStart = true;
                        configRow = s;
                    }
                    continue;
                }
                configLines.add(s);
            }
            List<List<String>> parsed = SwitchCommandParsers.parseColumnsRows(configLines, configRow, '-');
            TreeMap<Integer, String> collectorMap = new TreeMap<>();
            //upper parsing managed to pickup somegarbage 28, 1000SX, No, Yes, Up, 1000FDx, NA, off, 0], [re.hi, hlight.ha, ngton# co, e.highl, ght.ha, tington#]]
            //parse it out here
            for (List<String> strings : parsed) {
                if(strings.size() < 3){
                    continue;
                }
                if(!strings.get(1).contains("Tagged") && !strings.get(1).contains("Untagged")){
                    continue;
                }
                String interfaceName = strings.get(0);
                if(!collector.containsKey(interfaceName)){
                    LinkedHashMap<String, List<Integer>> inside = new LinkedHashMap<>();
                    inside.put("Untagged", new ArrayList<>());
                    inside.put("Tagged", new ArrayList<>());
                    collector.put(interfaceName, inside);
                }
                String taggedNotTagged = strings.get(1);

                if(taggedNotTagged.contains("Tagged")){
                    collector.get(interfaceName).get("Tagged").add(integerStringEntry.getKey());
                }
                if(taggedNotTagged.contains("Untagged")){
                    collector.get(interfaceName).get("Untagged").add(integerStringEntry.getKey());
                }
            }
        }
        this.taggedUntagged = collector;
        return collector;
    }
}
