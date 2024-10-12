package com.gmail.genek530.ssh.switchesconn.enddevice;

import java.util.LinkedHashMap;
import java.util.List;

//must implement linkedhashmaps as they have insertion order and i dont care about performance
//parsing must be able to continue with trash data
public interface SwitchCommands {
    public LinkedHashMap<String, String> getInterfaceUpDown() throws Exception;
    public LinkedHashMap<String, String> getInteraceDescriptions() throws Exception;
    public LinkedHashMap<Integer, String> getVlans() throws Exception;
    public LinkedHashMap<String, List<String>> getLoggingForInterface() throws Exception;
    public LinkedHashMap<String, LinkedHashMap<String, List<Integer>>> getTaggedUntagged() throws Exception;
}
