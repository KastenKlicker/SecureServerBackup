package de.kastenklicker.secureserverbackup;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Zip {

    private final ZipOutputStream zipOutputStream;
    private final List<String> excludeFiles;
    private final File mainDirectory;
    private final List<String> missingFiles = new ArrayList<>();

    public Zip(File backupFile, File mainDirectory , List<String> excludeFiles)
            throws FileNotFoundException {
        this.mainDirectory = mainDirectory;

        // Get unique path for every excluded file
        this.excludeFiles = excludeFiles.stream().map(path -> {
            try {
                return new File(mainDirectory, path).getCanonicalPath();
            } catch (IOException e) {
                missingFiles.add(path);
                return null;
            }
        }
        ).filter(Objects::nonNull).toList();

        // Create Zip OutputStream
        OutputStream fileOutputStream;

        fileOutputStream = new FileOutputStream(backupFile);

        zipOutputStream = new ZipOutputStream(fileOutputStream);
        zipOutputStream.setLevel(Deflater.DEFAULT_COMPRESSION);
    }

    /**
     * Get all files which weren't found.
     * @return Missing files list
     */
    public List<String> getMissingFiles() {
        return missingFiles;
    }

    /**
     * Internal helper file to zip file and directories recursive.
     * @param file file to add
     * @throws IOException Exceptions handled by the ExceptionListener
     */
    public void zip(File file) throws IOException {

        // Check if file should be excluded from the backup
        for (String path : excludeFiles) {
            if (file.getCanonicalPath().contains(path))
                return;
        }

        // If file is a directory, add all of its child files, if there are any
        if (file.isDirectory()) {
            Optional<File[]> optionalFiles = Optional.ofNullable(file.listFiles());
            if (optionalFiles.isPresent()) {
                for (File child : optionalFiles.get()) {
                    zip(child);
                }
            }
        } else {

            // File is a real file, so add it
            // Add Entry
            ZipEntry zipEntry = new ZipEntry(
                    file.getAbsolutePath().replace(mainDirectory.getAbsolutePath(), ""));

            zipOutputStream.putNextEntry(zipEntry);

            // Write file to zip
            byte[] buf = new byte['?'];

            FileInputStream fileInputStream = new FileInputStream(file);
            int len;
            while ((len = fileInputStream.read(buf)) > 0) {
                zipOutputStream.write(buf, 0, len);
            }
            fileInputStream.close();
        }
    }

    /**
     * Finish up the zip file
     */
    public void finish() throws IOException {
        zipOutputStream.flush();
        zipOutputStream.close();
    }
}
