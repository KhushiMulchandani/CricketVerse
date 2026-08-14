import profile.loginCheck.LoginCheck;
import profile.profileCheck.ProfileCheck;
import profile.registration.Register;
import stats.Player;
import organizer.Organizer;
import database_Connection.DatabaseConnection;

import java.sql.*;
import java.util.*;

public class CricketVerse {
    public static void main(String[] args) {
        try (Scanner sc = new Scanner(System.in);
             Connection con = DatabaseConnection.getConnection()) {
            if(con!=null) {
                System.out.println("Connected Successfully");
            }
            else{
                System.out.println("Please connect to Database");
                return;
            }
            Statement stmt = con.createStatement();
            createTables(stmt);

            int ch = 0;
            do {
                System.out.println("\n<<< MAIN MENU >>>");
                System.out.println("1. Register");
                System.out.println("2. Login");
                System.out.println("3. Exit");
                System.out.print("Enter your choice: ");

                try {
                    ch = sc.nextInt();
                    sc.nextLine(); // consume newline

                    switch (ch) {
                        case 1 -> {
                            Register r = new Register();
                            r.reg(con, sc);
                        }
                        case 2 -> handleLogin(con, sc);
                        case 3 -> System.out.println("Goodbye!");
                        default -> System.out.println("⚠ Invalid choice, please enter 1, 2 or 3.");
                    }
                } catch (InputMismatchException e) {
                    System.out.println("⚠ Invalid input. Please enter a number (1‑3).");
                    sc.nextLine(); // clear invalid input
                }

            } while (ch != 3);

        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
        }
    }

    private static void handleLogin(Connection con, Scanner sc) {
        LoginCheck lc = new LoginCheck(con,sc);
        while (true) {
            int userId = lc.check();
            if (userId > 0) {
                try {

                    String userType = fetchUserType(con, userId);
                    Organizer o=new Organizer(userId, con, sc);
                    if ("Organizer".equalsIgnoreCase(userType)) {
                        o.org();
                    } else {
                        ProfileCheck profileCheck = new ProfileCheck(con, sc);
                        profileCheck.checkProfile(userId);
                        Player player = new Player(userId);
                        handlePlayerMenu(player, sc);
                    }
                } catch (Exception e) {
                    System.out.println("Error during post-login processing: " + e.getMessage());
                }
                break;
            } else {
                System.out.println("Try again or enter 0 to go back to the main menu.");
                if (sc.nextLine().trim().equals("0")) break;
            }
        }
    }

    private static void handlePlayerMenu(Player player, Scanner sc) {
        int ch = 0;
        do {
            System.out.println("\nPlayer Menu:");
            System.out.println("1. Show Stats");
            System.out.println("2. Logout");
            System.out.print("Enter choice: ");

            try {
                ch = sc.nextInt();
                sc.nextLine();

                if (ch == 1) {
                    player.showStats();
                } else if (ch == 2) {
                    System.out.println("Logging out…");
                } else {
                    System.out.println("⚠ Invalid choice, please enter 1 or 2.");
                }
            } catch (InputMismatchException e) {
                System.out.println("⚠ Please enter a valid number (1 or 2).");
                sc.nextLine(); // clear invalid input
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        } while (ch != 2);
    }

    private static String fetchUserType(Connection con, int id) {
        String userType = "";
        try (PreparedStatement pst = con.prepareStatement("SELECT userType FROM user WHERE id = ?")) {
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                userType = rs.getString("userType");
            }
        } catch (Exception e) {
            System.out.println("Error retrieving user type: " + e.getMessage());
        }
        return userType;
    }
    static void createTables(Statement stmt) throws Exception{
        stmt.execute("CREATE TABLE IF NOT EXISTS user (id INT AUTO_INCREMENT PRIMARY KEY, name VARCHAR(100) NOT NULL, contact VARCHAR(10) UNIQUE NOT NULL, city VARCHAR(50), gender VARCHAR(10), dob DATE, userType VARCHAR(10));");
        stmt.execute("CREATE TABLE IF NOT EXISTS PlayerProfile (pid INT PRIMARY KEY, playing_role VARCHAR(50) DEFAULT NULL, batting_style VARCHAR(50) DEFAULT NULL, bowling_style VARCHAR(50) DEFAULT NULL, FOREIGN KEY (pid) REFERENCES user(id));");
        stmt.execute("CREATE TABLE IF NOT EXISTS Team (team_id INT AUTO_INCREMENT PRIMARY KEY, team_name VARCHAR(100) UNIQUE NOT NULL, city VARCHAR(50), captain_name VARCHAR(100), captain_contact VARCHAR(10), user_id INT, FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE SET NULL);");
        stmt.execute("CREATE TABLE IF NOT EXISTS Team_Players (team_id INT, player_id INT, PRIMARY KEY (team_id, player_id), FOREIGN KEY (team_id) REFERENCES Team(team_id) ON DELETE CASCADE, FOREIGN KEY (player_id) REFERENCES PlayerProfile(pid) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS Tournament (tournament_id INT AUTO_INCREMENT PRIMARY KEY, tournament_name VARCHAR(255) NOT NULL, city VARCHAR(100), ground VARCHAR(100), organizer_name VARCHAR(100), organizer_contact VARCHAR(10), start_date DATE, end_date DATE, ball_type VARCHAR(20), pitch_type VARCHAR(20), match_type VARCHAR(50), organizer_id INT, FOREIGN KEY (organizer_id) REFERENCES user(id) ON DELETE SET NULL);");
        stmt.execute("CREATE TABLE IF NOT EXISTS Tournament_Teams (tournament_id INT, team_id INT, PRIMARY KEY (tournament_id, team_id), FOREIGN KEY (tournament_id) REFERENCES Tournament(tournament_id) ON DELETE CASCADE, FOREIGN KEY (team_id) REFERENCES Team(team_id) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS Matches (match_id INT AUTO_INCREMENT PRIMARY KEY, teamA_id INT, teamB_id INT, tournament_id INT, match_date DATETIME DEFAULT CURRENT_TIMESTAMP, venue_city VARCHAR(100), venue_ground VARCHAR(100), overs INT, overs_per_bowler INT, toss_winner_team_id INT, toss_decision VARCHAR(10), winner_team_id INT, match_type VARCHAR(50), ball_type VARCHAR(20), pitch_type VARCHAR(20), FOREIGN KEY (teamA_id) REFERENCES Team(team_id), FOREIGN KEY (teamB_id) REFERENCES Team(team_id), FOREIGN KEY (tournament_id) REFERENCES Tournament(tournament_id) , FOREIGN KEY (toss_winner_team_id) REFERENCES Team(team_id), FOREIGN KEY (winner_team_id) REFERENCES Team(team_id));");
        stmt.execute("CREATE TABLE IF NOT EXISTS batting_stats (pid INT PRIMARY KEY, matches INT DEFAULT 0, innings INT DEFAULT 0, not_outs INT DEFAULT 0, highest_runs INT DEFAULT 0, runs INT DEFAULT 0, balls INT DEFAULT 0, avg Numeric(10,2) DEFAULT 0.0, sr Numeric(10,2) DEFAULT 0.0, thirties INT DEFAULT 0, fifties INT DEFAULT 0, hundreds INT DEFAULT 0, fours INT DEFAULT 0, sixes INT DEFAULT 0, ducks INT DEFAULT 0, FOREIGN KEY (pid) REFERENCES PlayerProfile(pid) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS bowling_stats (pid INT PRIMARY KEY, matches INT DEFAULT 0, innings INT DEFAULT 0, balls DOUBLE DEFAULT 0.0, maidens INT DEFAULT 0, wickets INT DEFAULT 0, runs INT DEFAULT 0, best_bowling VARCHAR(10) DEFAULT '0/0', 5_wickets_haul INT DEFAULT 0, economy Numeric(10,2) DEFAULT 0.0, sr Numeric(10,2) DEFAULT 0.0, avg Numeric(10,2) DEFAULT 0.0, wides INT DEFAULT 0, no_balls INT DEFAULT 0, dot_balls INT DEFAULT 0, 4s_conceded INT DEFAULT 0, 6s_conceded INT DEFAULT 0, FOREIGN KEY (pid) REFERENCES PlayerProfile(pid) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS fielding_stats (pid INT PRIMARY KEY, matches INT DEFAULT 0, catches INT DEFAULT 0, run_outs INT DEFAULT 0, caught_behind INT DEFAULT 0, stumping INT DEFAULT 0, FOREIGN KEY (pid) REFERENCES PlayerProfile(pid) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS captain_stats (pid INT PRIMARY KEY, matches_as_captain INT DEFAULT 0, toss_won INT DEFAULT 0, matches_won INT DEFAULT 0, matches_lost INT DEFAULT 0, win_percentage DOUBLE DEFAULT 0.0, loss_percentage DOUBLE DEFAULT 0.0, FOREIGN KEY (pid) REFERENCES PlayerProfile(pid) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS Score (ball_number INT, match_id INT, total_runs INT, total_wickets INT, balls_bowled INT, current_over INT, balls_in_current_over INT, striker VARCHAR(100), non_striker VARCHAR(100), bowler VARCHAR(100), ball_type VARCHAR(20) DEFAULT 'NORMAL', innings INT,FOREIGN KEY (match_id) REFERENCES Matches(match_id) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS Tournament_Round (round_id INT AUTO_INCREMENT PRIMARY KEY, tournament_id INT, round_name VARCHAR(100), round_type VARCHAR(50),status VARCHAR(20) DEFAULT 'SCHEDULED', FOREIGN KEY (tournament_id) REFERENCES Tournament(tournament_id) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS Tournament_Points (tournament_id INT, team_id INT, matches_played INT DEFAULT 0, wins INT DEFAULT 0, losses INT DEFAULT 0, ties INT DEFAULT 0, no_results INT DEFAULT 0, points INT DEFAULT 0, runs_scored INT DEFAULT 0, runs_conceded INT DEFAULT 0, overs_faced DOUBLE DEFAULT 0.0, overs_bowled DOUBLE DEFAULT 0.0, nrr DOUBLE DEFAULT 0.0, PRIMARY KEY (tournament_id, team_id), FOREIGN KEY (tournament_id) REFERENCES Tournament(tournament_id) ON DELETE CASCADE, FOREIGN KEY (team_id) REFERENCES Team(team_id) ON DELETE CASCADE);");
        stmt.execute("CREATE TABLE IF NOT EXISTS Scoreboard (\n" + "    scoreboard_id INT AUTO_INCREMENT PRIMARY KEY,\n" + "    match_id INT,\n" + "    file_path VARCHAR(255),\n" + "    saved_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,\n" + "    FOREIGN KEY (match_id) REFERENCES Matches(match_id) ON DELETE CASCADE\n" + ");\n");
    }
}