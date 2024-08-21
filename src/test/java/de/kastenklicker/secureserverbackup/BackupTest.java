package de.kastenklicker.secureserverbackup;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
        UploadClient uploadClient = new SFTPClient(
                "135.181.154.214", 22,
                "root",
                "/home/sven/.ssh/id_ed25519",
                "/home/sven/.ssh/known_hosts",
                20000,
                "/root");

        assertTrue(new Backup(
                new ArrayList<>(),
                backupsDirectory,
                new File("./src/test/resources/zipTest"),
                uploadClient,
                1)
                .backup().exists());
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
