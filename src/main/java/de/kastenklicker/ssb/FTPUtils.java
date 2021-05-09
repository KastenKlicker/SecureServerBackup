package de.kastenklicker.ssb;

import com.jcraft.jsch.*;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Properties;

public class FTPUtils {

    private final String prefix;
    private final CommandSender sender;

    public FTPUtils(String prefix, CommandSender sender) {
        this.prefix = prefix;
        this.sender = sender;
    }

    /**
     *
     * Uploading backup to a sftp server
     */

    public boolean uploadSFTP(String[] server, File zipFile) {

        boolean exception = false;

        JSch jSch = new JSch();

        try {

            //HostKey verification
            byte [] key = Base64.getDecoder().decode(server[5]);
            HostKey hostKey1 = new HostKey(server[0], key);
            jSch.getHostKeyRepository().add(hostKey1, null);

            //Connect
            Session session = jSch.getSession(server[2], server[0], Integer.parseInt(server[1]));
            session.setPassword(server[3]);
            session.connect(1000 * 20);
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(1000 * 20);

            //Upload
            channel.put(zipFile.getPath(), server[4] + zipFile.getName());

            //Disconnect
            channel.exit();
            session.disconnect();
        }

        catch (Exception e) {
            e.printStackTrace();
            exception = true;
        }

        return exception;

    }

    public String getHostKey(String host, int port,  String user, String password) {

        String hostkey = null;

        JSch jsch = new JSch();
        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");

        try {
            Session session = jsch.getSession(user, host, port);
            session.setPassword(password);
            session.setConfig(config);
            session.connect(1000 * 20);
            hostkey = session.getHostKey().getKey();
            session.disconnect();
        } catch (JSchException e) {
            if (e.getMessage().contains("Auth fail")) sender.sendMessage(prefix + ChatColor.RED + " Wrong username or password.");
            else if (e.getMessage().contains("UnknownHostException")) sender.sendMessage(prefix + ChatColor.RED + " Couldn't reach host.");
            else e.printStackTrace();
        }


        return hostkey;
    }


    /**
     * Uploading backup to a ftps server
     */
    public boolean uploadFTPS(String[] server, File zipFile) {

        boolean exception = false;

        FTPSClient ftpClient = new FTPSClient();

        try {

            FileInputStream zipFileStream = new FileInputStream(zipFile);

            if (ftpClient.isConnected()) {
                sender.sendMessage(prefix + " FTPSClient was already connected. Disconnecting");
                ftpClient.logout();
                ftpClient.disconnect();
                ftpClient = new FTPSClient();
            }

            //Connect
            ftpClient.connect(server[0], Integer.parseInt(server[1]));
            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");
            ftpClient.login(server[2], server[3]);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            //Upload
            boolean done = ftpClient.storeFile(server[4] + zipFile.getName(), zipFileStream);
            zipFileStream.close();
            if (!done) sender.sendMessage(prefix + ChatColor.YELLOW + " Something failed (maybe)! Status=" + ftpClient.getStatus());

        }
        catch (Exception e) {
            e.printStackTrace();
            exception = true;
        }
        finally {

            try {
                if (ftpClient.isConnected()) {
                    ftpClient.logout();
                    ftpClient.disconnect();
                }

            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        return exception;

    }

}

