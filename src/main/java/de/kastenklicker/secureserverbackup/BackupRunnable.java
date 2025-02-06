package de.kastenklicker.secureserverbackup;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import de.kastenklicker.secureserverbackuplibrary.Backup;
import de.kastenklicker.secureserverbackuplibrary.upload.FTPSClient;
import de.kastenklicker.secureserverbackuplibrary.upload.SFTPClient;
import de.kastenklicker.secureserverbackuplibrary.upload.UploadClient;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitWorker;
import org.jetbrains.annotations.NotNull;

public class BackupRunnable extends BukkitRunnable {

    private final List<String> excludeFiles;
    private final List<String> includedFiles;
    private final File backupDirectory;
    private final File mainDirectory;
    private final List<UploadClient> uploadClients = new ArrayList<>();
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
        
        // Exclude session locks, because those are locked by paper
        excludeFiles.add("world/session.lock");
        excludeFiles.add("world_nether/session.lock");
        excludeFiles.add("world_the_end/session.lock");
        
        includedFiles = configuration.getStringList("includedFiles");
        if (includedFiles.isEmpty()) {
            includedFiles.addAll(List.of(Objects.requireNonNull(mainDirectory.list())));
        }
        backupDirectory = new File(mainDirectory, configuration.getString("backupFolder", "backups"));
        maxBackupDirectorySize = configuration.getLong("maxBackupFolderSize")
                * (1000*1000*1000); // KB*MB*GB;

        @NotNull List<Map<?, ?>> uploadServers = configuration.getMapList("uploadServers");

        for (Map<?, ?> uploadServer : uploadServers) {

            // Get upload information
            String protocol = (String) uploadServer.get("uploadProtocol");
            String hostname = (String) uploadServer.get("hostname");
            int port = (int) uploadServer.get("port");
            String username = (String) uploadServer.get("username");
            String authentication = (String) uploadServer.get("authentication");
            String knownHosts = (String) uploadServer.get("knownHosts");
            int timeout = (int) uploadServer.get("timeout")*1000;
            String remoteDirectory = (String) uploadServer.get("remoteDirectory");
            
            switch (protocol) {
                case "sftp":
                    if (knownHosts == null)
                        throw new NullPointerException("Read null for knownHosts! Check your config.yml.");
                    uploadClients.add(new SFTPClient(hostname, port, username, authentication,
                            new File(knownHosts), timeout, remoteDirectory));
                    break;

                case "ftps":
                    uploadClients.add(new FTPSClient(hostname, port, username, authentication, remoteDirectory));
                    break;
            }
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
        Backup backup = new Backup(includedFiles,excludeFiles, backupDirectory,
                mainDirectory, uploadClients, maxBackupDirectorySize);
        try {
            backup.backup();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        backupLogger.info("Finished Backup.");
    }
}
