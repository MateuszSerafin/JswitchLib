package com.gmail.genek530.ssh.switchdata;

import com.gmail.genek530.ssh.switchesconn.enddevice.SwitchCommands;

import java.util.*;

public class ProcessedSSHswitch {
    private PreProcessSwitchInfo preProcessSwitchInfo;
    private LinkedHashMap<Integer, String> vlans;
    private List<ProcessedSwitchPort> array = new ArrayList<>();

    public ProcessedSSHswitch(SwitchCommands connectedSwitch, PreProcessSwitchInfo preProcessSwitchInfo) throws Exception {
        this.preProcessSwitchInfo = preProcessSwitchInfo;
        //lets not allow gson to serialize it and store it on disk
        this.preProcessSwitchInfo.setLogin("<removed login>");
        this.preProcessSwitchInfo.setPassword("<removed password>");
        //cache everything
        connectedSwitch.getInterfaceUpDown();
        this.vlans = connectedSwitch.getVlans();
        connectedSwitch.getInteraceDescriptions();
        connectedSwitch.getTaggedUntagged();
        //connectedSwitch.getLoggingForInterface();

        //| connectedSwitch.getLoggingForInterface().size() != problemIfDifferent

        //int problemIfDifferent = connectedSwitch.getInteraceDescriptions().size();
        //if(connectedSwitch.getInterfaceUpDown().size() != problemIfDifferent | connectedSwitch.getInteraceDescriptions().size() != problemIfDifferent | connectedSwitch.getTaggedUntagged().size() != problemIfDifferent ){
        //    throw new Exception("Somehow processed data doesn't have equal sizes this is 100% issue with parsing on this switch provide me information about this switch so i can verify");
        //}

        for (Map.Entry<String, String> nameDescription : connectedSwitch.getInteraceDescriptions().entrySet()) {
            String interfaceName = nameDescription.getKey();
            String description = nameDescription.getValue();

            LinkedHashMap<String, List<Integer>> parsed = connectedSwitch.getTaggedUntagged().get(interfaceName);
            List<Integer> untagged = null;
            List<Integer> tagged = null;
            if(parsed == null){
                untagged = null;
                tagged = null;
            } else {
                untagged = parsed.get("Untagged");
                tagged = parsed.get("Tagged");
            }

            Integer untaggedInt = null;
            if(untagged != null){
                if(untagged.size() > 1){
                    throw new Exception("Some how there are 2 values for untagged i don't know");
                }
            }
            if(untagged != null){
                if(untagged.size() == 1){
                    untaggedInt = untagged.get(0);
                }
            }

            String upDown = connectedSwitch.getInterfaceUpDown().get(interfaceName);
            //List<String> log = connectedSwitch.getLoggingForInterface().get(interfaceName);
            //System.out.println(connectedSwitch.getLoggingForInterface());
            this.array.add(new ProcessedSwitchPort(interfaceName, description, upDown, untaggedInt, tagged, null));
        }
    }

    public LinkedHashMap<Integer, String> getVlans() {
        return vlans;
    }

    public PreProcessSwitchInfo getPreProcessSwitchInfo() {
        return preProcessSwitchInfo;
    }

    public List<ProcessedSwitchPort> getProcessedPorts() {
        return array;
    }

    @Override
    public String toString() {
        return "Processed switch object " + this.preProcessSwitchInfo.getIp();
    }
}

