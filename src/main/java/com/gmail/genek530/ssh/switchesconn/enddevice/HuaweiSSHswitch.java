package com.gmail.genek530.ssh.switchesconn.enddevice;

import com.gmail.genek530.utils.ConnectionUtils;
import com.gmail.genek530.ssh.switchesconn.in.InputDeviceInterface;
import com.gmail.genek530.utils.SwitchCommandParsers;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class HuaweiSSHswitch implements SwitchCommands {
    //cache
    private LinkedHashMap<String, String> cachedUpDown;
    private LinkedHashMap<String, String> descriptions;
    private LinkedHashMap<Integer, String> cachedVlans;
    private LinkedHashMap<String, List<String>> loggingForInterface = new LinkedHashMap<>();
    //the problem is u can have LAG interface and it wont show under normal interfaces this is the only thing that needs extra care
    //that means that first key can not exist
    private LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> taggedUntagged;

    //io
    private Map<String, String> replacable = new HashMap<>();
    private InputStream readConsole;
    private OutputStream writeConsole;
    //this contains last line so it knows where to stop reading
    private String stopReadingOnThat = null;



    public HuaweiSSHswitch(InputDeviceInterface instance) throws Exception {
        replacable.put("---- More ----", " ");

        if(!instance.connectedTo().getModel().toLowerCase().contains("huawei")) throw new Exception("Not a hujawej switch");
        if(!instance.isAuthenticated()) throw new Exception("Should be authenticated before this is called might change in future");
        this.readConsole = instance.getReadConsole();
        this.writeConsole = instance.getWriteConsole();

        this.writeConsole.write("\n".getBytes());
        this.writeConsole.flush();

        List<String> data = ConnectionUtils.readTillMet(List.of(">", "#", "$"), readConsole, writeConsole);
        for (String datum : data) {
            if(datum.contains(">") | datum.contains("#") | datum.contains("$")){
                stopReadingOnThat = datum;
                break;
            }
        }
        if(stopReadingOnThat == null){
            throw new Exception("Couldn't detect next line failing");
        }
    }

    private List<String> sendCommandToSwitch(String command) throws Exception {
        ConnectionUtils.flushReader(this.readConsole);

        this.writeConsole.write((command + "\n").getBytes());
        this.writeConsole.flush();
        Thread.sleep(3000);
        List<String> data = ConnectionUtils.readTillMet(List.of(stopReadingOnThat), this.readConsole, writeConsole, replacable);
        //old parsing is fragile it will die if last line is just empty new line
        data.remove(data.size() - 1);
        return data;
    }

    @Override
    public LinkedHashMap<String, String> getInterfaceUpDown() throws Exception {
        if(this.cachedUpDown != null) return this.cachedUpDown;
        this.getInteraceDescriptions();
        return this.cachedUpDown;
    }

    @Override
    public LinkedHashMap<String, String> getInteraceDescriptions() throws Exception {
        if(this.descriptions != null) return this.descriptions;
        List<String> preprocessed = sendCommandToSwitch("dis int desc");
        List<String> data = new ArrayList<>();
        boolean started = false;
        for (String line : preprocessed) {
            if(line.contains("Interface")){
                started = true;
                continue;
            }
            if(started) data.add(line);
        }
        List<List<String>> simpleParsed = SwitchCommandParsers.BetterSpaceSpliter(data);

        LinkedHashMap<String, String> collectorDescriptions = new LinkedHashMap();
        LinkedHashMap<String, String> collectorProtocol = new LinkedHashMap();

        for (List<String> strings : simpleParsed) {
            int i = 0;
            //if(strings.size() < 4) continue;

                String InterfaceName = strings.get(i);
                if(InterfaceName.contains("GE")) {
                    InterfaceName = InterfaceName.replace("GE", "GigabitEthernet");
                }
                int protocolIndex = i + 2;
                if(protocolIndex >= strings.size()) {
                    throw new Exception("should not happen");
                }
                collectorProtocol.put(InterfaceName, strings.get(protocolIndex));
                if(protocolIndex + 1 >= strings.size()){
                    collectorDescriptions.put(InterfaceName, "");
                } else{
                    StringBuilder builder = new StringBuilder();
                    for (int i1 = protocolIndex + 1; i1 < strings.size(); i1++) {
                        builder.append(strings.get(i1).replace("\r", ""));
                        builder.append(" ");
                    }
                    collectorDescriptions.put(InterfaceName, builder.toString().strip());
                }
            }

        this.descriptions = collectorDescriptions;
        this.cachedUpDown = collectorProtocol;
        return collectorDescriptions;
    }

    @Override
    public LinkedHashMap<Integer, String> getVlans() throws Exception {
        if(this.cachedVlans == null) getTaggedUntagged();
        return this.cachedVlans;
    }

    @Override
    public LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> getTaggedUntagged() throws Exception {
        if(this.taggedUntagged != null) return this.taggedUntagged;
        List<String> data = sendCommandToSwitch("dis vlan");

        LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> vlanMappings = new LinkedHashMap<>();
        List<String> taggedUntaggedRaw = new ArrayList<>();
        List<String> vlanDesc = new ArrayList<>();

        int vid = 0;
        for (int i = 0; i < data.size(); i++) {
           if(data.get(i).contains("VID")){
               vid += 1;
           }
           if(vid == 1){
               taggedUntaggedRaw.add(data.get(i));
           }
           if(vid == 2){
               vlanDesc.add(data.get(i));
           }
        }

        Iterator<String> iterator = taggedUntaggedRaw.iterator();
        int vlanCurrentlyProcessed = 0;
        boolean tagged = false;
        boolean untagged = true;
        Integer vlanID = null;
        while(iterator.hasNext()){
            String taggedraw = iterator.next();
            if(taggedraw.contains("TG:")){
                tagged = true;
                untagged = false;
                taggedraw = taggedraw.replace("TG:", "");
            }
            if(taggedraw.contains("UT:")){
                untagged = true;
                tagged = false;
                taggedraw = taggedraw.replace("UT:", "");
            }
            List<String> tokenized = SwitchCommandParsers.BetterSpaceSpliter(taggedraw.strip());
            try{
                vlanID = Integer.parseInt(tokenized.get(0));
            } catch (Exception e){
                if(vlanID == null) continue;
            }
            for (String s : tokenized) {
                if(!s.contains("GE")){
                    continue;
                }
                String shortName = s;
                String fullName = shortName.replace("GE", "GigabitEthernet").replaceAll("\\(.*?\\)", "");;
                if(!vlanMappings.containsKey(fullName)){
                    LinkedHashMap<String, List<Integer>> inside = new LinkedHashMap<>();
                    inside.put("Tagged", new ArrayList<>());
                    inside.put("Untagged", new ArrayList<>());
                    vlanMappings.put(fullName, inside);
                }
                if(tagged){
                    vlanMappings.get(fullName).get("Tagged").add(vlanID);
                }
                if(untagged){
                    vlanMappings.get(fullName).get("Untagged").add(vlanID);
                }
            }
        }
        this.taggedUntagged = vlanMappings;

        LinkedHashMap<Integer, String> vlanDescriptions =  new LinkedHashMap<>();
        for (String s : vlanDesc) {
            List<String> tokenized = SwitchCommandParsers.BetterSpaceSpliter(s);
            Integer descVlanID = null;
            try{
                descVlanID = Integer.parseInt(tokenized.get(0));
            } catch (Exception e){
                continue;
            }
            StringBuilder builder = new StringBuilder();
            for (int i1 = 5; i1 < tokenized.size(); i1++) {
                builder.append(tokenized.get(i1).replace("\r", ""));
                builder.append(" ");
            }
            vlanDescriptions.put(descVlanID, builder.toString().strip());
        }
        this.cachedVlans = vlanDescriptions;
        return this.taggedUntagged;
    }

    @Override
    public LinkedHashMap<String, List<String>> getLoggingForInterface() throws Exception {
        List<String> data = sendCommandToSwitch("display logbuffer size 1024");
        for (Map.Entry<String, String> stringStringEntry : getInterfaceUpDown().entrySet()) {
            List<String> collector = new ArrayList<>();
            for (String s : data) {
                String[] splited = s.split(" ");
                boolean isInteresting = false;
                for (int i = 0; i < splited.length; i++) {
                    if(splited[i].contains("Interface")){
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
}