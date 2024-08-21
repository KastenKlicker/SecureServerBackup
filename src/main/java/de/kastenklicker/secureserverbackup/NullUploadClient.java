package de.kastenklicker.secureserverbackup;

import java.io.File;

public class NullUploadClient extends UploadClient{

    public NullUploadClient() {
        super(null, 0, null, null, null, 0, null);
    }

    @Override
    public void upload(File file) {
    }
}
