package profile.profileCheck;

import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class ProfileCheck {
    private final Connection con;
    private final Scanner sc;

    public ProfileCheck(Connection con, Scanner sc) {
        this.con = con;
        this.sc = sc;
    }

    public boolean checkProfile(int id) {
        try {
            // Check existing profile
            try (PreparedStatement pstCheck = con.prepareStatement("SELECT playing_role FROM PlayerProfile WHERE pid = ?")) {
                pstCheck.setInt(1, id);
                ResultSet rs = pstCheck.executeQuery();
                if (rs.next()) {
                    String role = rs.getString("playing_role");
                    if (role!=null) {
                        return false; // Already configured
                    }
                }
            }

            // Build profile
            System.out.println("\n--- Set Up Your Player Profile ---");
            String selectedRole = getChoice("Choose Playing Role:", new String[]{"Batsman", "Bowler", "All‑rounder", "Wicket‑keeper"});
            String selectedBatStyle = getChoice("Choose Batting Style:", new String[]{"Right‑hand bat", "Left‑hand bat"});
            String selectedBowlStyle = getChoice("Choose Bowling Style:", new String[]{
                    "Right‑arm Fast", "Right‑arm Medium", "Right‑arm Spin",
                    "Left‑arm Fast", "Left‑arm Medium", "Left‑arm Spin"
            });

            // Upsert profile
            String sql = """
                    INSERT INTO PlayerProfile (pid, playing_role, batting_style, bowling_style)
                    VALUES (?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        playing_role = VALUES(playing_role),
                        batting_style = VALUES(batting_style),
                        bowling_style = VALUES(bowling_style)
                    """;
            try (PreparedStatement pst = con.prepareStatement(sql)) {
                pst.setInt(1, id);
                pst.setString(2, selectedRole);
                pst.setString(3, selectedBatStyle);
                pst.setString(4, selectedBowlStyle);
                int result = pst.executeUpdate();
                if (result > 0) {
                    System.out.println("✅ Profile saved successfully!");
                    return true;
                }
                System.out.println("⚠ Profile save didn’t complete.");
            }
        } catch (SQLException e) {
            System.out.println("❌ DB Error saving profile: " + e.getMessage());
        }
        return false;
    }

    private String getChoice(String prompt, String[] options) {
        while (true) {
            System.out.println("\n" + prompt);
            for (int i = 0; i < options.length; i++) {
                System.out.printf("%d. %s%n", i + 1, options[i]);
            }
            System.out.print("Enter choice (1-" + options.length + "): ");
            try {
                int choice = sc.nextInt();
                sc.nextLine();
                if (choice >= 1 && choice <= options.length) {
                    return options[choice - 1];
                }
                System.out.println("⚠ Enter between 1 and " + options.length);
            } catch (InputMismatchException e) {
                System.out.println("⚠ Invalid input. Please enter a number.");
                sc.nextLine();
            }
        }
    }
}
