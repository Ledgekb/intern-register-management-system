package com.internregister;

import java.sql.*;

public class DumpUsers {
    public static void main(String[] args) throws Exception {
        Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/intern_register", "root",
                "Ledge.98");
        ResultSet rs = conn.createStatement().executeQuery("SELECT username, email, role FROM users");
        System.out.println("Username | Email | Role");
        System.out.println("-------------------------");
        while (rs.next()) {
            System.out.println(rs.getString(1) + " | " + rs.getString(2) + " | " + rs.getString(3));
        }
    }
}
