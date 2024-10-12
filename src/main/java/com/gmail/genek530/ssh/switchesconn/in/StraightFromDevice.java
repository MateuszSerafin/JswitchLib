package com.gmail.genek530.ssh.switchesconn.in;

import com.gmail.genek530.ssh.switchdata.PreProcessSwitchInfo;
import com.gmail.genek530.ssh.switchesconn.auth.AuthWrapper;
import java.io.InputStream;
import java.io.OutputStream;

//todo this is causing zombie processes requires other solution
public class StraightFromDevice implements InputDeviceInterface {

    private Process bashProcess;
    private InputStream readConsole;
    private OutputStream writeConsole;
    private PreProcessSwitchInfo switchInfo;
    private boolean isAuthenticated = false;

    public StraightFromDevice(PreProcessSwitchInfo switchinfo) throws Exception {
        ProcessBuilder pb = new ProcessBuilder("/bin/bash");
        bashProcess = pb.start();


        Thread closeChildThread = new Thread() {
            public void run() {
                bashProcess.destroy();
            }
        };
        Runtime.getRuntime().addShutdownHook(closeChildThread);

        readConsole = bashProcess.getInputStream();
        writeConsole = bashProcess.getOutputStream();

        this.switchInfo = switchinfo;
    }

    @Override
    public boolean performLogin(boolean telnet, boolean ssh) throws Exception {
        if(isAuthenticated) return true;
        boolean result = AuthWrapper.AuthUsingMultipleMethods(readConsole, writeConsole, switchInfo, telnet, ssh);
        if(result) isAuthenticated = true;
        return result;
    }

    @Override
    public PreProcessSwitchInfo connectedTo() {
        return this.switchInfo;
    }

    public boolean isAuthenticated() {
        return isAuthenticated;
    }

    public InputStream getReadConsole() {
        return readConsole;
    }

    public OutputStream getWriteConsole() {
        return writeConsole;
    }

    @Override
    public void cleanup() {
        bashProcess.destroy();
    }
}
