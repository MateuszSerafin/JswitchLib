package com.gmail.genek530.ssh.switchesconn.auth;

import com.gmail.genek530.ssh.switchdata.PreProcessSwitchInfo;
import com.gmail.genek530.utils.ConnectionUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

//yes this could use Map<String, String> from readtillmet but i didn't do it this way
public class TelnetAuth implements LoginAttempt {
    private PreProcessSwitchInfo preProcessSwitchInfo;
    private InputStream readConsole;
    private OutputStream writeConsole;

    public TelnetAuth(InputStream readConsole, OutputStream writeConsole, PreProcessSwitchInfo preProcessSwitchInfo) {
        this.preProcessSwitchInfo = preProcessSwitchInfo;
        this.readConsole = readConsole;
        this.writeConsole = writeConsole;
    }

    public boolean login() throws Exception {
        //send interrupts and cleanup in case i messed up somewhere above
        writeConsole.write(3);
        writeConsole.write(3);
        writeConsole.write(3);
        writeConsole.write("\n".getBytes());
        writeConsole.flush();
        ConnectionUtils.flushReader(readConsole);

        writeConsole.write(("telnet " + preProcessSwitchInfo.getIp() + "\n").getBytes());
        writeConsole.flush();

        //3 options i never seen something different
        //either logs in
        //asks for password
        //or username

        //this might throw but it's okay, should throw only on unexpected data
        List<String> raw = ConnectionUtils.readTillMet(List.of("username", "password:", ">", "#", "any key"), readConsole, writeConsole);
        boolean userName = false;
        for (String s : raw) {
            //todo this needs changing
            if (s.contains(">") | s.contains("#")) {
                System.out.println("is that called");
                //pretty sure there is issue with aruba or something like that they want press anything to continue
                writeConsole.write(" \n".getBytes());
                writeConsole.flush();
                return true;
            };
            if (s.toLowerCase().contains("username")) {
                System.out.println("username");
                if (preProcessSwitchInfo.getLogin() == null) {
                    throw new Exception("Telnet requires username provided switch no username");
                }
                userName = true;
                writeConsole.write((preProcessSwitchInfo.getLogin() + "\n").getBytes());
                writeConsole.flush();
                break;
            }
            if (s.toLowerCase().contains("password:")) {
                if (preProcessSwitchInfo.getPassword() == null) {
                    throw new Exception("Telnet requires password but switch provided has no password");
                }
                writeConsole.write((preProcessSwitchInfo.getPassword() + "\n").getBytes());
                writeConsole.flush();
                break;
            }
            if (s.toLowerCase().contains("any key")) {
                writeConsole.write(" \n".getBytes());
                writeConsole.flush();
                return true;
            }

        }

        if (userName) {
            //realistically it will fail if it doesn't ask for password it must ask for password
            //Helpers.peekAtData(readConsole);
            ConnectionUtils.readTillMet(List.of("password:", ">", "#"), readConsole, writeConsole);
            writeConsole.write((preProcessSwitchInfo.getPassword() + "\n").getBytes());
            writeConsole.flush();
        }
        raw = ConnectionUtils.readTillMet(List.of("username", "password:", ">", "#", "any key"), readConsole, writeConsole);
        for (String s : raw) {
            if (s.contains(">") | s.contains("#") | s.contains("$")) {
                //pretty sure there is issue with aruba or something like that they want press anything to continue
                writeConsole.write(" \n".getBytes());
                writeConsole.flush();
                return true;
            };

            if (s.toLowerCase().contains("any key")) {
                writeConsole.write(" \n".getBytes());
                writeConsole.flush();
                return true;
            }

            if (s.toLowerCase().contains("username")) {
                return false;
            }

            if (s.toLowerCase().contains("password:")) {
                return false;
            }
        }
        throw new Exception("By default nothing should happen thus you see this message");
    }
}
