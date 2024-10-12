package com.gmail.genek530.ssh.switchesconn.in;

import com.gmail.genek530.ssh.switchdata.PreProcessSwitchInfo;
import com.gmail.genek530.ssh.switchesconn.auth.AuthWrapper;
import com.jcraft.jsch.*;
import java.io.*;
import java.nio.charset.Charset;

public class GateWayConnection implements InputDeviceInterface {
    //boilerplate
    private String keyPath;
    private String keyPassword;
    private String gatewayUser;
    private String gateWayHost;
    private PreProcessSwitchInfo connectedTo;


    private boolean isAuthenticated = false;
    //io
    private Session session;
    private ChannelShell channel;
    private InputStream readConsole;
    private OutputStream writeConsole;

    public GateWayConnection(String keyPath, String keyPassword, String user, String gateWayHost, PreProcessSwitchInfo switchinfo) throws Exception {
        this.keyPath = keyPath;
        this.keyPassword = keyPassword;
        this.gatewayUser = user;
        this.gateWayHost = gateWayHost;
        this.connectedTo = switchinfo;
    }

    public InputStream getReadConsole() {
        return readConsole;
    }

    public OutputStream getWriteConsole() {
        return writeConsole;
    }

    @Override
    public PreProcessSwitchInfo connectedTo() {
        return this.connectedTo;
    }

    @Override
    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    @Override
    public void cleanup() {
        this.channel.disconnect();
        this.session.disconnect();
    }

    @Override
    public boolean performLogin(boolean telnet, boolean ssh) throws Exception {
        if(isAuthenticated) return true;
        JSch jSch = new JSch();
        jSch.addIdentity(keyPath, keyPassword.getBytes(Charset.defaultCharset()));
        java.util.Properties config = new java.util.Properties();
        config.put("StrictHostKeyChecking", "no");

        this.session = jSch.getSession(this.gatewayUser, gateWayHost);
        this.session.setConfig(config);
        session.connect();

        channel = (ChannelShell) session.openChannel("shell");
        this.channel.setPtySize(999,999,999,999);
        readConsole =  channel.getInputStream();
        writeConsole = channel.getOutputStream();
        channel.connect();
        boolean result = AuthWrapper.AuthUsingMultipleMethods(readConsole, writeConsole, connectedTo, telnet, ssh);
        if(result) isAuthenticated = true;
        return result;
    }
}