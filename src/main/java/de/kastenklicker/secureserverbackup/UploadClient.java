package de.kastenklicker.secureserverbackup;

import java.io.File;

public abstract class UploadClient {

    protected final String hostname;
    protected final int port;
    protected String username;
    protected final String authentication; // password or path of private key file
    protected final String knownHosts;
    protected final int timeout;
    protected final String remoteDirectory;

    public UploadClient(String hostname, int port, String username, String authentication,
            String knownHosts, int timeout, String remoteDirectory) {
        this.hostname = hostname;
        this.port = port;
        this.username = username;
        this.authentication = authentication;
        this.knownHosts = knownHosts;
        this.timeout = timeout;
        this.remoteDirectory = remoteDirectory;
    }

    public abstract void upload(File file) throws Exception;
}
