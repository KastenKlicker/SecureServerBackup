package de.kastenklicker.secureserverbackup;

import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.KeyPair;

import java.io.File;
import java.io.IOException;

/**
 * Class to generate test RSA KeyPairs
 */
public class KeyPairGenerator {
    
    private static final File privateKeyFile = new File("./src/test/resources/ssh_host_rsa_key");
    private static final File publicKeyFile = new File("./src/test/resources/ssh_host_rsa_key.pub");

    public File getPrivateKeyFile() {
        return privateKeyFile;
    }

    public File getPublicKeyFile() {
        return publicKeyFile;
    }

    /**
     * Generate RSA KeyPair
     */
    public void generate() throws IOException, JSchException {
        // Generate RSA keyPair
        JSch jSch = new JSch();
        KeyPair keyPair = KeyPair.genKeyPair(jSch, KeyPair.RSA, 4096);

        // Write Key files to file
        keyPair.writePrivateKey("src/test/resources/ssh_host_rsa_key");
        keyPair.writePublicKey("src/test/resources/ssh_host_rsa_key.pub", "sftp@docker");
        
        
    }
}
