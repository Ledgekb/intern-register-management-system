package com.internregister;

import java.sql.*;

public class CheckDatabase {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/intern_register";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ledge.98";

    public static void main(String[] args) {
        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            System.out.println("Connected to " + DB_URL);

            checkTable(conn, "interns");
            checkTable(conn, "users");
            checkTable(conn, "attendance");
            checkTable(conn, "leave_requests");
            checkTable(conn, "intern_contracts");

            System.out.println("\nRole counts in users table:");
            try (Statement stmt = conn.createStatement();
                    ResultSet rs = stmt.executeQuery("SELECT role, COUNT(*) as count FROM users GROUP BY role")) {
                while (rs.next()) {
                    System.out.println("  " + rs.getString("role") + ": " + rs.getInt("count"));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private static void checkTable(Connection conn, String table) {
        try (Statement stmt = conn.createStatement();
                ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM " + table)) {
            if (rs.next()) {
                System.out.println("Table " + table + ": " + rs.getInt(1) + " records");
            }
        } catch (SQLException e) {
            System.out.println("Table " + table + " does not exist or error: " + e.getMessage());
        }
    }
}
