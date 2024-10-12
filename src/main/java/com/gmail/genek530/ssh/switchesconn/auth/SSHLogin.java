package com.gmail.genek530.ssh.switchesconn.auth;

import com.gmail.genek530.ssh.switchdata.PreProcessSwitchInfo;
import com.gmail.genek530.utils.ConnectionUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

//yes this could use Map<String, String> from readtillmet but i didn't do it this way
public class SSHLogin implements LoginAttempt {

    private PreProcessSwitchInfo preProcessSwitchInfo;
    private InputStream readConsole;
    private OutputStream writeConsole;

    public SSHLogin(InputStream readConsole, OutputStream writeConsole, PreProcessSwitchInfo preProcessSwitchInfo) {
        this.preProcessSwitchInfo = preProcessSwitchInfo;
        this.readConsole = readConsole;
        this.writeConsole = writeConsole;
    }

    @Override
    public boolean login() throws Exception {
        if(preProcessSwitchInfo.getPassword() == null | preProcessSwitchInfo.getLogin() == null){
            throw new Exception("User or password is null");
        }

        //send interrupts and cleanup in case i messed up somewhere above
        writeConsole.write(3);
        writeConsole.write(3);
        writeConsole.write(3);
        writeConsole.write("\n".getBytes());
        writeConsole.flush();
        ConnectionUtils.flushReader(readConsole);

        writeConsole.write(("ssh " + preProcessSwitchInfo.getLogin() + "@" + preProcessSwitchInfo.getIp() + "\n").getBytes());
        writeConsole.flush();


        //2 options either are you sure or password
        List<String> raw = ConnectionUtils.readTillMet(List.of("are you sure you want to continue connecting", "password:"), readConsole, writeConsole);

        boolean areYouSure = false;
        boolean password = false;

        for (String s : raw) {
            if (s.toLowerCase().contains("password")) {
                writeConsole.write((preProcessSwitchInfo.getPassword() + "\n").getBytes());
                writeConsole.flush();
                password = true;
                break;
            }

            if (s.toLowerCase().contains("are you sure you want to continue connecting")) {
                writeConsole.write(("yes" + "\n").getBytes());
                writeConsole.flush();
                areYouSure = true;
                break;
            }
        }

        if(areYouSure){
            raw = ConnectionUtils.readTillMet(List.of("password:"), readConsole, writeConsole);

            for (String s : raw) {
                if (s.toLowerCase().contains("password:")) {
                    writeConsole.write((preProcessSwitchInfo.getPassword() + "\n").getBytes());
                    writeConsole.flush();
                    password = true;
                    break;
                }
            }
        }

        if(password){
            raw = ConnectionUtils.readTillMet(List.of("password:", ">", "#", "any key"), readConsole, writeConsole);
            for (String s : raw) {
                if (s.contains(">") | s.contains("#") | s.contains("$")) {
                    //pretty sure there is issue with aruba or something like that they want press anything to continue
                    writeConsole.write(" \n".getBytes());
                    writeConsole.flush();
                    return true;
                };

                if (s.toLowerCase().contains("password:")) {
                    return false;
                }

                if (s.toLowerCase().contains("any key")) {
                    writeConsole.write(" \n".getBytes());
                    writeConsole.flush();
                    return true;
                }

            }
        }

        throw new Exception("By default nothing should happen thus you see this message");
    }
}
