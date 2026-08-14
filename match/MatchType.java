package match;

import database_Connection.DatabaseConnection;

import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class MatchType {
    int no_of_overs;
    int overs_per_bowler;
    String city;
    String ground;
    String ballType;
    String pitchType;
    Scanner sc = new Scanner(System.in);

    int matchTypeDetails() {
        // Overs details
        this.no_of_overs = -1;
        while (no_of_overs < 0) {
            try {
                System.out.print("Enter Number of Overs: ");
                while (true) {
                    try {
                        no_of_overs = sc.nextInt();
                        break;
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine(); // clear buffer
                    }
                }
                sc.nextLine();

                if (no_of_overs <= 0) {
                    System.out.println("Number of overs must be a positive number. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number for overs.");
                sc.nextLine(); // Clear the buffer
            }
        }

        // Overs per bowler

        int overs_per_bowler = -1;
        while (overs_per_bowler < 0) {
            try {
                System.out.print("Enter overs per Bowler (optional, press -1 to skip): ");
                while (true) {
                    try {
                        overs_per_bowler = sc.nextInt();
                        break;
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine(); // clear buffer
                    }
                }
                sc.nextLine();

                if (overs_per_bowler < -1) {
                    System.out.println("Invalid input. Please enter a positive number or -1 to skip.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number for overs per bowler.");
                sc.nextLine(); // Clear the buffer
            }
        }

        // City
        System.out.print("Enter city/town: ");
        String city = sc.nextLine();

        // Ground
        System.out.print("Enter Ground: ");
        String ground = sc.nextLine();

        // Ball Type
        int ballTypeChoice = -1;
        while (ballTypeChoice < 1 || ballTypeChoice > 3) {
            try {
                System.out.print("Enter Ball Type (1. Tennis  2. Leather  3. Other): ");
                while (true) {
                    try {
                        ballTypeChoice = sc.nextInt();
                        break;
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine(); // clear buffer
                    }
                }
                sc.nextLine();

                if (ballTypeChoice == 1) ballType = "Tennis";
                else if (ballTypeChoice == 2) ballType = "Leather";
                else if (ballTypeChoice == 3) ballType = "Other";
                else System.out.println("Invalid input. Please select a valid ball type.");
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number for ball type.");
                sc.nextLine(); // Clear the buffer
            }
        }

        // Pitch Type
        String[] pType = {"Rough", "Cement", "Turf", "Astroturf", "Matting"};
        int ptype = -1;
        while (ptype < 1 || ptype > pType.length) {
            try {
                for (int i = 0; i < pType.length; i++) {
                    System.out.println((i + 1) + ". " + pType[i]);
                }
                System.out.print("Enter pitch type: ");
                while (true) {
                    try {
                        ptype = sc.nextInt();
                        break;
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine(); // clear buffer
                    }
                }
                sc.nextLine();

                if (ptype < 1 || ptype > pType.length) {
                    System.out.println("Invalid pitch type selected. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number for pitch type.");
                sc.nextLine(); // Clear the buffer
            }
        }

        pitchType = pType[ptype - 1];

        String query = "INSERT INTO Matches (match_date, venue_city, venue_ground, overs, overs_per_bowler,ball_type, pitch_type) VALUES (CURRENT_TIMESTAMP, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement pstmt = DatabaseConnection.getConnection().prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, city);
            pstmt.setString(2, ground);
            pstmt.setInt(3, no_of_overs);
            pstmt.setInt(4, overs_per_bowler);
            pstmt.setString(5, ballType);
            pstmt.setString(6, pitchType);

            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating match failed, no rows affected.");
            }
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return generatedKeys.getInt(1);
                } else {
                    throw new SQLException("Creating match failed, no ID obtained.");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            return -1;
        }
    }
}






