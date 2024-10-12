package com.gmail.genek530.ssh.switchesconn.auth;

import com.gmail.genek530.ssh.switchdata.PreProcessSwitchInfo;
import com.gmail.genek530.utils.ConnectionUtils;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

//yes this could use Map<String, String> from readtillmet but i didn't do it this way
public class SSHnoUser implements LoginAttempt {

    private PreProcessSwitchInfo preProcessSwitchInfo;
    private InputStream readConsole;
    private OutputStream writeConsole;

    public SSHnoUser(InputStream readConsole, OutputStream writeConsole, PreProcessSwitchInfo preProcessSwitchInfo){
        this.preProcessSwitchInfo = preProcessSwitchInfo;
        this.readConsole = readConsole;
        this.writeConsole = writeConsole;
    }

    @Override
    public boolean login() throws Exception {
        //send interrupts and cleanup in case i messed up somewhere above
        writeConsole.write(3);
        writeConsole.write(3);
        writeConsole.write(3);
        writeConsole.write("\n".getBytes());
        writeConsole.flush();
        ConnectionUtils.flushReader(readConsole);

        writeConsole.write(("ssh " + preProcessSwitchInfo.getIp() + "\n").getBytes());
        writeConsole.flush();

        //3 options i never seen something different
        //either logs in
        //asks for password
        //or Are you sure you want to continue connecting (yes/no)?
        List<String> raw = ConnectionUtils.readTillMet(List.of("are you sure you want to continue connecting", "password:", ">", "#", "any key"), readConsole, writeConsole);
        boolean areYouSure = false;

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

            if (s.toLowerCase().contains("are you sure you want to continue connecting")) {
                writeConsole.write(("yes" + "\n").getBytes());
                writeConsole.flush();
                areYouSure = true;
                break;
            }

            if (s.toLowerCase().contains("any key")) {
                writeConsole.write(" \n".getBytes());
                writeConsole.flush();
                return true;
            }
        }

        if(areYouSure){
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
