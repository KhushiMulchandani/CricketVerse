package profile.loginCheck;

import java.sql.*;
import java.util.InputMismatchException;
import java.util.Scanner;

public class LoginCheck {
    private final Connection con;
    private final Scanner sc;

    public LoginCheck(Connection con, Scanner sc) {
        this.con = con;
        this.sc = sc;
    }

    public int check() {
        while (true) {
            System.out.print("Enter User ID to login : ");
            try {
                int id = sc.nextInt();
                sc.nextLine();
                if (id == 0) return -1;

                String query = "SELECT name FROM user WHERE id = ?";
                try (PreparedStatement pst = con.prepareStatement(query)) {
                    pst.setInt(1, id);
                    ResultSet rs = pst.executeQuery();
                    if (rs.next()) {
                        System.out.println("✅ Welcome back, " + rs.getString("name") + "!");
                        return id;
                    }
                    System.out.println("⚠ Invalid User ID. Try again.");
                }
            } catch (InputMismatchException e) {
                System.out.println("⚠ Invalid input. Enter a numeric User ID.");
                sc.nextLine();
            } catch (SQLException e) {
                System.out.println("❌ DB error during login: " + e.getMessage());
                return -1;
            }
        }
    }
}
