package de.kastenklicker.secureserverbackup;

import java.io.File;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Logger;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.scheduler.BukkitWorker;

/**
 * Class to integrate Backup functionality with paper
 */
public class BackupConsumerTask implements Consumer<BukkitTask> {

    private final List<String> excludeFiles;
    private final File backupDirectory;
    private final File mainDirectory;
    private final UploadClient uploadClient;
    private final Logger logger;
    private final long maxBackupDirectorySize;

    private final BackupLogger backupLogger;

    /**
     * Constructor sets up everything needed to back up the server
     */
    public BackupConsumerTask() {

        // Setup custom logging
        this.backupLogger = new BackupLogger();
        backupLogger.info("Starting backup...");

        // Get plugin information
        Plugin plugin = SecureServerBackup.getPlugin(SecureServerBackup.class);
        logger = plugin.getLogger();
        FileConfiguration configuration = plugin.getConfig();

        mainDirectory = plugin.getDataFolder().getAbsoluteFile().getParentFile().getParentFile();

        // Get configs
        excludeFiles = configuration.getStringList("excludedFiles");
        backupDirectory = new File(mainDirectory, configuration.getString("backupFolder", "backups"));
        maxBackupDirectorySize = configuration.getLong("maxBackupFolderSize");

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
                uploadClient = new SFTPClient(hostname, port, username, authentication,
                        knownHosts, timeout, remoteDirectory);
                break;

            default:
                uploadClient = new NullUploadClient();
        }
    }

    /**
     * Task to back up server
     * @param bukkitTask bukkitTask
     */
    @Override
    public void accept(BukkitTask bukkitTask) {
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
            bukkitTask.cancel();
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


        // Check which files should be included in the backup, but aren't
        List<String> missingFiles = backup.getMissingFiles();
        if (!missingFiles.isEmpty()) {
            backupLogger.warning("The following files should be excluded, but aren't: ");
            for (String fileName : missingFiles) {
                logger.warning(fileName);
            }
        }

        logger.info("Finished Backup.");
    }
}
