package com.internregister;

import java.sql.*;

public class ListDatabases {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ledge.98";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to MySQL");
            try (ResultSet rs = conn.getMetaData().getCatalogs()) {
                while (rs.next()) {
                    System.out.println("Database: " + rs.getString(1));
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
