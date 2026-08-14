package team;

import profile.registration.Register;
import database_Connection.DatabaseConnection;
import profile.profileCheck.ProfileCheck;
import stats.Player;

import java.sql.*;
import java.util.*;

public class Team{
    public String captainName;
    public int teamId;
    public String teamName;
    String city;
    public int captainPid; // This variable needs to be set
    public int totalPlayers;
    public List<Player> players = new ArrayList<>();
    Connection con= DatabaseConnection.getConnection();
    Scanner sc=new Scanner(System.in);

    public Team(String teamName, String city) throws Exception{
        this.teamName = teamName;
        this.city = city;
        this.teamId = getTeamId(); // Set the teamId when the object is created
    }
    public void setTeamDetails() throws SQLException {
        int existingPlayerCount = 0;
        existingPlayerCount = getPlayers();

        int number_of_players = 0;
        while (true) {
            try {
                System.out.print("Enter number of new players to add (Excluding old players): ");
                number_of_players = sc.nextInt();
                sc.nextLine();
                if (number_of_players >= 0) break;
                else System.out.println("Please enter a non-negative number.");
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a valid number.");
                sc.nextLine(); // clear buffer
            }
        }

        for (int i = 0; i < number_of_players; i++) {
            String contactNo;

            // Validate contact number
            while (true) {
                System.out.print("Enter mobile no. for player " + (i + 1) + ": ");
                contactNo = sc.nextLine();

                if (contactNo.matches("^[6-9]\\d{9}$")) {
                    break;
                } else {
                    System.out.println("Invalid number. Must be 10 digits and start with 6-9.");
                }
            }

            Player player = null;
            player = fetchPlayerByContact(contactNo);

            if (player == null) {
                System.out.println("Player data doesn't exist.");
                System.out.println("Registering new player...");

                Register r = new Register();
                r.reg(con, sc);

                int userId = r.userid;
                int ch;
                while (true) {
                    try {
                        System.out.println("Do you want to add player profile?");
                        System.out.println("1. Yes\n2. No");
                        ch = sc.nextInt();
                        sc.nextLine();

                        if (ch == 1) {
                            ProfileCheck p = new ProfileCheck(con, sc);
                            p.checkProfile(r.userid); // This creates the profile if it doesn't exist

                            try {
                                String q = "Insert Into team_players values (?, ?)";
                                PreparedStatement pst1 = con.prepareStatement(q);
                                pst1.setInt(1, this.teamId);
                                pst1.setInt(2, userId);
                                int done = pst1.executeUpdate();
                                if (done > 0) {
                                    System.out.println("Entered into team");
                                }
                            } catch (SQLException e) {
                                // Ignore duplicate entry errors if player is already in team
                                if(!e.getSQLState().equals("23000")) {
                                    System.out.println("Error inserting into team_players: " + e.getMessage());
                                }
                            }
                            break;
                        } else if (ch == 2) {
                            try {
                                String q = "Insert Into playerprofile (pid, playing_role, batting_style, bowling_style) values (?, 'N/A','N/A','N/A')";
                                PreparedStatement pst = con.prepareStatement(q);
                                pst.setInt(1, userId);
                                pst.executeUpdate();

                                q = "Insert Into team_players values (?, ?)";
                                PreparedStatement pst1 = con.prepareStatement(q);
                                pst1.setInt(1, this.teamId);
                                pst1.setInt(2, userId);
                                pst1.executeUpdate();
                                System.out.println("Entered into team");

                            } catch (SQLException e) {
                                if(!e.getSQLState().equals("23000")) {
                                    System.out.println("Error inserting player profile/team player: " + e.getMessage());
                                }
                            }
                            break;
                        } else {
                            System.out.println("Invalid choice. Please enter 1 or 2.");
                        }
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter 1 or 2.");
                        sc.nextLine();
                    }
                }

                // Re-fetch player info
                player = fetchPlayerById(r.userid);

                if (player == null) {
                    System.out.println("Failed to fetch newly registered player details. Skipping.");
                    continue;
                }
            }

            // Check if already in list
            boolean alreadyInTeam = false;
            for (Player p : players) {
                if (p.getId() == player.getId()) {
                    alreadyInTeam = true;
                    break;
                }
            }

            if (!alreadyInTeam) {
                try {
                    int userId = player.getId();
                    this.players.add(player);
                    String q = "Insert Into team_players values (?, ?)";
                    PreparedStatement pst1 = con.prepareStatement(q);
                    pst1.setInt(1, this.teamId);
                    pst1.setInt(2, userId);
                    pst1.executeUpdate();
                    System.out.println("Entered into team");
                } catch (SQLException e) {
                    if(!e.getSQLState().equals("23000")) {
                        System.out.println("Error inserting existing player into team: " + e.getMessage());
                    }
                }
            } else {
                System.out.println("Player already exists in the team list. Skipping duplicate.");
            }
        }

        this.totalPlayers = players.size();
    }
    public void setCaptainDetails() {
        int i = 0;
        sc.nextLine();
        for (Player player : players) {
            System.out.println((i + 1) + ".) " + player.name);
            i++;
        }

        while (true) {
            System.out.println("Select Captain (By Name): ");
            String name = sc.nextLine();

            String q = "SELECT id, contact FROM user WHERE name = ?";

            try (PreparedStatement pst = con.prepareStatement(q)) {
                pst.setString(1, name);
                ResultSet rs = pst.executeQuery();

                if (!rs.next()) {
                    System.out.println("No user found with that name. Try again.");
                    continue;
                }

                // --- FIX APPLIED HERE ---
                // Fetched the captain's ID and contact, and stored the ID in the captainPid variable.
                int captainId = rs.getInt("id");
                String contact = rs.getString("contact");
                this.captainPid = captainId; // Store the ID in the object
                this.captainName = name;    // Store the name in the object

                String updateQuery = "UPDATE Team SET captain_name = ?, captain_contact = ? WHERE team_id = ?";
                try (PreparedStatement pst1 = con.prepareStatement(updateQuery)) {
                    pst1.setString(1, name);
                    pst1.setString(2, contact);
                    pst1.setInt(3, this.teamId);
                    int rowsAffected = pst1.executeUpdate();
                    if (rowsAffected > 0) {
                        System.out.println("Captain set successfully!");
                        break; // exit loop
                    } else {
                        System.out.println("Failed to set captain. Team ID may be invalid.");
                    }
                }

            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            }
        }
    }

    private Player fetchPlayerByContact(String contact) {
        try {
            String query = "SELECT u.id, u.name, u.gender, p.playing_role, p.batting_style, p.bowling_style " +
                    "FROM user u JOIN PlayerProfile p ON u.id = p.pid WHERE u.contact = ?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setString(1, contact);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new Player(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("gender"),
                        rs.getString("playing_role"),
                        rs.getString("batting_style"),
                        rs.getString("bowling_style")
                );
            }

            String query1 = "SELECT id, name, gender FROM user WHERE contact = ?";
            PreparedStatement pst1 = con.prepareStatement(query1);
            pst1.setString(1, contact);
            ResultSet rs1 = pst1.executeQuery();

            if (rs1.next()) {
                int id = rs1.getInt("id");
                ProfileCheck p = new ProfileCheck(con, sc);
                p.checkProfile(id);
                return fetchPlayerByContact(contact);
            }

        } catch (SQLException e) {
            System.err.println("SQL error in fetchPlayerByContact: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error in fetchPlayerByContact: " + e.getMessage());
        }
        return null;
    }


    private Player fetchPlayerById(int id) {
        try {
            String query = "SELECT u.id, u.name, u.gender, p.playing_role, p.batting_style, p.bowling_style " +
                    "FROM user u JOIN PlayerProfile p ON u.id = p.pid WHERE u.id = ?";
            PreparedStatement pst = con.prepareStatement(query);
            pst.setInt(1, id);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                return new Player(
                        rs.getInt("id"),
                        rs.getString("name"),
                        rs.getString("gender"),
                        rs.getString("playing_role"),
                        rs.getString("batting_style"),
                        rs.getString("bowling_style")
                );
            }

        } catch (SQLException e) {
            System.err.println("SQL error in fetchPlayerById: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Unexpected error in fetchPlayerById: " + e.getMessage());
        }
        return null;
    }

    public int getTeamId() {
        int team_id = -1;
        String query = "SELECT team_id FROM Team WHERE team_name = ?";

        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setString(1, this.teamName);
            ResultSet rs = pst.executeQuery();

            if (rs.next()) {
                team_id = rs.getInt("team_id");
            }
        } catch (SQLException e) {
            System.err.println("SQL error in getTeamId: " + e.getMessage());
        }
        return team_id;
    }

    int getPlayers() {
        int team_id = this.teamId;
        String query = "SELECT player_id FROM Team_Players WHERE team_id = ?";
        int count = 0;

        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, team_id);
            ResultSet rs = pst.executeQuery();
            System.out.println("Players already existing:\n");

            while (rs.next()) {
                int playerId = rs.getInt("player_id");

                String playerQuery = "SELECT pid, name, gender, playing_role, batting_style, bowling_style " +
                        "FROM PlayerProfile " +
                        "INNER JOIN user ON user.id = PlayerProfile.pid " +
                        "WHERE pid = ?";

                try (PreparedStatement pst1 = con.prepareStatement(playerQuery)) {
                    pst1.setInt(1, playerId);
                    ResultSet rs1 = pst1.executeQuery();

                    if (rs1.next()) {
                        Player p = new Player(
                                rs1.getInt("pid"),
                                rs1.getString("name"),
                                rs1.getString("gender"),
                                rs1.getString("playing_role"),
                                rs1.getString("batting_style"),
                                rs1.getString("bowling_style")
                        );
                        this.players.add(p);
                        System.out.println(p.name);
                        count++;
                    }
                } catch (SQLException e1) {
                    System.err.println("SQL error while fetching player details for playerId=" + playerId + ": " + e1.getMessage());
                }
            }

        } catch (SQLException e) {
            System.err.println("SQL error in getPlayers: " + e.getMessage());
        }

        if (count == 0) {
            System.out.println("----No Players in Team------");
        }
        System.out.println();
        return count;
    }

    public void updateTeamDetails(Team otherTeam) throws Exception {
        System.out.println("Players in Team " + this.teamName + ":");
        for (Player p : this.players) {
            System.out.println(p.name);
        }

        sc.nextLine(); // Clear scanner buffer

        if (this.players.size() > otherTeam.players.size()) {
            System.out.print("Enter Player Name to delete: ");
            String pName = sc.nextLine();

            String selectQuery = "SELECT id FROM user WHERE name = ?";
            try (PreparedStatement pst = con.prepareStatement(selectQuery)) {
                pst.setString(1, pName);
                ResultSet rs = pst.executeQuery();

                if (!rs.next()) {
                    System.out.println("Invalid Name");
                    return;
                }

                int playerId = rs.getInt("id");

                String deleteQuery = "DELETE FROM Team_Players WHERE team_id = ? AND player_id = ?";
                try (PreparedStatement delPst = con.prepareStatement(deleteQuery)) {
                    delPst.setInt(1, this.teamId);
                    delPst.setInt(2, playerId);

                    int affected = delPst.executeUpdate();
                    if (affected > 0) {
                        System.out.println("Player " + pName + " removed from Team for this Match");
                        players.removeIf(p -> p.name.equalsIgnoreCase(pName));
                        this.totalPlayers--;
                    } else {
                        System.out.println("Player was not part of the team.");
                    }
                }
            } catch (SQLException e) {
                System.err.println("SQL error while deleting player " + pName + ": " + e.getMessage());
            }

        } else {
            System.out.print("Enter Player Name to add: ");
            String pName = sc.nextLine();

            String selectQuery = "SELECT id FROM user WHERE name = ?";
            try (PreparedStatement pst = con.prepareStatement(selectQuery)) {
                pst.setString(1, pName);
                ResultSet rs = pst.executeQuery();

                if (!rs.next()) {
                    System.out.println("No such Player registered yet");
                    return;
                }

                int playerId = rs.getInt("id");

                String checkQuery = "SELECT * FROM Team_Players WHERE team_id = ? AND player_id = ?";
                try (PreparedStatement checkPst = con.prepareStatement(checkQuery)) {
                    checkPst.setInt(1, this.teamId);
                    checkPst.setInt(2, playerId);
                    ResultSet checkRs = checkPst.executeQuery();

                    if (checkRs.next()) {
                        System.out.println("Player is already in the team.");
                    } else {
                        String insertQuery = "INSERT INTO Team_Players (team_id, player_id) VALUES (?, ?)";
                        try (PreparedStatement insPst = con.prepareStatement(insertQuery)) {
                            insPst.setInt(1, this.teamId);
                            insPst.setInt(2, playerId);
                            insPst.executeUpdate();
                            System.out.println("Player " + pName + " added to your Team");
                            this.totalPlayers++;
                        } catch (SQLException e) {
                            if(!e.getSQLState().equals("23000")) {
                                System.err.println("SQL error while inserting player " + pName + " into team: " + e.getMessage());
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                System.err.println("SQL error while fetching playerId for " + pName + ": " + e.getMessage());
            }
        }
    }


    public String getTeamName() {
        return teamName;
    }
    public void setTeamName(String teamName) {
        this.teamName = teamName;
    }
    public String getCity() {
        return city;
    }
    public void setCity(String city) {
        this.city = city;
    }

    public String getTeamCaptainName() {
        return captainName;
    }
    public void setTeamCaptainName(String teamCaptainName) {
        this.captainName = teamCaptainName;
    }
}
