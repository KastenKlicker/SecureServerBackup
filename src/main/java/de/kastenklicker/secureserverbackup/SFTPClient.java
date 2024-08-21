package de.kastenklicker.secureserverbackup;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;

public class SFTPClient extends UploadClient{

    public SFTPClient(String hostname, int port, String username, String authentication,
            String knownHosts, int timeout, String remoteDirectory) {
        super(hostname, port, username, authentication, knownHosts, timeout, remoteDirectory);
    }

    /**
     * Internal helper method to upload file to sftp server
     * @param backupFile file to upload
     * @throws JSchException Some exceptions
     * @throws SftpException Some exceptions
     */
    @Override
    public void upload(File backupFile)
            throws JSchException, SftpException {
        JSch jsch = new JSch();
        jsch.setKnownHosts(knownHosts);

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
