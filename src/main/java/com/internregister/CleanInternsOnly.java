package com.internregister;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Clean Intern Data Script
 * Removes only Intern-related data: Interns, Attendance, Leave Requests, and
 * Intern Users.
 * Keeps Admins, Supervisors, Departments, and Fields.
 */
public class CleanInternsOnly {

    private static final String DB_URL = "jdbc:mysql://localhost:3306/intern_register";
    private static final String DB_USER = "root";
    private static final String DB_PASSWORD = "Ledge.98";

    public static void main(String[] args) {
        // Clear terminal screen
        try {
            if (System.getProperty("os.name").contains("Windows")) {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
            } else {
                System.out.print("\033[H\033[2J");
                System.out.flush();
            }
        } catch (Exception e) {
            // Silently ignore if clear fails
        }

        System.out.println("Cleaning Intern Data...");

        try (Connection conn = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD)) {
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("SET FOREIGN_KEY_CHECKS = 0");

                int totalDeleted = 0;
                totalDeleted += stmt.executeUpdate("DELETE FROM leave_requests");
                totalDeleted += stmt.executeUpdate("DELETE FROM attendance");

                try {
                    totalDeleted += stmt.executeUpdate("DELETE FROM intern_contracts");
                } catch (SQLException e) {
                }
                totalDeleted += stmt.executeUpdate("DELETE FROM interns");
                totalDeleted += stmt.executeUpdate("DELETE FROM users WHERE role = 'INTERN'");
                try {
                    totalDeleted += stmt.executeUpdate("DELETE FROM verification_codes");
                } catch (SQLException e) {
                }

                // Reset IDs
                String[] tables = { "leave_requests", "attendance", "interns", "intern_contracts",
                        "verification_codes" };
                for (String table : tables) {
                    try {
                        stmt.execute("ALTER TABLE " + table + " AUTO_INCREMENT = 1");
                    } catch (SQLException e) {
                    }
                }

                stmt.execute("SET FOREIGN_KEY_CHECKS = 1");

                System.out.println("-----------------------------------------");
                if (totalDeleted > 0) {
                    System.out.println("SUCCESS: Removed " + totalDeleted + " intern-related records.");
                } else {
                    System.out.println("System is already clean (0 intern records found).");
                }
                System.out.println("Admins and Supervisors remain untouched.");
                System.out.println("-----------------------------------------");
            }
        } catch (SQLException e) {
            System.err.println("ERROR: Cleanup failed: " + e.getMessage());
            System.exit(1);
        }
    }
}
