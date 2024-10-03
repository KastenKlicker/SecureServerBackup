package de.kastenklicker.secureserverbackup;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPConnectionClosedException;

import java.io.File;
import java.io.FileInputStream;

public class FTPSClient extends UploadClient {

    public FTPSClient(String hostname, int port, String username, String authentication, String knownHosts, int timeout, String remoteDirectory) {
        super(hostname, port, username, authentication, knownHosts, timeout, remoteDirectory);
    }

    /**
     * Method for uploading with FTPS
     * @param file Backup File
     * @throws Exception FTPS & File related exceptions
     */
    @Override
    public void upload(File file) throws Exception {
        org.apache.commons.net.ftp.FTPSClient ftpsClient = new org.apache.commons.net.ftp.FTPSClient();
        ftpsClient.connect(hostname, port);
        
        // Set encryption parameters
        ftpsClient.execPBSZ(0);
        ftpsClient.execPROT("P");
        
        // Set connection parameters
        ftpsClient.login(username, authentication);
        ftpsClient.setFileType(FTP.LOCAL_FILE_TYPE);
        ftpsClient.enterLocalPassiveMode();
        
        // Upload file
        FileInputStream fileInputStream = new FileInputStream(file);
        String remoteFile = new File(remoteDirectory, file.getName()).getAbsolutePath();
        ftpsClient.storeFile(remoteFile, fileInputStream);
        fileInputStream.close();
        
        // Finish upload
        try {
            ftpsClient.logout();
        } catch (FTPConnectionClosedException ignore) {} 
            
        ftpsClient.disconnect();
    }
}
