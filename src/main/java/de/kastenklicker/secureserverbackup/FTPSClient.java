package de.kastenklicker.secureserverbackup;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.util.TrustManagerUtils;
import org.bouncycastle.jsse.BCExtendedSSLSession;
import org.bouncycastle.jsse.BCSSLSocket;
import org.bouncycastle.jsse.provider.BouncyCastleJsseProvider;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.*;

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
        ReuseableFTPSClient ftpsClient = new ReuseableFTPSClient();
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

    /**
     * Extending apaches FTPSClient to reuse the ssl data connection
     * To get to know more about this look here:
     * <a href="https://stackoverflow.com/a/77587106/13260382">...</a>
     */
    private static class ReuseableFTPSClient extends org.apache.commons.net.ftp.FTPSClient {
        static {
            Security.addProvider(new BouncyCastleJsseProvider());
        }
        
        private ReuseableFTPSClient() {
            super(createSslContext());
        }

        private static SSLContext createSslContext() {
            try {
                // doesn't work with TLSv1.3
                SSLContext sslContext = SSLContext.getInstance("TLSv1.2", "BCJSSE");
                sslContext.init(null, new TrustManager[]{TrustManagerUtils.getValidateServerCertificateTrustManager()}, new SecureRandom()); // 1
                return sslContext;
            } catch (NoSuchAlgorithmException | NoSuchProviderException | KeyManagementException e) {
                throw new RuntimeException("Cannot create SSL context.", e);
            }
        }

        /**
         * Reuse the data channel
         * @param socket Socket
         */
        @Override
        protected void _prepareDataSocket_(Socket socket) {
            if (_socket_ instanceof BCSSLSocket sslSocket) {
                BCExtendedSSLSession bcSession = sslSocket.getBCSession();
                if (bcSession != null && bcSession.isValid() && socket instanceof BCSSLSocket dataSslSocket) {
                    dataSslSocket.setBCSessionToResume(bcSession); // 2
                    dataSslSocket.setHost(bcSession.getPeerHost()); // 3
                }
            }
        }
    }
}
