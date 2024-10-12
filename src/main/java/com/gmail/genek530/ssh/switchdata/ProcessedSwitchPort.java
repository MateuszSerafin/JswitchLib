package com.gmail.genek530.ssh.switchdata;

import javax.annotation.Nullable;
import java.util.List;

public class ProcessedSwitchPort {
    private String name;
    private String description;
    private String upDown;
    @Nullable
    private Integer untagged;
    private List<Integer> tagged;
    private List<String> log;

    public ProcessedSwitchPort(String name, String description, String upDown, @Nullable Integer untagged, @Nullable List<Integer> tagged, List<String> logRelatedToThisInterfaceOnly){
        this.name = name;
        this.description = description;
        this.upDown = upDown;
        this.untagged = untagged;
        this.tagged = tagged;
        this.log = logRelatedToThisInterfaceOnly;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getUpDown() {
        return upDown;
    }

    @Nullable
    public List<Integer> getTagged() {
        return tagged;
    }

    @Nullable
    public Integer getUntagged() {
        return untagged;
    }

    public String isInterestingForPatching(){
        String lowerDesc  = this.description.toLowerCase();
        if(lowerDesc.contains("uplink") | lowerDesc.contains("downlink")){
            return "Not required";
        }
        if(lowerDesc.contains("ap")){
            if(untagged == null){
                return "is ap doesn't tag anywhere should be subscriber";
            }
            if(tagged != null) {
                if(untagged != 4){
                   return "is ap untagged not subscriber";
                }
            }
            return "Not required";
        }
        if(lowerDesc.contains("subscriber")){
            if(tagged != null) {
                if (!this.tagged.isEmpty()) return "Subscriber but tags somewhere";
            }
            if(this.untagged != null){
                if(this.untagged != 4){
                    return "Subscriber but vlan is different than 4";
                }
                return "Not required";
            }
            return "is subscriber but doesn't tag anywhere";
        }

        if(lowerDesc.contains("disabled")){
            if(tagged != null){
                if(!this.tagged.isEmpty()) return "taggs something even its disabled";
            }
            if(this.untagged != null){
                if(this.untagged != 1){
                    return "Have disabled description but is untagged on other vlan than 1";
                }
                return "Not required";
            }
            return "is disabled but it's not part of 1 untagged";
        }

        if(lowerDesc.contains("future expansion")){
            if(tagged != null) {
                if (!this.tagged.contains(20)) {
                    return "future expansion should contain 20 tagged";

                }
                if (this.tagged.size() > 1) {
                    return "more than one tagged for future expansion";
                }
            }
            if (this.untagged != null) {
                return "future expansion shouldn't untag";
            }
            return "Not required";
        }

        return "<requires manual look>";
    }
}
