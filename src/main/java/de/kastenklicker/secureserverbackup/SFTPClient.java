package de.kastenklicker.secureserverbackup;

import com.jcraft.jsch.*;

import java.io.*;
import java.nio.file.Files;
import java.util.Base64;

public class SFTPClient extends UploadClient{

    public SFTPClient(String hostname, int port, String username, String authentication,
                      String hostKey, int timeout, String remoteDirectory) {
        super(hostname, port, username, authentication, hostKey, timeout, remoteDirectory);
    }

    /**
     * Internal helper method to upload file to sftp server
     * @param backupFile file to upload
     * @throws JSchException Some exceptions
     * @throws SftpException Some exceptions
     */
    @Override
    public void upload(File backupFile)
            throws JSchException, SftpException, IOException {
        JSch jsch = new JSch();
        
        
        File hostKeyFile = new File(knownHosts);
        String hostKeyFileName = hostKeyFile.getName();
        
        if (!hostKeyFile.exists())
            throw new RuntimeException("Couldn't add hostKey. " + hostKeyFileName +  " doesn't exists.");

        // Check if the given String is the knownHosts file or a HostKey
        if (hostKeyFileName.length() >= 4 &&    // File ist a public HostKey
                hostKeyFileName.substring(hostKeyFileName.length() - 4).equalsIgnoreCase(".pub")) {
            // Add public hostKey            
            String keyString = Files.readString(hostKeyFile.toPath()).trim();
            
            // Get just the Base64 part
            String[] hostKeyParts = keyString.split(" ");
            
            if (hostKeyParts.length != 3)
                throw new RuntimeException("Invalid hostKey format");
            
            keyString = hostKeyParts[1];
            
            // Decode the base64 string
            byte[] key = Base64.getDecoder().decode(keyString);
            HostKey hostKey = new HostKey(hostname, key);
            jsch.getHostKeyRepository().add(hostKey, null);
        }    
        else {  // File is a knownHosts file
            jsch.setKnownHosts(knownHosts);
        }
            
        
        Session session = jsch.getSession(username, hostname, port);
        
        // If string is path, then use key authentication, else use password authentication
        if (new File(authentication).exists()) {
            jsch.addIdentity(authentication);
        } else {
            session.setPassword(authentication);
        }

        session.connect(timeout);
        ChannelSftp channelSftp = (ChannelSftp) session.openChannel("sftp");

        // Upload
        channelSftp.connect(timeout);
        channelSftp.put(backupFile.getPath(), remoteDirectory);

        // Disconnect
        channelSftp.exit();
        session.disconnect();
    }

}
