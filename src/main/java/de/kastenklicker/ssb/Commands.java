package de.kastenklicker.ssb;

import me.zombie_striker.sr.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;


public class Commands {

    public boolean onCommand(CommandSender sender, String[] args, String prefix, JavaPlugin plugin, String separator, Main Main, String user, String password1, String password2) {

        String dataFolder = plugin.getDataFolder().getAbsolutePath();

        if (!sender.hasPermission("secureserverbackup.login")) {
            sender.sendMessage(prefix + ChatColor.RED + " You do not have permission to use this command.");
            return true;
        }

        if (args[0].equalsIgnoreCase("setLogin")) {

            if(new File(plugin.getDataFolder().getPath() + separator + "server.mv.db").exists()) {
                sender.sendMessage(prefix + ChatColor.RED + "Login already set. To change your login information delete the server.mv.db file. All saved hosts will be deleted.");
                return true;
            }

            if (args.length != 4) {
                sender.sendMessage(prefix + "/ssb setLogin <user> <password1> <password2>");
                return true;
            }

            if ( args[2].trim().length() < 6 || args[3].trim().length() < 6) {
                sender.sendMessage(prefix + ChatColor.RED + " Password must be at least 6 chars long.");
                return true;
            }

            H2Utils h2 = new H2Utils(dataFolder + separator, args[1], args[2] + " " + args[3], sender, prefix);
            h2.createNewTable();

            h2.close();

            Main.setUser(args[1]);
            Main.setPassword1(args[2]);
            Main.setPassword2(args[3]);

            return true;
        }

        if(!new File(plugin.getDataFolder().getPath() + separator + "server.mv.db").exists()) {
            sender.sendMessage(prefix + ChatColor.RED + "No Database found. Run /ssb setLogin <user> <password1> <password2> to create Database.");
            return true;
        }

        if (args[0].equalsIgnoreCase("login")) {

            if (args.length != 4) {
                sender.sendMessage(prefix + "/ssb login <user> <password1> <password2>");
                return true;
            }

            Main.setUser(args[1]);
            Main.setPassword1(args[2]);
            Main.setPassword2(args[3]);

            H2Utils h2 = new H2Utils(dataFolder + separator, args[1], args[2] + " " + args[3], sender, prefix);

            Main.setSftpList(h2.getSFTPServers());
            Main.setFtpsList(h2.getFTPSServers());

            h2.close();

            return true;
        }

        if (args[0].equalsIgnoreCase("addServer")) {

            if (user == null) {
                sender.sendMessage(prefix + ChatColor.RED + " You aren't logged in.");
                return true;
            }

            if (args.length == 6 || args.length == 7) {

                boolean sftp = args.length == 7;

                H2Utils h2 = new H2Utils(dataFolder + separator, user, password1 + " " + password2, sender, prefix);
                if (h2.addServer(args[1], args[2], args[3], args[4], args[5], sftp)) sender.sendMessage(prefix + ChatColor.GREEN + " Added server successfully.");

                Main.setSftpList(h2.getSFTPServers());
                Main.setFtpsList(h2.getFTPSServers());

                h2.close();

                return true;

            }

            sender.sendMessage(prefix + " /ssb addServer <host> <port> <user> <password> <path> <sftp>");
            return true;

        }

        if (args[0].equalsIgnoreCase("removeServer")) {

            if (user == null) {
                sender.sendMessage(prefix + ChatColor.RED + " You aren't logged in.");
                return true;
            }
            if (args.length != 2) {
                sender.sendMessage(prefix + " /ssb removeServer <host>");
                return true;
            }

            H2Utils h2 = new H2Utils(dataFolder + separator, user, password1 + " " + password2, sender, prefix);
            h2.removeServer(args[1]);

            Main.setSftpList(h2.getSFTPServers());
            Main.setFtpsList(h2.getFTPSServers());

            h2.close();

            return true;

        }

        return false;
    }

}
