package de.kastenklicker.secureserverbackup;

import static java.lang.System.getenv;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.SftpException;
import java.io.File;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class SFTPClientTest {

    private final String hostname = getenv("SECURE_SERVER_BACKUP_HOSTNAME");
    private final int port = 22;
    private final String username = getenv("SECURE_SERVER_BACKUP_USERNAME");
    private final String authentication = getenv("SECURE_SERVER_BACKUP_AUTHENTICATION_SFTP"); // password or path of private key file
    private final String knownHosts = "/home/sven/.ssh/known_hosts";
    private final int timeout = 20000;
    private final String remoteDirectory = "/home/kek"; // Must not end with slash

    @Test
    public void testUpload() throws JSchException, SftpException {

        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, knownHosts, timeout, remoteDirectory);

        sftpClient.upload(
                new File("./src/test/resources/zipTest/test.txt"));
    }

    @Test
    public void testUploadWrongDirectory() {

        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, knownHosts, timeout,  remoteDirectory + "/sus/kek");

        Exception exception = assertThrows(SftpException.class, () ->
                sftpClient.upload(
                    new File("./src/test/resources/zipTest/test.txt"))
        );

        assertTrue(exception.getMessage().contains("No such file"));
    }

    @AfterEach
    public void cleanUp() throws JSchException, SftpException {
        try {
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
        channelSftp.rm(remoteDirectory + "/test.txt");

        // Disconnect
        channelSftp.exit();
        session.disconnect();
        } catch (Exception e) {
            if (!e.getMessage().contains("No such file")) {
                throw e;
            }
        }
    }

}
