package de.kastenklicker.ssr;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Base64;

public class FTPUtils {

    private final String serverFTP, userFTP, passwordFTP, removeFilePath, prefix;
    private final int portFTP;
    private final boolean deleteZipOnFail, deleteZipOnFTP;
    private final File zipFile;
    private final CommandSender sender;

    public FTPUtils(String serverFTP, int portFTP, String userFTP, String passwordFTP, String removeFilePath, String prefix, boolean deleteZipOnFail, boolean deleteZipOnFTP, File zipFile, CommandSender sender) {
        this.serverFTP = serverFTP;
        this.userFTP = userFTP;
        this.passwordFTP = passwordFTP;
        this.removeFilePath = removeFilePath;
        this.prefix = prefix;
        this.portFTP = portFTP;
        this.deleteZipOnFail = deleteZipOnFail;
        this.deleteZipOnFTP = deleteZipOnFTP;
        this.zipFile = zipFile;
        this.sender = sender;
    }

    /**
     *
     * @param hostKey given in config.yml
     * @param strictHostKeyChecking given in config.yml
     *
     * Uploading backup to a sftp server
     */

    public void uploadSFTP(String hostKey, boolean strictHostKeyChecking) {
        try {
            JSch jsch = new JSch();

            //Check for given hostkey
            if(!hostKey.trim().equals("")) {
                byte [] key = Base64.getDecoder().decode(hostKey);
                HostKey hostKey1 = new HostKey(serverFTP, key);
                jsch.getHostKeyRepository().add(hostKey1, null);
            }

            //Connect
            Session session = jsch.getSession(userFTP, serverFTP, portFTP);

            if (!strictHostKeyChecking) session.setConfig("StrictHostKeyChecking", "no");

            session.setConfig("PreferredAuthentications", "password");
            session.setPassword(passwordFTP);
            session.connect(1000 * 20);
            ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
            channel.connect(1000 * 20);

            //Upload
            channel.put(zipFile.getPath(), removeFilePath + zipFile.getName());

            //Disconnect
            channel.exit();
            session.disconnect();

            if (deleteZipOnFTP)
                zipFile.delete();

            sender.sendMessage(prefix + " Successfully uploaded backup");

        }

        catch (Exception e) {
            if (e.getMessage().contains("UnknownHostKey")) {
                System.out.println(e.getMessage());
                sender.sendMessage(prefix + " RSA Hostkey couldn't be found. The following advices may help you:");
                sender.sendMessage(prefix + " Set the RSA Hostkey in the config.yml");
                sender.sendMessage(prefix + " Or set StrictHostKeyChecking to false in the config.yml file, this is not recommended due to security flaws.");

            }

            else {
                sender.sendMessage(
                        prefix + " FAILED TO SFTP TRANSFER FILE: " + zipFile.getName() + ". ERROR IN CONSOLE.");
                e.printStackTrace();
            }
            if (deleteZipOnFail)
                zipFile.delete();
        }
    }


    /**
     * Uploading backup to a ftps server
     */
    public void uploadFTPS() {
        sender.sendMessage(prefix + " Starting FTPS Transfer");

        FTPSClient ftpClient = new FTPSClient();

        try {

            FileInputStream zipFileStream = new FileInputStream(zipFile);

            if (ftpClient.isConnected()) {
                sender.sendMessage(prefix + "FTPSClient was already connected. Disconnecting");
                ftpClient.logout();
                ftpClient.disconnect();
                ftpClient = new FTPSClient();
            }

            //Connect
            ftpClient.connect(serverFTP, portFTP);
            ftpClient.execPBSZ(0);
            ftpClient.execPROT("P");
            ftpClient.login(userFTP, passwordFTP);
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            //Upload
            boolean done = ftpClient.storeFile(removeFilePath + zipFile.getName(), zipFileStream);
            zipFileStream.close();
            if (done) {
                sender.sendMessage(prefix + " Transfered backup using FTPS!");
            } else {
                sender.sendMessage(prefix + " Something failed (maybe)! Status=" + ftpClient.getStatus());
            }

            if (deleteZipOnFTP)
                zipFile.delete();
        } catch (Exception | Error e) {
            sender.sendMessage(
                    prefix + " FAILED TO FTPS TRANSFER FILE: " + zipFile.getName() + ". ERROR IN CONSOLE.");
            if (deleteZipOnFail)
                zipFile.delete();
            e.printStackTrace();
        } finally {
            try {
                if (ftpClient.isConnected()) {
                    sender.sendMessage(prefix + " Disconnecting");
                    ftpClient.logout();
                    ftpClient.disconnect();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }
}

