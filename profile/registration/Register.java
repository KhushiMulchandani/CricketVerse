package profile.registration;

import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class Register {
    private String name;
    private String gender;
    public int userid;

    public void reg(Connection con, Scanner sc) {
        try {
            String typeOfUser = "";

            // User Type Selection
            while (true) {
                System.out.println("\nRegister as:");
                System.out.println("1. Organizer");
                System.out.println("2. Player");
                System.out.print("Enter choice: ");
                try {
                    int choice = sc.nextInt();
                    sc.nextLine();
                    if (choice == 1) { typeOfUser = "Organizer"; break; }
                    if (choice == 2) { typeOfUser = "Player"; break; }
                    System.out.println("⚠ Please select 1 or 2 only.");
                } catch (InputMismatchException e) {
                    System.out.println("⚠ Invalid input. Enter numeric choice.");
                    sc.nextLine();
                }
            }

            // Mobile validation
            String mobNo;
            while (true) {
                System.out.print("Enter 10-digit mobile number (starts 6-9): ");
                mobNo = sc.next();
                sc.nextLine();
                if (!mobNo.matches("^[6-9]\\d{9}$")) {
                    System.out.println("⚠ Invalid format.");
                    continue;
                }
                try (PreparedStatement checkPst = con.prepareStatement("SELECT id FROM user WHERE contact = ?")) {
                    checkPst.setString(1, mobNo);
                    ResultSet rs = checkPst.executeQuery();
                    if (rs.next()) {
                        System.out.println("⚠ Number already registered.");
                    } else {
                        break;
                    }
                }
            }

            // Name & Location
            System.out.print("Enter your name: ");
            name = sc.nextLine();
            System.out.print("Enter your city: ");
            String city = sc.nextLine();

            // Gender validation
            while (true) {
                System.out.print("Gender (M/F/O): ");
                gender = sc.next().trim().toUpperCase();
                sc.nextLine();
                if ("M".equals(gender) || "F".equals(gender) || "O".equals(gender)) break;
                System.out.println("⚠ Please enter 'M' or 'F'.");
            }

            // DOB validation
            String dob;
            while (true) {
                System.out.print("Date of Birth (YYYY-MM-DD): ");
                dob = sc.nextLine().trim();
                if (dob.matches("\\d{4}-\\d{2}-\\d{2}")) break;
                System.out.println("⚠ Invalid date format.");
            }

            // Call Stored Procedure
            try (CallableStatement cstmt = con.prepareCall("{CALL RegisterUser(?, ?, ?, ?, ?, ?, ?)}")) {
                cstmt.setString(1, name);
                cstmt.setString(2, mobNo);
                cstmt.setString(3, city);
                cstmt.setString(4, gender);
                cstmt.setString(5, dob);
                cstmt.registerOutParameter(6, Types.INTEGER);
                cstmt.setString(7, typeOfUser);
                cstmt.execute();

                userid = cstmt.getInt(6);
                if (userid > 0) {
                    System.out.println("✅ Registered successfully! Your User ID is: " + userid);
                } else {
                    System.out.println("⚠ Registration failed. Please try again.");
                    return;
                }
            } catch (SQLException e) {
                System.out.println("❌ Error during registration: " + e.getMessage());
                return;
            }

            // If Player, initialize profile
            if ("Player".equalsIgnoreCase(typeOfUser)) {
                try (PreparedStatement pst = con.prepareStatement("INSERT INTO PlayerProfile (pid) VALUES (?)")) {
                    pst.setInt(1, userid);
                    pst.executeUpdate();
                    System.out.println("Player profile created.");
                } catch (SQLException e) {
                    System.out.println("⚠ Could not init player profile: " + e.getMessage());
                }
            }

        } catch (Exception e) {
            System.out.println("Unexpected error during registration: " + e.getMessage());
        }
    }
}
