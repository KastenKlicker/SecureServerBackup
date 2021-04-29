package de.kastenklicker.ssr;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;


public class Commands {

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args, String prefix, JavaPlugin plugin, String separator) {

        if (args[0].equalsIgnoreCase("setLogin")) {

            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + " You have to be a player.");
                return true;
            }

            if (!sender.hasPermission("serverrestorer.login")) {
                sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
                return true;
            }

            if(new File(plugin.getDataFolder().getPath() + separator + "server.mv.db").exists()) {
                sender.sendMessage(prefix + ChatColor.RED + "Login already set. To change your login information delete the server.mv.db file. All saved hosts will be deleted.");
                return true;
            }

            if (args.length != 4) {
                sender.sendMessage(prefix + "/ssr setLogin <user> <password1> <password2>");
                return true;
            }

            String dataFolder = plugin.getDataFolder().getAbsolutePath();
            H2Utils h2 = new H2Utils(dataFolder + separator, args[1], args[2] + " " + args[3]);
            return true;
        }

        if (args[0].equalsIgnoreCase("login")) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(prefix + ChatColor.RED + " You have to be a player.");
                return true;
            }

            if (!sender.hasPermission("serverrestorer.login")) {
                sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
                return true;
            }
            if(!new File(plugin.getDataFolder().getPath() + separator + "server.mv.db").exists()) {
                sender.sendMessage(prefix + ChatColor.RED + "No login set. Run /setLogin <user> <password1> <password2> to set login information.");
                return true;
            }
            if (args.length != 4) {
                sender.sendMessage(prefix + "/ssr login <user> <password1> <password2>");
                return true;
            }
        }

        return false;
    }

}
