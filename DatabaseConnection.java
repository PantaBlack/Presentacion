package InventarioT3Package;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DatabaseConnection {
	 private static final String URL = "jdbc:sqlserver://LAPTOP-LDU01M3K:1433;databaseName=InventarioDB;encrypt=true;trustServerCertificate=true;integratedSecurity=true";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            return DriverManager.getConnection(URL);
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver JDBC no encontrado: " + e.getMessage());
        }
    }
}

//final