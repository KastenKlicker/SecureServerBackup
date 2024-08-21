package de.kastenklicker.secureserverbackup;

import java.util.Collection;
import java.util.logging.Logger;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

/**
 * Class to send messages to console and online players
 */
public class BackupLogger {

    private final Logger logger;
    private final Collection<? extends Player> players;

    public BackupLogger() {

        Plugin plugin = SecureServerBackup.getPlugin(SecureServerBackup.class);
        this.logger = plugin.getLogger();
        this.players = plugin.getServer().getOnlinePlayers();
    }

    /**
     * Send an information message.
     * @param message Message
     */
    public void info(String message) {
        logger.info(message);   // Send to console

        // Send to online players
        for (Player player : players) {
            player.sendMessage(message);
        }
    }

    /**
     * Send a warning message.
     * @param message Message
     */
    public void warning(String message) {
        logger.warning(message);    // Send to console

        // Send to online players
        for (Player player : players) {
            player.sendMessage(Component.text(message,
                Style.style().color(NamedTextColor.YELLOW).build()));
        }
    }
}
