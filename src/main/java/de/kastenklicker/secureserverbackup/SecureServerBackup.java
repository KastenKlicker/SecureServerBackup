package de.kastenklicker.secureserverbackup;

import java.io.File;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.logging.Logger;
import org.bukkit.plugin.java.JavaPlugin;

public class SecureServerBackup extends JavaPlugin {

    @Override
    public void onEnable() {

        //bstats Metrics
        //int pluginId = 11313;
        //new Metrics(this, pluginId);

        Logger logger = this.getLogger();

        // Config
        saveDefaultConfig();

        // Check for backup directory
        File mainDirectory = getDataFolder().getAbsoluteFile().getParentFile().getParentFile();
        File backupsDirectory = new File(mainDirectory,
                getConfig().getString("backupFolder", "backups"));
        if (!backupsDirectory.mkdirs() && !backupsDirectory.exists()) {
            logger.warning("Failed to access backup directory: "
                    + backupsDirectory.getAbsolutePath());
            logger.warning("Disabling server backup...");
            this.getServer().getPluginManager().disablePlugin(this);
            return;
        }

        // Register command
        this.getCommand("backup").setExecutor(new BackupCommand());

        // Set periode of backups
        // Convert time format to ticks
        String durationString = getConfig().getString("duration", "P1D");
        try {
            Duration duration = Duration.parse(durationString);
            long durationTicks = duration.toSeconds() * 20;

            new BackupRunnable().runTaskTimerAsynchronously(this, 0, durationTicks);
        } catch (DateTimeParseException e) {
            logger.warning("Couldn't parse duration. Disabling autobackup.");
        }
    }
}
