package de.kastenklicker.ssr;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPSClient;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Base64;

public class FTPUtils {

    private final String prefix;
    private final boolean deleteZipOnFail, deleteZipOnFTP;
    private final File zipFile;
    private final CommandSender sender;

    public FTPUtils(String prefix, boolean deleteZipOnFail, boolean deleteZipOnFTP, File zipFile, CommandSender sender) {
        this.prefix = prefix;
        this.deleteZipOnFail = deleteZipOnFail;
        this.deleteZipOnFTP = deleteZipOnFTP;
        this.zipFile = zipFile;
        this.sender = sender;
    }

    /**
     *
     * Uploading backup to a sftp server
     */

    public void uploadSFTP(ArrayList<String[]> servers) {

        sender.sendMessage(prefix + " Starting SFTP Transfer");

        for (String[] server : servers) {

            JSch jSch = new JSch();

            try {

                //HostKey verification
                byte [] key = Base64.getDecoder().decode(server[5]);
                HostKey hostKey1 = new HostKey(server[0], key);
                jSch.getHostKeyRepository().add(hostKey1, null);

                //Connect
                Session session = jSch.getSession(server[2], server[0], Integer.parseInt(server[1]));
                session.setPassword(server[3]);
                session.connect();
                ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
                channel.connect();

                //Upload
                channel.put(zipFile.getPath(), server[4] + zipFile.getName());

                //Disconnect
                channel.exit();
                session.disconnect();

                sender.sendMessage(prefix + ChatColor.GREEN + " Transferred backup using SFTP");

                if (deleteZipOnFTP)
                    zipFile.delete();
            }

            catch (Exception e) {

                    sender.sendMessage(
                            prefix + ChatColor.RED + " FAILED TO SFTP TRANSFER FILE: " + zipFile.getName() + ". ERROR IN CONSOLE.");
                    e.printStackTrace();
                if (deleteZipOnFail)
                    zipFile.delete();
            }
        }

    }


    /**
     * Uploading backup to a ftps server
     */
        public void uploadFTPS(String[] server, int listSize, int i) {

            sender.sendMessage(prefix + " Starting FTPS Transfer(" + i + "/" + listSize + ").");

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
                if (done) {
                    sender.sendMessage(prefix + " Transfered backup using FTPS!");
                } else {
                    sender.sendMessage(prefix + ChatColor.YELLOW + " Something failed (maybe)! Status=" + ftpClient.getStatus());
                }

                if (deleteZipOnFTP)
                    zipFile.delete();

            }
            catch (UnknownHostException e) {
                sender.sendMessage(prefix + ChatColor.RED + " Couldn't upload backup to " + server[0] + ". Server couldn't be reached.");
                if (deleteZipOnFail && i > listSize)
                    zipFile.delete();
            }
            catch (Exception e) {
                sender.sendMessage(prefix + ChatColor.RED + " FAILED TO FTPS TRANSFER FILE: " + zipFile.getName() + ". ERROR IN CONSOLE.");
                if (deleteZipOnFail && i > listSize)
                    zipFile.delete();
                e.printStackTrace();
            } finally {

                try {
                    if (ftpClient.isConnected()) {
                        sender.sendMessage(prefix + " Disconnecting");
                        ftpClient.logout();
                        ftpClient.disconnect();
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                }

            }

     }

}

