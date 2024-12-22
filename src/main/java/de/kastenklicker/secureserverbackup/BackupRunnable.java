package de.kastenklicker.secureserverbackup;

import java.io.File;
import java.util.List;

import de.kastenklicker.secureserverbackuplibrary.Backup;
import de.kastenklicker.secureserverbackuplibrary.upload.FTPSClient;
import de.kastenklicker.secureserverbackuplibrary.upload.NullUploadClient;
import de.kastenklicker.secureserverbackuplibrary.upload.SFTPClient;
import de.kastenklicker.secureserverbackuplibrary.upload.UploadClient;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitWorker;

public class BackupRunnable extends BukkitRunnable {

    private final List<String> excludeFiles;
    private final File backupDirectory;
    private final File mainDirectory;
    private final UploadClient uploadClient;
    private final long maxBackupDirectorySize;

    private final BackupLogger backupLogger;

    /**
     * Constructor sets up everything needed to back up the server
     */
    public BackupRunnable() {

        // Setup custom logging
        this.backupLogger = new BackupLogger();
        backupLogger.info("Starting backup...");

        // Get plugin information
        Plugin plugin = SecureServerBackup.getPlugin(SecureServerBackup.class);
        FileConfiguration configuration = plugin.getConfig();

        mainDirectory = plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();

        // Get configs
        excludeFiles = configuration.getStringList("excludedFiles");
        backupDirectory = new File(mainDirectory, configuration.getString("backupFolder", "backups"));
        maxBackupDirectorySize = configuration.getLong("maxBackupFolderSize")
                * (1000*1000*1000); // KB*MB*GB;

        // Get upload information
        String hostname = configuration.getString("hostname");
        int port = configuration.getInt("port");
        String username = configuration.getString("username");
        String authentication = configuration.getString("authentication");
        String knownHosts = configuration.getString("knownHosts");
        int timeout = configuration.getInt("timeout")*1000;
        String remoteDirectory = configuration.getString("remoteDirectory");

        switch (configuration.getString("uploadProtocol", "")) {
            case "sftp":
                if (knownHosts == null)
                    throw new NullPointerException("Read null for knownHosts! Check your config.yml.");
                uploadClient = new SFTPClient(hostname, port, username, authentication,
                        new File(knownHosts), timeout, remoteDirectory);
                break;
                
            case "ftps":
                uploadClient = new FTPSClient(hostname, port, username, authentication, remoteDirectory);
                break;

            default:
                uploadClient = new NullUploadClient();
        }
    }

    @Override
    public void run() {
        /*
        Check if backup is already running
         */

        // Get all workers
        List<BukkitWorker> workers =SecureServerBackup
                .getPlugin(SecureServerBackup.class)
                .getServer()
                .getScheduler()
                .getActiveWorkers();

        // Get the workers of the plugin
        workers = workers
                .stream()
                .filter(bukkitWorker ->
                        bukkitWorker.getThread().getName().contains("SecureServerBackup"))
                .toList();

        // If the numbers of workers is greater than 1, an update is already running
        if (workers.size() > 1) {
            backupLogger.warning("A backup is already running, cancelling newer backup task");
            this.cancel();
            return;
        }

        // Backup files
        Backup backup = new Backup(excludeFiles, backupDirectory,
                mainDirectory, uploadClient, maxBackupDirectorySize);
        try {
            backup.backup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        backupLogger.info("Finished Backup.");
    }
}
