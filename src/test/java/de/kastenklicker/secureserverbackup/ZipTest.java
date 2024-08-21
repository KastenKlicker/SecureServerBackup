package de.kastenklicker.secureserverbackup;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

public class ZipTest {

    static File mainDirectory = new File("./src/test/resources/zipTest");
    Zip zip = new Zip(
                new File("./src/test/resources/zipTest/test.zip"),
                mainDirectory,
                new ArrayList<>());

    static File testFile = new File(mainDirectory, "test.zip");

    public ZipTest() throws FileNotFoundException {
    }

    @Test
    public void testZip() throws IOException {

        // Pack into zip
        File test = new File(mainDirectory, "test.txt");
        zip.zip(test);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 100);
    }

    @Test
    public void testZipFolder() throws IOException {

        // Pack into zip
        File test = new File(mainDirectory, "testDir/testDir.txt");
        zip.zip(test);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 100);
    }

    @Test
    public void testZipExclude() throws IOException {

        // Pack into zip
        ArrayList<String> excludeFiles = new ArrayList<>();
        excludeFiles.add("testDir");

        zip = new Zip(
                new File(mainDirectory,"test.zip"),
                mainDirectory,
                excludeFiles);

        File test = new File(mainDirectory, "testDir/testDir.txt");
        zip.zip(test);
        File testKek = new File(mainDirectory, "test.txt");
        zip.zip(testKek);
        zip.finish();

        assertTrue(testFile.exists());
        assertTrue(testFile.length() > 100 && testFile.length() < 200);
    }

    @AfterEach
    public void deleteTestFile() {
        if (!testFile.delete())
            throw new RuntimeException("Could not delete test file: " + testFile.getAbsolutePath());
    }

}
