package de.kastenklicker.ssr;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.sql.*;
import java.util.ArrayList;

/**
 * Class to connect to encrypted Database
 */

public class H2Utils {

    private Connection conn;
    private Statement stmt;
    private final CommandSender sender;
    private final String prefix;

    public H2Utils(String path, String user, String password, CommandSender sender, String prefix) {

        this.sender = sender;
        this.prefix = prefix;

        try {

            Class.forName ("org.h2.Driver");

            this.conn = DriverManager.getConnection("jdbc:h2:file:" + path + "server;CIPHER=AES", user, password);
            this.stmt = this.conn.createStatement();
        } catch (Exception e) {

            String message = e.getMessage();

            if (message.contains("28000-200") || message.contains("90049-200")) sender.sendMessage(prefix + ChatColor.RED + " Wrong username or password.");
            else e.printStackTrace();
        }

    }

    public void createNewTable() {
        String h2 = "CREATE TABLE IF NOT EXISTS Server(" +
                "HOST VARCHAR(255) PRIMARY KEY," +
                "PORT INT," +
                "USER VARCHAR(255)," +
                "PASSWORD VARCHAR(255)," +
                "PATH VARCHAR(255)," +
                "HOSTKEY VARCHAR(600) NULL" +
                ");";

        try {
            stmt.execute(h2);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void close() {
        try {
            this.stmt.close();
            this.conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public boolean addServer(String host, String portString, String user, String password, String path, String useSFTP, String hostkey) {

        createNewTable();

        int port;
        try {
            port = Integer.parseInt(portString);
        }
        catch (NumberFormatException e) {
            sender.sendMessage(prefix + ChatColor.RED + " Port must be a number.");

            return false;
        }

        if(useSFTP.equalsIgnoreCase("sftp")) {

            String h2 = "INSERT INTO Server(host, port, user, password, path, hostKey) VALUES(?,?,?,?,?,?)";
            try (PreparedStatement pStmt = conn.prepareStatement(h2)){
                pStmt.setString(1, host);
                pStmt.setInt(2, port);
                pStmt.setString(3, user);
                pStmt.setString(4, password);
                pStmt.setString(5, path);
                pStmt.setString(6, hostkey);

                pStmt.executeUpdate();

            } catch (SQLException e) {

                if (e.getMessage().contains("PRIMARY_KEY")) sender.sendMessage(prefix + ChatColor.RED + " Server with the address: " + host + " already saved in the Database.");
                else {
                    e.printStackTrace();
                }

                return false;

            }

        }
        else {
            String h2 = "INSERT INTO Server(host, port, user, password, path) VALUES(?,?,?,?,?)";

            try (PreparedStatement pStmt = conn.prepareStatement(h2)){
                pStmt.setString(1, host);
                pStmt.setInt(2, port);
                pStmt.setString(3, user);
                pStmt.setString(4, password);
                pStmt.setString(5, path);

                pStmt.executeUpdate();

            } catch (SQLException e) {

                if (e.getMessage().contains("PRIMARY_KEY")) sender.sendMessage(prefix + ChatColor.RED + " Server with the address: " + host + " already saved in the Database.");
                else {
                    e.printStackTrace();
                }

                return false;

            }
        }

        return true;
    }

    public void removeServer(String host) {

        String h2 = "DELETE FROM SERVER WHERE HOST=?";

        try (PreparedStatement pStmt = conn.prepareStatement(h2)) {
            pStmt.setString(1, host);
            pStmt.executeUpdate();

            sender.sendMessage(prefix + ChatColor.GREEN + " Server removed successfully.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<String[]> getSFTPServers() {

        ArrayList<String[]> servers = new ArrayList<>();

        String h2 = "SELECT * FROM SERVER WHERE HOSTKEY IS NOT NULL";

        try {

            if (stmt == null) return null;

            ResultSet rs = this.stmt.executeQuery(h2);

            while (rs.next()) {

                String[] server = {
                        rs.getString("HOST"),
                        Integer.toString(rs.getInt("PORT")),
                        rs.getString("USER"),
                        rs.getString("PASSWORD"),
                        rs.getString("PATH"),
                        rs.getString("HOSTKEY")
                };

                servers.add(server);

            }
            rs.close();
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return servers;
    }

    public ArrayList<String[]> getFTPSServers() {

        ArrayList<String[]> servers = new ArrayList<>();

        String h2 = "SELECT * FROM SERVER WHERE HOSTKEY IS NULL";

        try {
            if (stmt == null) return null;

            ResultSet rs = this.stmt.executeQuery(h2);

            while (rs.next()) {

                String[] server = {
                        rs.getString("HOST"),
                        Integer.toString(rs.getInt("PORT")),
                        rs.getString("USER"),
                        rs.getString("PASSWORD"),
                        rs.getString("PATH")
                };

                servers.add(server);

            }
            rs.close();
        }
        catch (Exception e) {

            e.printStackTrace();
        }
        return servers;

    }

}
