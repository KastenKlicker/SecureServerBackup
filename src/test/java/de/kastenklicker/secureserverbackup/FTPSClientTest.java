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
    
    private static final File parentDir = new File("./src/test/resources/cert/");
    private static final File certFile = new File(parentDir, "cert.pem");
    private static final File keyFile = new File(parentDir,  "key.pem");

    private static String hostname;
    private static int port;
    private static final String USERNAME = "alpineftp";
    private static final String AUTHENTICATION = "alpineftp"; // password
    private static final String REMOTE_DIRECTORY = "/ftp/alpineftp";
    private static final int PASSIV_PORT = 21000;
    
    @BeforeAll
    public static void BeforeAll() throws Exception {
        
        // Generate Certificate and Key file
        // Check if parent directory exists
        if (!parentDir.exists() && !parentDir.mkdir())
            throw new RuntimeException("Couldn't create certificate directory.");
        CertGenerator.generate(certFile, keyFile);
        
        // Create & start Docker Container
        ftpsContainer = new FixedHostPortGenericContainer<>("delfer/alpine-ftp-server:latest")
                .withFixedExposedPort(PASSIV_PORT, PASSIV_PORT)
                .withExposedPorts(21)
                .withEnv("MIN_PORT", String.valueOf(PASSIV_PORT))
                .withEnv("MAX_PORT", String.valueOf(PASSIV_PORT))
                .withEnv("TLS_CERT", "/letsencrypt/cert.pem")
                .withEnv("TLS_KEY", "/letsencrypt/key.pem")
                .withCopyFileToContainer(
                        MountableFile.forHostPath("cert_old/"),
                        "/letsencrypt"
                )
        ;
        
        ftpsContainer.start();
        
        hostname = ftpsContainer.getHost();
        port = ftpsContainer.getMappedPort(21);
    }

    /**
     * Checks if the upload is working fine
     * @throws Exception FTPS Exceptions
     */
    @Test
    public void testUpload() throws Exception {
        
        final FTPSClient ftpsClient = new FTPSClient(hostname, port, USERNAME,
                AUTHENTICATION, null, 0, REMOTE_DIRECTORY);
        
         ftpsClient.upload(
                new File("./src/test/resources/zipTest/test.txt"));

        // Check if file was transferred correctly
        File testFile = new File("./src/test/resources/testUpload.txt");
        ftpsContainer.copyFileFromContainer(REMOTE_DIRECTORY + "/test.txt", testFile.getPath());
        assertTrue(testFile.delete());
    }

    @AfterAll
    public static void afterAll() {

        // Stop Docker Container
        ftpsContainer.stop();
        
        // Delete certificates
        parentDir.delete();

    }
}
