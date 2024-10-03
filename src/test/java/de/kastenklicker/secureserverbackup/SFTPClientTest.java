package de.kastenklicker.secureserverbackup;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jcraft.jsch.*;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

public class SFTPClientTest {

    private static GenericContainer<?> sftpContainer;
    
    private static File privateHostKey;
    private static File publicHostKey;

    private static String hostname;
    private static int port;
    private final String username = "foo";
    private final String authentication = "pass"; // password or path of private key file
    private final int timeout = 20000;
    private final String remoteDirectory = "/upload"; // Must not end with slash
    
    @BeforeAll
    public static void BeforeAll() throws IOException, JSchException {
        
        // Generate Test RSA Keys
        KeyPairGenerator keyPairGenerator = new KeyPairGenerator();
        keyPairGenerator.generate();
        publicHostKey = keyPairGenerator.getPublicKeyFile();
        privateHostKey = keyPairGenerator.getPrivateKeyFile();
        
        // Create & start Docker Container        
        sftpContainer = new GenericContainer<>("atmoz/sftp:alpine")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(privateHostKey.getAbsolutePath()),
                        "/etc/ssh/ssh_host_rsa_key"
                )
                .withExposedPorts(22)
                .withCommand("foo:pass:::upload");
        
        sftpContainer.start();

        hostname = sftpContainer.getHost();
        port = sftpContainer.getMappedPort(22);
    }
    
    @Test
    public void testUpload() throws JSchException, SftpException, IOException {

        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, publicHostKey.getPath(), timeout, remoteDirectory);

        sftpClient.upload(
                new File("./src/test/resources/zipTest/test.txt"));
        
        // Check if file was transferred correctly
        File testFile = new File("./src/test/resources/testUpload.txt");
        sftpContainer.copyFileFromContainer("/home/foo/upload/test.txt", testFile.getPath());
        assertTrue(testFile.delete());
    }
    
    @Test
    public void testUploadScanHostKey() throws JSchException, SftpException, IOException {
        
        if (!publicHostKey.delete() && publicHostKey.exists())
            throw new RuntimeException("Couldn't run test, because publicHostKey file couldn't be deleted");
        
        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, publicHostKey.getPath(), timeout, remoteDirectory);

        sftpClient.upload(
                new File("./src/test/resources/zipTest/test.txt"));

        // Check if file was transferred correctly
        File testFile = new File("./src/test/resources/testUpload.txt");
        sftpContainer.copyFileFromContainer("/home/foo/upload/test.txt", testFile.getPath());
        assertTrue(testFile.delete());
    }

    @Test
    public void testUploadWrongDirectory() {

        final SFTPClient sftpClient = new SFTPClient(hostname, port, username,
                authentication, publicHostKey.getPath(), timeout,  remoteDirectory + "/sus/kek");

        Exception exception = assertThrows(SftpException.class, () ->
                sftpClient.upload(
                    new File("./src/test/resources/zipTest/test.txt"))
        );

        assertTrue(exception.getMessage().contains("No such file"));
    }
    
    @AfterAll
    public static void afterAll() {
        
        // Stop Docker Container
        sftpContainer.stop();
        
        // Remove HostKeyFiles
        publicHostKey.delete();
        privateHostKey.delete();
        
    }

}
