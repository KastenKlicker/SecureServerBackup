package de.kastenklicker.ssr;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Class to connect to encrypted Database
 */

public class H2Utils {

    private Connection conn;
    private Statement stmt;

    public H2Utils(String path, String user, String password) {
        try {

            Class.forName ("org.h2.Driver");

            this.conn = DriverManager.getConnection("jdbc:h2:file:" + path + "server;CIPHER=AES", user, password);
            this.stmt = this.conn.createStatement();
        } catch (Exception e) {
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

}
