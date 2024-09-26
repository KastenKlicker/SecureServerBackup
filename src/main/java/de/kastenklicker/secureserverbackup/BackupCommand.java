package de.kastenklicker.secureserverbackup;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

/**
 * Class for the /backup command
 */
public class BackupCommand implements CommandExecutor {

    /**
     * This method is run when the /backup command is executed
     * @param commandSender Entity which executed the command
     * @param command The Command
     * @param s Some random String, just look up the jd
     * @param strings Some random arguments, i don't need
     * @return if the command is executed successfully, always returns true
     */
    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command,
            @NotNull String s, @NotNull String[] strings) {
        
        // Check if commandSender is player
        if (!commandSender.isOp()) {
            commandSender.sendMessage("You don't have the permission to backup the server!");
            return false;
        }    

        // Get plugin information
        Plugin plugin = SecureServerBackup.getPlugin(SecureServerBackup.class);

        new BackupRunnable().runTaskAsynchronously(plugin);

        return true;
    }
}
