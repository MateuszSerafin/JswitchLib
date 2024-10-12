package com.gmail.genek530.ssh.switchdata;

import com.gmail.genek530.utils.IPregex;

public class PreProcessSwitchInfo {
    
    private String ip;
    private String asset;
    private String model;

    private String login = "";

    private String password = "";

    public PreProcessSwitchInfo(String ip, String asset, String model, String login, String password) throws Exception {
        if(!IPregex.isValidIPAddress(ip)) throw new Exception("Incorrect ip");
        this.ip = ip;
        this.asset = asset;
        this.model = model;
        this.login = login;
        this.password = password;
    }

    public PreProcessSwitchInfo(String ip, String asset, String model) throws Exception {
        if(!IPregex.isValidIPAddress(ip)) throw new Exception("Incorrect ip");
        this.ip = ip;
        this.asset = asset;
        this.model = model;
    }

    public String getIp() {
        return ip;
    }

    public String getAsset() {
        return asset;
    }

    public String getModel() {
        return model;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getLogin() {
        return login;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }


    @Override
    public String toString() {
        if(this.password.isEmpty()){
            return this.getIp() + " Asset:" + this.getAsset() + " Model:" + this.getModel() + " <Only rsa authentication>";
        }
        return this.getIp() + " Asset:" + this.getAsset() + " Model:" + this.getModel() + " Login:" + this.getLogin() + " Pass:" + this.getPassword();
    }
}
