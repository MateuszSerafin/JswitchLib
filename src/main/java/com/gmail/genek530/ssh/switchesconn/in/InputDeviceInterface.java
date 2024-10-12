package com.gmail.genek530.ssh.switchesconn.in;

import com.gmail.genek530.ssh.switchdata.PreProcessSwitchInfo;
import java.io.InputStream;
import java.io.OutputStream;

public interface InputDeviceInterface {
    public PreProcessSwitchInfo connectedTo();
    public boolean isAuthenticated();
    public InputStream getReadConsole();
    public OutputStream getWriteConsole();
    public void cleanup();
    public boolean performLogin(boolean telnet, boolean ssh) throws Exception;
}
