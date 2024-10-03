package de.kastenklicker.secureserverbackup;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.FixedHostPortGenericContainer;
import org.testcontainers.utility.MountableFile;

import java.io.File;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class FTPSClientTest {

    private static FixedHostPortGenericContainer<?> ftpsContainer;

    private static String hostname;
    private static int port;
    private static final String username = "alpineftp";
    private static final String authentication = "alpineftp"; // password or path of private key file
    private final String remoteDirectory = "/ftp/alpineftp";
    
    @BeforeAll
    public static void BeforeAll() {
        // Create & start Docker Container
        ftpsContainer = new FixedHostPortGenericContainer<>("delfer/alpine-ftp-server:latest")
                .withFixedExposedPort(21000, 21000)
                .withExposedPorts(21)
                .withEnv("MIN_PORT", "21000")
                .withEnv("MAX_PORT", "21000")
                .withEnv("TLS_CERT", "/letsencrypt/cert.pem")
                .withEnv("TLS_KEY", "/letsencrypt/key.pem")
                .withCopyFileToContainer(
                        MountableFile.forClasspathResource("cert/"),
                        "/letsencrypt"
                )
        ;
        
        ftpsContainer.start();
        
        hostname = ftpsContainer.getHost();
        port = ftpsContainer.getMappedPort(21);
    }
    
    @Test
    public void testUpload() throws Exception {
        
        final FTPSClient ftpsClient = new FTPSClient(hostname, port, username,
                authentication, null, 0, remoteDirectory);
        
         ftpsClient.upload(
                new File("./src/test/resources/zipTest/test.txt"));

        // Check if file was transferred correctly
        File testFile = new File("./src/test/resources/testUpload.txt");
        ftpsContainer.copyFileFromContainer("/ftp/alpineftp/test.txt", testFile.getPath());
        assertTrue(testFile.delete());
    }

    @AfterAll
    public static void afterAll() {

        // Stop Docker Container
        ftpsContainer.stop();

    }
}
