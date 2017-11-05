package org.arquillian.cube.containerobject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

public class FtpClient {

    FTPClient ftpClient;

    public FtpClient(String host, int port, String user, String password) throws Exception {

        ftpClient = new FTPClient();
        int reply;
        ftpClient.connect(host, port);
        reply = ftpClient.getReplyCode();

        if (!FTPReply.isPositiveCompletion(reply)) {
            ftpClient.disconnect();
            throw new Exception("Exception in connecting to FTP Server");
        }
        ftpClient.login(user, password);
        ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        if ("localhost".equals(host)) {
            ftpClient.enterLocalPassiveMode();
        } else {
            ftpClient.enterLocalActiveMode();
        }
    }

    public void uploadFile(File fileToUplaod, String filename) throws IOException {
        try (InputStream input = new FileInputStream(fileToUplaod)) {
            this.ftpClient.storeFile(filename, input);
        }
    }

    public void disconnect() {
        if (this.ftpClient.isConnected()) {
            try {
                this.ftpClient.logout();
                this.ftpClient.disconnect();
            } catch (IOException f) {
                // do nothing as file is already saved to server
            }
        }
    }
}
