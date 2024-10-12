package com.gmail.genek530.ssh.switchesconn.auth;

import com.gmail.genek530.logger.LoggingMain;
import com.gmail.genek530.ssh.switchdata.PreProcessSwitchInfo;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class AuthWrapper {
    public static boolean AuthUsingMultipleMethods(InputStream readConsole, OutputStream writeConsole, PreProcessSwitchInfo switchInfo, boolean tryTelnet, boolean trySSH) throws Exception {
        //LoggingMain.getLogger().info("Trying to auth using SSH: " + trySSH + "  Telnet: " + tryTelnet);
        ArrayList<LoginAttempt> interfaces = new ArrayList<>();
        if(tryTelnet){
            interfaces.add(new TelnetAuth(readConsole, writeConsole, switchInfo));
        }
        if(trySSH){
            interfaces.add(new SSHnoUser(readConsole, writeConsole, switchInfo));
            interfaces.add(new SSHLogin(readConsole, writeConsole, switchInfo));
        }
        if(interfaces.isEmpty()) throw new Exception("incorrect authmethods selected");
        for (LoginAttempt anInterface : interfaces) {
            //LoggingMain.getLogger().info("Success for " + anInterface.getClass().getName());
            if(anInterface.login()) return true;
        }
        //LoggingMain.getLogger().info("Failed for all auth types");
        return false;
    }
}
