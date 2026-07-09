package com.internregister;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class ViewFaceData {
    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/intern_register";
        String user = "root";
        String password = "Ledge.98";

        try {
            Connection conn = DriverManager.getConnection(url, user, password);
            Statement stmt = conn.createStatement();
            String sql = "SELECT email, LEFT(face_data, 150) as descriptor FROM interns WHERE face_data IS NOT NULL";
            ResultSet rs = stmt.executeQuery(sql);

            System.out.println("--- Registered Faces in Database ---");
            boolean found = false;
            while (rs.next()) {
                found = true;
                System.out.println("Email: " + rs.getString("email"));
                System.out.println("Descriptor (Partial): " + rs.getString("descriptor") + "...");
                System.out.println("------------------------------------");
            }
            
            if (!found) {
                System.out.println("No registered faces found in the database.");
            }
            
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
