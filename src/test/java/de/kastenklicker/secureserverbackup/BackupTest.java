package de.kastenklicker.secureserverbackup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

public class BackupTest {

    File backupsDirectory = new File("./src/test/resources/zipTest/backups");

    /**
     * Setup FileConfiguration and backup directory
     */
    @BeforeEach
    public void setup() {
        if (!backupsDirectory.mkdirs()) {
            throw new RuntimeException("Couldn't create test backup dir. Does it already exists?");
        }
    }

    @Test
    public void testBackup() throws Exception {

        assertTrue(new Backup(
                new ArrayList<>(),
                backupsDirectory,
                new File("./src/test/resources/zipTest"),
                new NullUploadClient(),
                1)
                .backup().exists());
    }

    @Test
    public void testBackupSFTP() throws Exception {

        // Generate Test RSA Keys
        KeyPairGenerator keyPairGenerator = new KeyPairGenerator();
        keyPairGenerator.generate();
        File publicHostKey = keyPairGenerator.getPublicKeyFile();
        File privateHostKey = keyPairGenerator.getPrivateKeyFile();
        
        // Create & start Docker Container        
        GenericContainer<?> sftpContainer = new GenericContainer<>("atmoz/sftp:alpine")
                .withCopyFileToContainer(
                        MountableFile.forHostPath(privateHostKey.getAbsolutePath()),
                        "/etc/ssh/ssh_host_rsa_key"
                )
                .withExposedPorts(22)
                .withCommand("foo:pass:::upload");

        sftpContainer.start();
        
        // The real testing
        UploadClient uploadClient = new SFTPClient(
                sftpContainer.getHost(), sftpContainer.getMappedPort(22), 
                "foo",
                "pass",
                publicHostKey.getPath(), 
                20000,
                "/upload");

        assertTrue(new Backup(
                new ArrayList<>(),
                backupsDirectory,
                new File("./src/test/resources/zipTest"),
                uploadClient,
                1)
                .backup().exists());
        
        // Clean up
        publicHostKey.delete();
        privateHostKey.delete();
    }

    @Test
    public void testBackupMaxDirSizeReached() throws Exception {

        // Create RandomAccessFile in backupDir
        RandomAccessFile randomAccessFile = new RandomAccessFile(
                "./src/test/resources/zipTest/backups/temporaryTestFile","rw");

        //randomAccessFile.setLength(1024*1024*1024); // KB -> MB -> GB
        randomAccessFile.setLength(1024*1024*1024); // KB -> MB -> GB

        // Write RandomAccessFile into backups directory
        randomAccessFile.close();

        // Check if it was created
        assertTrue(new File("./src/test/resources/zipTest/backups/temporaryTestFile").exists());

        assertTrue(new Backup(
                new ArrayList<>(),
                backupsDirectory,
                new File("./src/test/resources/zipTest"),
                new NullUploadClient(),
                1)
                .backup().exists());

        // Check if RandomAccessFile still exist - it shouldn't
        assertFalse(new File("./src/test/resources/zipTest/backups/temporaryTestFile").exists());
    }

    @AfterEach
    public void cleanUp() {
        for (File file : backupsDirectory.listFiles()) {
            file.delete();
        }
        backupsDirectory.delete();
    }
}
