package organizer;

import match.Match;
import team.Team;
import dataStructure.S_LinkedList;

import java.sql.*;
import java.util.*;
public class Organizer {
    int organizerId;
    Connection con;
    Scanner sc;

    // Constructor to properly initialize the class
    public Organizer(int id, Connection con, Scanner sc) {
        this.organizerId = id;
        this.con = con;
        this.sc = sc;
    }

    // Main method for organizer actions
    public void org() throws Exception {
        while (true) {
            try {
                System.out.println("""
                1. Add a tournament/series
                2. Start a match
                3. View Points Table
                0. Back (previous menu)""");

                int choice = -1;
                try {
                    choice = sc.nextInt();
                    //sc.nextLine();
                    // clear buffer
                } catch (InputMismatchException ime) {
                    System.err.println("⚠ Invalid input. Please enter a number.");
                    sc.nextLine(); // clear buffer
                    continue; // retry
                }

                if (choice == 1) {
                    try {
                        addTournament();
                    } catch (Exception e) {
                        System.err.println("❌ Error while adding tournament: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (choice == 2) {
                    try (Statement st = con.createStatement();
                         ResultSet rst = st.executeQuery("SELECT * FROM tournament;")) {

                        boolean atLeastOneTournamentExists = rst.next(); // just check existence

                        if (!atLeastOneTournamentExists) {
                            try {
                                startMatch();
                            } catch (Exception e) {
                                System.err.println("❌ Error while starting match: " + e.getMessage());
                                e.printStackTrace();
                            }
                        } else {
                            while (true) {
                                System.out.println("""
                                1. Tournament
                                2. Individual
                                0. Back""");

                                int matchChoice = -1;
                                try {
                                    matchChoice = sc.nextInt();
                                    sc.nextLine();
                                } catch (InputMismatchException ime) {
                                    System.err.println("⚠ Invalid input. Please enter a number.");
                                    sc.nextLine();
                                    continue; // retry
                                }

                                if (matchChoice == 1) {
                                    try {
                                        startTournament();
                                    } catch (Exception e) {
                                        System.err.println("❌ Error while starting tournament: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                    break;
                                } else if (matchChoice == 2) {
                                    try {
                                        startMatch();
                                    } catch (Exception e) {
                                        System.err.println("❌ Error while starting individual match: " + e.getMessage());
                                        e.printStackTrace();
                                    }
                                    break;
                                } else if (matchChoice == 0) {
                                    System.out.println("⬅ Going back...");
                                    break;
                                } else {
                                    System.out.println("⚠ Invalid choice. Please try again.");
                                }
                            }
                        }
                    } catch (SQLException e) {
                        System.err.println("❌ SQL error while checking tournaments: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else if (choice == 0) {
                    System.out.println("⬅ Back to previous menu...");
                    return;
                } else if (choice==3) {
                    getId();
                } else {
                    System.out.println("⚠ Invalid choice. Please try again.");
                }

            } catch (Exception e) {
                System.err.println("❌ Unexpected error in org(): " + e.getMessage());
                e.printStackTrace();
            }
        }
    }


    Team selectOrAddTeam(Team otherTeam, String teamLetter) throws Exception {
        while (true) {
            System.out.println("\n--- Select Team " + teamLetter + " ---");
            System.out.println("1. Select Existing Team");
            System.out.println("2. Add New Team");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");

            int choice = -1;
            try {
                choice = sc.nextInt();
                sc.nextLine(); // clear buffer
            } catch (InputMismatchException ime) {
                System.err.println("⚠ Invalid input. Please enter 0, 1, or 2.");
                sc.nextLine();
                continue; // retry menu
            }

            if (choice == 0) {
                System.out.println("⬅ Going back...");
                return null;
            }

            if (choice == 1) { // Select Existing Team
                System.out.println("\nAvailable Teams:");
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT team_name FROM Team")) {
                    int teamCount = 0;
                    while (rs.next()) {
                        System.out.println("- " + rs.getString("team_name"));
                        teamCount++;
                    }
                    if (teamCount == 0) {
                        System.out.println("⚠ No teams found. Please add a team first.");
                        continue; // go back to menu
                    }
                } catch (SQLException e) {
                    System.err.println("❌ SQL error while fetching teams: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

                System.out.print("Enter the name of the team to select: ");
                String teamSearched = sc.nextLine().trim();

                if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamSearched)) {
                    System.out.println("⚠ Error: Cannot select the same team for both A and B.");
                    continue;
                }

                String sql = "SELECT team_name, city FROM Team WHERE team_name = ?";
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, teamSearched);
                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {
                        System.out.println("✅ Team '" + teamSearched + "' selected.");
                        return new Team(
                                rs.getString("team_name"),
                                rs.getString("city")
                        );
                    } else {
                        System.out.println("⚠ Team not found.");
                        continue; // retry
                    }
                } catch (SQLException e) {
                    System.err.println("❌ SQL error while selecting team: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

            } else if (choice == 2) { // Add New Team
                System.out.print("Enter New Team Name: ");
                String teamName = sc.nextLine().trim();

                if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamName)) {
                    System.out.println("⚠ Error: A team with this name is already selected.");
                    continue;
                }

                System.out.print("Enter city/town: ");
                String cityName = sc.nextLine().trim();

                String sql = "INSERT INTO Team (team_name, city, user_id) VALUES (?, ?, ?)";
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, teamName);
                    pst.setString(2, cityName);
                    pst.setInt(3, organizerId);

                    int inserted = pst.executeUpdate();
                    if (inserted > 0) {
                        System.out.println("✅ Team '" + teamName + "' added and selected.");
                        return new Team(teamName, cityName);
                    } else {
                        System.out.println("⚠ Failed to add team. Please try again.");
                    }
                } catch (SQLException e) {
                    System.err.println("❌ SQL error while inserting new team: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

            } else {
                System.out.println("⚠ Invalid choice. Please try again.");
            }
        }
    }


    Team selectOrAddTeamForTournament(Team otherTeam, String teamLetter, String tName) throws Exception {
        while (true) {
            System.out.println("\n--- Select Team " + teamLetter + " ---");
            System.out.println("1. Tournament Teams");
            System.out.println("2. My Teams");
            System.out.println("3. Add New Team");
            System.out.println("0. Back");
            System.out.print("Enter choice: ");

            int choice = -1;
            try {
                choice = sc.nextInt();
                sc.nextLine(); // clear buffer
            } catch (InputMismatchException ime) {
                System.err.println("⚠ Invalid input. Please enter 0, 1, 2, or 3.");
                sc.nextLine();
                continue; // retry
            }

            if (choice == 0) {
                System.out.println("⬅ Going back...");
                return null;
            }

            if (choice == 1) { // Tournament Teams
                System.out.println("\nAvailable Teams for Tournament: " + tName);
                String query = """
                SELECT DISTINCT team_name, tournament_name
                FROM team
                JOIN tournament_teams ON team.team_id = tournament_teams.team_id
                JOIN tournament ON tournament_teams.tournament_id = tournament.tournament_id
                WHERE tournament_name = ?""";

                try (PreparedStatement pst = con.prepareStatement(query)) {
                    pst.setString(1, tName);
                    ResultSet rs = pst.executeQuery();

                    int teamCount = 0;
                    while (rs.next()) {
                        System.out.println("- " + rs.getString("team_name"));
                        teamCount++;
                    }
                    if (teamCount == 0) {
                        System.out.println("⚠ No teams found for tournament " + tName + ". Please add a team first.");
                        continue;
                    }
                } catch (SQLException e) {
                    System.err.println("❌ SQL error while fetching tournament teams: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

                System.out.print("Enter the name of the team to select: ");
                String teamSearched = sc.nextLine().trim();

                if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamSearched)) {
                    System.out.println("⚠ Error: Cannot select the same team for both A and B.");
                    continue;
                }

                String sql = "SELECT team_name, city FROM Team WHERE team_name = ?";
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, teamSearched);
                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {
                        System.out.println("✅ Team '" + teamSearched + "' selected.");
                        return new Team(rs.getString("team_name"), rs.getString("city"));
                    } else {
                        System.out.println("⚠ Team not found.");
                        continue;
                    }
                } catch (SQLException e) {
                    System.err.println("❌ SQL error while selecting tournament team: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

            } else if (choice == 2) { // My Teams
                System.out.println("\nAvailable My Teams:");
                try (Statement st = con.createStatement();
                     ResultSet rs = st.executeQuery("SELECT team_name FROM team")) {
                    int teamCount = 0;
                    while (rs.next()) {
                        System.out.println("- " + rs.getString("team_name"));
                        teamCount++;
                    }
                    if (teamCount == 0) {
                        System.out.println("⚠ No teams found. Please add a team first.");
                        continue;
                    }
                } catch (SQLException e) {
                    System.err.println("❌ SQL error while fetching my teams: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

                System.out.print("Enter the name of the team to select: ");
                String teamSearched = sc.nextLine().trim();

                if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamSearched)) {
                    System.out.println("⚠ Error: Cannot select the same team for both A and B.");
                    continue;
                }

                String sql = "SELECT team_name, city FROM Team WHERE team_name = ?";
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, teamSearched);
                    ResultSet rs = pst.executeQuery();

                    if (rs.next()) {
                        System.out.println("✅ Team '" + teamSearched + "' selected.");
                        return new Team(rs.getString("team_name"), rs.getString("city"));
                    } else {
                        System.out.println("⚠ Team not found.");
                        continue;
                    }
                } catch (SQLException e) {
                    System.err.println("❌ SQL error while selecting my team: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

            } else if (choice == 3) { // Add New Team
                System.out.print("Enter New Team Name: ");
                String teamName = sc.nextLine().trim();

                if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamName)) {
                    System.out.println("⚠ Error: A team with this name is already selected.");
                    continue;
                }

                System.out.print("Enter city/town: ");
                String cityName = sc.nextLine().trim();

                String sql = "INSERT INTO Team (team_name, city, user_id) VALUES (?, ?, ?)";
                try (PreparedStatement pst = con.prepareStatement(sql)) {
                    pst.setString(1, teamName);
                    pst.setString(2, cityName);
                    pst.setInt(3, organizerId);

                    int inserted = pst.executeUpdate();
                    if (inserted > 0) {
                        System.out.println("✅ Team '" + teamName + "' added and selected.");
                        return new Team(teamName, cityName);
                    } else {
                        System.out.println("⚠ Failed to add team. Please try again.");
                    }
                } catch (SQLException e) {
                    System.err.println("❌ SQL error while inserting new team: " + e.getMessage());
                    e.printStackTrace();
                    continue;
                }

            } else {
                System.out.println("⚠ Invalid choice. Please try again.");
            }
        }
    }


    public void startMatch() throws Exception {
        Team teamA = null;
        Team teamB = null;

        // Step 1: Select Teams
        while (teamA == null || teamB == null) {
            System.out.println("\n--- Match Setup ---");
            System.out.println("Team A: " + (teamA != null ? teamA.getTeamName() : "Not Selected"));
            System.out.println("Team B: " + (teamB != null ? teamB.getTeamName() : "Not Selected"));
            System.out.println("""
                1. Select/Add Team A
                2. Select/Add Team B
                0. Back
                Enter choice: """);

            int choice = -1;
            try {
                choice = sc.nextInt();
                sc.nextLine(); // clear buffer
            } catch (InputMismatchException ime) {
                System.err.println("⚠ Invalid input. Please enter 0, 1, or 2.");
                sc.nextLine();
                continue;
            }

            switch (choice) {
                case 0 -> {
                    System.out.println("⬅ Going back...");
                    return; // cancel match setup
                }
                case 1 -> teamA = selectOrAddTeam(teamB, "A");
                case 2 -> teamB = selectOrAddTeam(teamA, "B");
                default -> System.out.println("⚠ Invalid choice. Please try again.");
            }
        }

        // Step 2: Enter squads & captains
        System.out.println("\n✅ Match Ready: " + teamA.teamName + " vs " + teamB.teamName);

        System.out.println("\nEnter Squad of team: " + teamA.teamName);
        teamA.setTeamDetails();
        teamA.setCaptainDetails();

        System.out.println("\nEnter Squad of team: " + teamB.teamName);
        teamB.setTeamDetails();
        teamB.setCaptainDetails();

        // Step 3: Validate squads before match
        while (true) {
            if (teamA.totalPlayers != teamB.totalPlayers) {
                System.out.println("\n⚠ " + teamA.teamName + " and " + teamB.teamName +
                        " squads are not the same size.\n" +
                        "1. Yes, continue anyway\n" +
                        "2. Update teams\n" +
                        "0. Cancel match");

                int ch = -1;
                try {
                    ch = sc.nextInt();
                    sc.nextLine(); // clear buffer
                } catch (InputMismatchException ime) {
                    System.err.println("⚠ Invalid input. Please enter 0, 1, or 2.");
                    sc.nextLine();
                    continue;
                }

                if (ch == 1) {
                    // continue with mismatch
                    playMatch(teamA, teamB);
                    break;
                } else if (ch == 2) {
                    System.out.println("Update Team:");
                    System.out.println("1. " + teamA.teamName);
                    System.out.println("2. " + teamB.teamName);
                    System.out.println("(other key) Skip update");

                    int ch1 = -1;
                    try {
                        ch1 = sc.nextInt();
                        sc.nextLine();
                    } catch (InputMismatchException ime) {
                        System.err.println("⚠ Invalid input. Skipping update.");
                        sc.nextLine();
                    }

                    if (ch1 == 1) {
                        teamA.updateTeamDetails(teamB);
                    } else if (ch1 == 2) {
                        teamB.updateTeamDetails(teamA);
                    } else {
                        System.out.println("Skipping team update.");
                    }

                    if (teamA.totalPlayers == teamB.totalPlayers) {
                        System.out.println("✅ Now both teams have the same squad size.\n");
                    }

                } else if (ch == 0) {
                    System.out.println("⬅ Match cancelled.");
                    return;
                } else {
                    System.out.println("⚠ Invalid choice. Try again.");
                }

            } else { // squads match
                playMatch(teamA, teamB);
                break;
            }
        }
    }

    // Helper to reduce repetition
    private void playMatch(Team teamA, Team teamB) throws Exception {
        Match m = new Match(teamA, teamB, con);
        m.matchTypeDetails();
        System.out.println("\n🎲 Toss....");
        m.toss();
        m.playMatch(0);
        System.out.println("\n🏆 Results");
        m.results();
    }

    public void addTournament() throws Exception {
        sc.nextLine(); // consume leftover newline

        System.out.println("\n--- Add New Tournament ---");

        // 1. Tournament Details
        System.out.print("Enter tournament/series name: ");
        String tName = sc.nextLine();

        System.out.print("Enter city: ");
        String city = sc.nextLine();

        System.out.print("Enter ground name: ");
        String ground = sc.nextLine();

        System.out.print("Enter organizer's name: ");
        String organizerName = sc.nextLine();

        // Phone (loop + validation)
        String organizerNo;
        while (true) {
            System.out.print("Enter organizer's phone no. (10 digits, starts 6-9) [0 = Back]: ");
            organizerNo = sc.nextLine().trim();
            if ("0".equals(organizerNo)) {
                System.out.println("Back to previous menu.");
                return;
            }
            if (organizerNo.matches("^[6-9]\\d{9}$")) {
                break;
            }
            System.out.println("Invalid mobile number. Please try again.");
        }

        // Dates (loop + validation; end >= start)
        String startDate;
        java.time.LocalDate sDate = null;
        while (true) {
            System.out.print("Enter tournament start date (YYYY-MM-DD) [0 = Back]: ");
            startDate = sc.nextLine().trim();
            if ("0".equals(startDate)) {
                System.out.println("Back to previous menu.");
                return;
            }
            try {
                sDate = java.time.LocalDate.parse(startDate);
                break;
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        String endDate;
        java.time.LocalDate eDate = null;
        while (true) {
            System.out.print("Enter tournament end date (YYYY-MM-DD) [0 = Back]: ");
            endDate = sc.nextLine().trim();
            if ("0".equals(endDate)) {
                System.out.println("Back to previous menu.");
                return;
            }
            try {
                eDate = java.time.LocalDate.parse(endDate);
                if (!eDate.isBefore(sDate)) {
                    break;
                }
                System.out.println("End date must be on or after start date.");
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Invalid date format. Please use YYYY-MM-DD.");
            }
        }

        // 2. Ball Type
        String ballType = "";
        while (true) {
            try {
                System.out.println("\nSelect Ball Type:");
                System.out.println("1. Tennis\n2. Leather\n3. Other");
                System.out.print("Enter choice: ");
                int bTypeChoice = sc.nextInt();
                sc.nextLine();
                ballType = switch (bTypeChoice) {
                    case 1 -> "Tennis";
                    case 2 -> "Leather";
                    default -> "Other";
                };
                break;
            } catch (InputMismatchException ime) {
                System.out.println("Invalid input. Please enter 1, 2, or 3.");
                sc.nextLine();
            }
        }

        // 3. Pitch Type
        String[] pType = {"Rough", "Cement", "Turf", "Astroturf", "Matting"};
        String pitchType = "";
        while (true) {
            try {
                System.out.println("\nSelect Pitch Type:");
                for (int i = 0; i < pType.length; i++) {
                    System.out.println((i + 1) + ". " + pType[i]);
                }
                System.out.print("Enter choice: ");
                int pChoice = sc.nextInt();
                sc.nextLine();
                if (pChoice >= 1 && pChoice <= pType.length) {
                    pitchType = pType[pChoice - 1];
                    break;
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            } catch (InputMismatchException ime) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
            }
        }

        // 4. Match Type
        String[] mType = {"Limited Overs", "Box/Turf Cricket", "Pair Cricket", "Test Match", "The Hundred"};
        String matchType = "";
        while (true) {
            try {
                System.out.println("\nSelect Match Type:");
                for (int i = 0; i < mType.length; i++) {
                    System.out.println((i + 1) + ". " + mType[i]);
                }
                System.out.print("Enter choice: ");
                int mChoice = sc.nextInt();
                sc.nextLine();
                if (mChoice >= 1 && mChoice <= mType.length) {
                    matchType = mType[mChoice - 1];
                    break;
                } else {
                    System.out.println("Invalid choice. Try again.");
                }
            } catch (InputMismatchException ime) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
            }
        }

        // 5. Insert Tournament into DB
        String sql = """
         INSERT INTO Tournament
         (tournament_name, city, ground, organizer_name, organizer_contact,
          start_date, end_date, ball_type, pitch_type, match_type, organizer_id)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
         """;

        try (PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, tName);
            pst.setString(2, city);
            pst.setString(3, ground);
            pst.setString(4, organizerName);
            pst.setString(5, organizerNo);
            pst.setString(6, startDate);
            pst.setString(7, endDate);
            pst.setString(8, ballType);
            pst.setString(9, pitchType);
            pst.setString(10, matchType);
            pst.setInt(11, this.organizerId);

            int rowsAffected = pst.executeUpdate();

            if (rowsAffected > 0) {
                System.out.println("Tournament '" + tName + "' has been added successfully!");

                ResultSet rsKeys = pst.getGeneratedKeys();
                int newTournamentId = -1;
                if (rsKeys.next()) {
                    newTournamentId = rsKeys.getInt(1);
                }

                Statement st = con.createStatement();

                // Tournament menu loop
                while (true) {
                    try {
                        System.out.println("""
                        \n--- Tournament Menu ---
                        1. About
                        2. Teams
                        3. Matches
                        4. Points Table
                        0. Back
                        Enter choice: """);
                        int tournamentChoice = sc.nextInt();
                        sc.nextLine();

                        switch (tournamentChoice) {
                            case 0 -> {
                                System.out.println("Returning to previous menu...");
                                return;
                            }
                            case 1 -> {
                                String orgInfoQuery =
                                        "SELECT organizer_name, city, COUNT(*) " +
                                                "FROM tournament WHERE organizer_id=" + this.organizerId +
                                                " GROUP BY organizer_id";
                                ResultSet orgInfo = st.executeQuery(orgInfoQuery);
                                System.out.println("\n--- Organizer Details ---");
                                while (orgInfo.next()) {
                                    System.out.println("Name : " + orgInfo.getString(1));
                                    System.out.println("City : " + orgInfo.getString(2));
                                    System.out.println("Tournaments Organized : " + orgInfo.getInt(3));
                                }
                                System.out.println("\n--- Tournament Details ---");
                                String tournamentInfoQuery =
                                        "SELECT tournament_name, start_date, end_date, ground, ball_type, tournament_id " +
                                                "FROM tournament WHERE tournament_id=" + newTournamentId;
                                ResultSet tournamentInfo = st.executeQuery(tournamentInfoQuery);
                                while (tournamentInfo.next()) {
                                    System.out.println("Name : " + tournamentInfo.getString(1));
                                    System.out.println("Date : " + tournamentInfo.getString(2) + " to " + tournamentInfo.getString(3));
                                    System.out.println("Ground : " + tournamentInfo.getString(4));
                                    System.out.println("Ball Type : " + tournamentInfo.getString(5));
                                    System.out.println("Tournament ID : " + tournamentInfo.getString(6));
                                }
                            }
                            case 2 -> addTeamsForTournament(newTournamentId);
                            case 3 -> {
                                System.out.println("1. Schedule Match");
                                System.out.println("2. Start Match");
                                int matchChoice = sc.nextInt();
                                sc.nextLine();
                                if (matchChoice == 1) {
                                    addNewRound(tName);
                                } else if (matchChoice == 2) {
                                    startTournamentMatch(tName);
                                } else {
                                    System.out.println("Invalid choice.");
                                }
                            }
                            case 4 -> {
                                // TODO: implement points table
                                System.out.println("Points table feature not yet implemented.");
                                Match.displayTournamentPoints(con,newTournamentId);
                            }
                            default -> System.out.println("Invalid choice. Try again.");
                        }
                    } catch (InputMismatchException ime) {
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine();
                    }
                }

            } else {
                System.out.println("Error: Could not add the tournament.");
            }
        }
    }
    int getId() throws Exception{
        int tournamentId=0;
        Statement st = con.createStatement();
        ResultSet tournaments = st.executeQuery("select tournament_id,tournament_name from tournament;");
        HashMap<Integer,String> tournamentsList = new HashMap<>();
        while (tournaments.next()) {
            System.out.println(tournaments.getString(2));
            tournamentsList.put(tournaments.getInt(1),tournaments.getString(2).toLowerCase());
        }
        sc.nextLine();
        System.out.println("Select tournament : ");
        String tName = sc.nextLine();
        if (tournamentsList.containsValue((tName.toLowerCase()))) {
            Statement s = con.createStatement();
            for (Map.Entry<Integer, String> entry : tournamentsList.entrySet()) {
                if (entry.getValue().equals(tName)) {
                    tournamentId = entry.getKey();
                    if(tournamentId==0)
                    {
                        System.out.println("Tournament is not played yet!");
                    }
                    else {
                        Match.displayTournamentPoints(con,tournamentId);
                    }
                }
            }
        }
        else
        {
            System.out.println("invalid tournament name");
        }
        return tournamentId;
    }
    void startTournament() throws Exception
    {
        int tournamentId=0;
        Statement st = con.createStatement();
        ResultSet tournaments = st.executeQuery("select tournament_id,tournament_name from tournament;");
        HashMap<Integer,String> tournamentsList = new HashMap<>();
        while (tournaments.next()) {
            System.out.println(tournaments.getString(2));
            tournamentsList.put(tournaments.getInt(1),tournaments.getString(2).toLowerCase());
        }
        sc.nextLine();
        S_LinkedList rounds = new S_LinkedList();
        while(true) {
            System.out.println("Select tournament : ");
            String tName = sc.nextLine();
            if (tournamentsList.containsValue((tName.toLowerCase()))) {
                Statement s = con.createStatement();
                for (Map.Entry<Integer, String> entry : tournamentsList.entrySet()) {
                    if (entry.getValue().equals(tName)) {
                        tournamentId = entry.getKey();
                    }
                }
                ResultSet r=s.executeQuery("select round_name from tournament_round where tournament_id=(select tournament_id from tournament where tournament_name='"+tName+"') and status='scheduled';");
                boolean atleastOneround=false;
                while (r.next())
                {
                    atleastOneround=true;
                    //System.out.println(r.getString("round_name"));

                    /*if (!rounds.findByName(r.getString("round_name"))) {
                        rounds.insertAtLast(r.getString("round_name"));
                    }*/

                rounds.insertAtLast(r.getString("round_name"));
                }
            if(!atleastOneround) {
                System.out.println("*You will need to add Rounds and Groups to generate Points Table.");
                addNewRound(tName);
            }
            else
            {
                rounds.display();
                System.out.println("enter round : ");
                String round_selected = rounds.findByPosition(sc.nextInt());
                startTournamentMatch(tName);
                PreparedStatement pst = con.prepareStatement("UPDATE tournament_round SET status='completed' WHERE round_name=? AND tournament_id=?");
                rounds.delete(round_selected);
                pst.setString(1, round_selected);
                pst.setInt(2, tournamentId);
                pst.executeUpdate();
            }
            break;
        }
        else
        {
            System.out.println("invalid tournament name");
        }
    }
    }



    void addNewRound(String tName) throws Exception {
        // Predefined round types
        String[] robinRounds = {
                "League Matches", "Pre Quarter Final", "Quarter Final", "Semi Final", "Final",
                "Super League", "Super Eight", "Super Ten", "Super Six", "Super Four", "Super Three",
                "Qualifier 1", "Eliminator", "Qualifier 2", "Third Position", "Fourth Position",
                "Fifth Position", "Warm up Match", "Seven Position", "Nine Position", "Eleven Position",
                "Relegation Matches", "Super Division Matches",
                "1st Test", "2nd Test", "3rd Test", "4th Test", "5th Test",
                "Gold Final", "Silver Final", "Platinum Final"
        };

        String[] knockoutRounds = {
                "Super Knockout", "Round One", "Round Two", "Round Three", "Round Four", "Round Five",
                "Pre Quarter Final", "Quarter Final", "Semi Final", "Final",
                "Super League", "Super Six",
                "Third Position", "Fourth Position", "Fifth Position", "Warm up Match",
                "Seven Position", "Nine Position", "Eleven Position", "Deciding Match",
                "1st Test", "2nd Test", "3rd Test", "4th Test", "5th Test"
        };

        int tournamentId = 0;
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT tournament_id FROM tournament WHERE LOWER(tournament_name) = ?")) {
            ps.setString(1, tName.toLowerCase());
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tournamentId = rs.getInt("tournament_id");
            } else {
                System.out.println("Tournament not found!");
                return;
            }
        }

        String[] selectedSet = null;
        String roundType = "";

        // Choose Round Type with validation
        while (true) {
            try {
                System.out.println("""
                Select Round Type:
                1. Round Robin (League Matches)
                2. Knock Out
                0. Back
                """);
                System.out.print("Enter choice: ");
                int roundTypeChoice = sc.nextInt();
                sc.nextLine();

                if (roundTypeChoice == 0) return; // back
                else if (roundTypeChoice == 1) {
                    selectedSet = robinRounds;
                    roundType = "Round Robin";
                    break;
                } else if (roundTypeChoice == 2) {
                    selectedSet = knockoutRounds;
                    roundType = "KnockOut";
                    break;
                } else {
                    System.out.println("Invalid choice! Try again.");
                }
            } catch (InputMismatchException ime) {
                System.err.println("Invalid input, enter a number.");
                sc.nextLine();
            }
        }

        // Show available rounds
        System.out.println("\nAvailable Rounds:");
        for (int i = 0; i < selectedSet.length; i++) {
            System.out.println((i + 1) + ". " + selectedSet[i]);
        }

        int noOfRounds = 0;
        while (true) {
            try {
                System.out.print("Enter total number of rounds (max " + selectedSet.length + "): ");
                noOfRounds = sc.nextInt();
                sc.nextLine();
                if (noOfRounds > 0 && noOfRounds <= selectedSet.length) break;
                else System.out.println("Invalid number, try again.");
            } catch (InputMismatchException ime) {
                System.err.println("Invalid input, enter a number.");
                sc.nextLine();
            }
        }

        String[] roundsSelected = new String[noOfRounds];
        String insertSql = "INSERT INTO tournament_round (tournament_id, round_name, round_type, status) VALUES (?, ?, ?, 'scheduled')";

        // Round selection loop
        for (int j = 0; j < noOfRounds; j++) {
            int choice;
            while (true) {
                try {
                    System.out.print("Select round " + (j + 1) + ": ");
                    choice = sc.nextInt();
                    sc.nextLine();
                    if (choice >= 1 && choice <= selectedSet.length) break;
                    else System.out.println("Invalid selection, try again.");
                } catch (InputMismatchException ime) {
                    System.err.println("Invalid input, enter a number.");
                    sc.nextLine();
                    continue;
                }
            }
            roundsSelected[j] = selectedSet[choice - 1];

            try (PreparedStatement pst = con.prepareStatement(insertSql)) {
                pst.setInt(1, tournamentId);
                pst.setString(2, roundsSelected[j]);
                pst.setString(3, roundType);
                pst.executeUpdate();
            }
        }

        System.out.println("\n" + noOfRounds + " rounds added to the tournament.");
        for (int i = 0; i < roundsSelected.length; i++) {
            System.out.println((i + 1) + ". " + roundsSelected[i]);
        }

        int roundChoice;
        while (true) {
            try {
                System.out.print("Select round (0 = Back): ");
                roundChoice = sc.nextInt();
                sc.nextLine();
                if (roundChoice == 0) return; // back
                if (roundChoice >= 1 && roundChoice <= roundsSelected.length) break;
                else System.out.println("Invalid selection, try again.");
            } catch (InputMismatchException ime) {
                System.err.println("Invalid input, enter a number.");
                sc.nextLine();
            }
        }

        String roundSelected = roundsSelected[roundChoice - 1];

        // If KnockOut → mark as completed immediately
        if (roundType.equals("KnockOut")) {
            try (PreparedStatement pst = con.prepareStatement(
                    "UPDATE tournament_round SET status = 'completed' WHERE round_name = ? AND tournament_id = ?")) {
                pst.setString(1, roundSelected);
                pst.setInt(2, tournamentId);
                pst.executeUpdate();
            }
        }

        // Start matches for this tournament
        startTournamentMatch(tName);
    }

    boolean startTournamentMatch(String tName) throws Exception {
        Team teamA = null;
        Team teamB = null;
        int tournamentId = 0;

        // ✅ Secure query
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT tournament_id FROM tournament WHERE tournament_name = ?")) {
            ps.setString(1, tName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                tournamentId = rs.getInt("tournament_id");
            }
        }

        // 🔄 Match Setup Loop
        while (teamA == null || teamB == null) {
            System.out.println("\n--- Match Setup ---");
            System.out.println("Team A: " + (teamA != null ? teamA.getTeamName() : "Not Selected"));
            System.out.println("Team B: " + (teamB != null ? teamB.getTeamName() : "Not Selected"));
            System.out.println("""
                            1.) Select/Add Team A
                            2.) Select/Add Team B
                            3.) Back to previous page
                            Enter choice: """);

            int choice = -1;
            while (true) {
                try {
                    choice = sc.nextInt();
                    sc.nextLine(); // consume newline
                    // ✅ validate menu choice range here manually
                    break;
                } catch (InputMismatchException e) {
                    System.out.println("⚠ Invalid input. Please enter a number.");
                    sc.nextLine(); // clear buffer
                }
            }
            sc.nextLine(); // consume newline

            switch (choice) {
                case 1 -> teamA = selectOrAddTeamForTournament(teamB, "A", tName);
                case 2 -> teamB = selectOrAddTeamForTournament(teamA, "B", tName);
                case 3 -> { return false; } // Exit to previous page
                default -> System.out.println("Invalid choice.");
            }
        }

        // ✅ Both teams ready
        System.out.println("\nMatch Ready: " + teamA.getTeamName() + " vs " + teamB.getTeamName());

        // Enter squads
        System.out.println("Enter Squad of team: " + teamA.getTeamName());
        teamA.setTeamDetails();
        teamA.setCaptainDetails();
        System.out.println("Enter Squad of team: " + teamB.getTeamName());
        teamB.setTeamDetails();
        teamB.setCaptainDetails();

        // 🔄 Match validation loop
        while (true) {
            if (teamA.totalPlayers != teamB.totalPlayers) {
                System.out.println(teamA.getTeamName() + " and " + teamB.getTeamName() +
                        " squads have different sizes. Continue?");
                System.out.println("1. Yes, I'm sure\n2. Update teams");
                int ch = -1;
                while (true) {
                    try {
                        ch = sc.nextInt();
                        sc.nextLine(); // consume newline
                        // ✅ validate menu choice range here manually
                        break;
                    } catch (InputMismatchException e) {
                        System.out.println("⚠ Invalid input. Please enter a number.");
                        sc.nextLine(); // clear buffer
                    }
                }
                sc.nextLine();

                if (ch == 1) {
                    playMatch(teamA, teamB, tournamentId);
                    break;
                } else if (ch == 2) {
                    System.out.println("Update \n1. " + teamA.getTeamName() +
                            "\n2. " + teamB.getTeamName() +
                            "\n(press any other key) Skip update");

                    int updateChoice = -1;
                    while (true) {
                        try {
                            updateChoice = sc.nextInt();
                            sc.nextLine(); // consume newline
                            // ✅ validate menu choice range here manually
                            break;
                        } catch (InputMismatchException e) {
                            System.out.println("⚠ Invalid input. Please enter a number.");
                            sc.nextLine(); // clear buffer
                        }
                    }
                    sc.nextLine();

                    if (updateChoice == 1) {
                        teamA.updateTeamDetails(teamB);
                    } else if (updateChoice == 2) {
                        teamB.updateTeamDetails(teamA);
                    } else {
                        System.out.println("Skipping team update.");
                    }

                    if (teamA.totalPlayers == teamB.totalPlayers) {
                        System.out.println("Now both teams have equal squad sizes ✅");
                    }
                } else {
                    System.out.println("Invalid choice.");
                }
            } else {
                playMatch(teamA, teamB, tournamentId);
                break;
            }
        }

        return true;
    }

    private void playMatch(Team teamA, Team teamB, int tournamentId) throws Exception {
        Match m = new Match(teamA, teamB, con);
        m.matchTypeDetails();
        System.out.println("Toss....");
        m.toss();
        m.playMatch(tournamentId);
        System.out.println("Results");
        m.results(tournamentId, teamA, teamB);
    }


    void addTeamsForTournament(int tournamentId) throws SQLException {
        String sqlTeamsInTournament = """
        SELECT team_name 
        FROM team 
        WHERE team_id IN (SELECT team_id FROM tournament_teams WHERE tournament_id = ?)
    """;

        boolean atLeastOneTeam = false;

        try (PreparedStatement pst = con.prepareStatement(sqlTeamsInTournament)) {
            pst.setInt(1, tournamentId);
            ResultSet teams = pst.executeQuery();

            while (teams.next()) {
                atLeastOneTeam = true;
                System.out.println(teams.getString("team_name"));
            }
        }

        if (!atLeastOneTeam) {
            System.out.println("No teams added!");
            System.out.print("How many teams are you expecting for this tournament? ");
            int noOfTeams = -1;
            while (true) {
                try {
                    noOfTeams = sc.nextInt();
                    sc.nextLine(); // consume newline
                    // ✅ validate menu choice range here manually
                    break;
                } catch (InputMismatchException e) {
                    System.out.println("⚠ Invalid input. Please enter a number.");
                    sc.nextLine(); // clear buffer
                }
            }
            sc.nextLine(); // consume newline

            Set<String> selectedTeams = new HashSet<>(); // store already added team names

            for (int i = 0; i < noOfTeams; i++) {
                System.out.println("\nAdd/Select team " + (i + 1));
                System.out.println("1. Select Existing Team");
                System.out.println("2. Add New Team");
                System.out.print("Enter choice: ");

                int choice = -1;
                while (true) {
                    try {
                        choice = sc.nextInt();
                        sc.nextLine(); // consume newline
                        // ✅ validate menu choice range here manually
                        break;
                    } catch (InputMismatchException e) {
                        System.out.println("⚠ Invalid input. Please enter a number.");
                        sc.nextLine(); // clear buffer
                    }
                }
                sc.nextLine(); // consume newline

                if (choice == 1) {
                    selectExistingTeamForTournament(tournamentId, selectedTeams, i);
                } else if (choice == 2) {
                    addNewTeamForTournament(tournamentId, selectedTeams, i);
                } else {
                    System.out.println("Invalid choice. Try again.");
                    i--; // retry iteration
                }
            }
        }
    }

    private void selectExistingTeamForTournament(int tournamentId, Set<String> selectedTeams, int i) throws SQLException {
        System.out.println("\nAvailable Teams:");
        int teamCount = 0;
        try (Statement stm = con.createStatement();
             ResultSet rs = stm.executeQuery("SELECT team_name FROM Team")) {
            while (rs.next()) {
                System.out.println("- " + rs.getString("team_name"));
                teamCount++;
            }
        }

        if (teamCount == 0) {
            System.out.println("No teams found. Please add a team first.");
            return; // exit early
        }

        System.out.print("Enter the name of the team to select: ");
        String teamSearched = sc.nextLine().trim();

        if (teamSearched.isEmpty()) {
            System.out.println("Team name cannot be empty.");
            return;
        }

        if (selectedTeams.contains(teamSearched.toLowerCase())) {
            System.out.println("Error: This team is already added to the tournament.");
            return;
        }

        String sql = "SELECT team_id, team_name FROM Team WHERE team_name = ?";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setString(1, teamSearched);
            ResultSet rs = pst.executeQuery();
            if (rs.next()) {
                int teamId = rs.getInt("team_id");
                insertTournamentTeam(tournamentId, teamId);
                selectedTeams.add(teamSearched.toLowerCase());
                System.out.println("Team '" + teamSearched + "' selected.");
            } else {
                System.out.println("Team not found.");
            }
        }
    }

    private void addNewTeamForTournament(int tournamentId, Set<String> selectedTeams, int i) throws SQLException {
        System.out.print("Enter New Team Name: ");
        String teamName = sc.nextLine().trim();

        if (teamName.isEmpty()) {
            System.out.println("Team name cannot be empty.");
            return;
        }

        if (selectedTeams.contains(teamName.toLowerCase())) {
            System.out.println("Error: This team is already added to the tournament.");
            return;
        }

        System.out.print("Enter city/town: ");
        String cityName = sc.nextLine().trim();

        if (cityName.isEmpty()) {
            System.out.println("City cannot be empty.");
            return;
        }

        String sql = "INSERT INTO Team (team_name, city, user_id) VALUES (?, ?, ?)";
        int newTeamId = -1;
        try (PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pst.setString(1, teamName);
            pst.setString(2, cityName);
            pst.setInt(3, organizerId);
            pst.executeUpdate();

            ResultSet keys = pst.getGeneratedKeys();
            if (keys.next()) {
                newTeamId = keys.getInt(1);
            }
        }

        if (newTeamId > 0) {
            insertTournamentTeam(tournamentId, newTeamId);
            selectedTeams.add(teamName.toLowerCase());
            System.out.println("Team '" + teamName + "' added and selected.");
        }
    }

    void insertTournamentTeam(int tournamentId, int teamId) throws SQLException {
        String sql = "INSERT INTO Tournament_Teams (tournament_id, team_id) VALUES (?,?)";
        try (PreparedStatement pst = con.prepareStatement(sql)) {
            pst.setInt(1, tournamentId);
            pst.setInt(2, teamId);
            pst.executeUpdate();
            System.out.println("inserted----------------------------------------------------");
        }
    }

}



//package organizer;
//
//import match.Match;
//import team.Team;
//
//import java.sql.*;
//import java.util.*;
//public class Organizer {
//    int organizerId;
//    Connection con;
//    Scanner sc;
//
//    // Constructor to properly initialize the class
//    public Organizer(int id, Connection con, Scanner sc) {
//        this.organizerId = id;
//        this.con = con;
//        this.sc = sc;
//    }
//
//    // Main method for organizer actions
//    public void org() throws Exception {
//        try {
//            System.out.println("""
//                1. Add a tournament/series
//                2. Start a match""");
//
//            int choice = -1;
//            try {
//                choice = sc.nextInt();
//            } catch (InputMismatchException ime) {
//                System.err.println("Invalid input. Please enter a number.");
//                sc.nextLine(); // clear buffer
//                return;
//            }
//
//            if (choice == 1) {
//                try {
//                    addTournament();
//                } catch (Exception e) {
//                    System.err.println("Error while adding tournament: " + e.getMessage());
//                    e.printStackTrace();
//                }
//            } else if (choice == 2) {
//                try (Statement st = con.createStatement();
//                     ResultSet rst = st.executeQuery("SELECT * FROM tournament;")) {
//
//                    boolean atLeastOneTournamentExists = false;
//                    while (rst.next()) { // safe loop
//                        atLeastOneTournamentExists = true;
//                        break; // only need to check existence
//                    }
//
//                    if (!atLeastOneTournamentExists) {
//                        try {
//                            startMatch();
//                        } catch (Exception e) {
//                            System.err.println("Error while starting match: " + e.getMessage());
//                            e.printStackTrace();
//                        }
//                    } else {
//                        System.out.println("""
//                            1. Tournament
//                            2. Individual""");
//
//                        int matchChoice = -1;
//                        try {
//                            matchChoice = sc.nextInt();
//                        } catch (InputMismatchException ime) {
//                            System.err.println("Invalid input. Please enter a number.");
//                            sc.nextLine(); // clear buffer
//                            return;
//                        }
//
//                        if (matchChoice == 1) {
//                            try {
//                                startTournament();
//                            } catch (Exception e) {
//                                System.err.println("Error while starting tournament: " + e.getMessage());
//                                e.printStackTrace();
//                            }
//                        } else if (matchChoice == 2) {
//                            try {
//                                startMatch();
//                            } catch (Exception e) {
//                                System.err.println("Error while starting individual match: " + e.getMessage());
//                                e.printStackTrace();
//                            }
//                        } else {
//                            System.out.println("Invalid choice.");
//                        }
//                    }
//                } catch (SQLException e) {
//                    System.err.println("SQL error while checking tournaments: " + e.getMessage());
//                    e.printStackTrace();
//                }
//            } else {
//                System.out.println("Invalid choice.");
//            }
//        } catch (Exception e) {
//            System.err.println("Unexpected error in org(): " + e.getMessage());
//            e.printStackTrace();
//        }
//    }
//
//
//    Team selectOrAddTeam(Team otherTeam, String teamLetter) throws Exception {
//        System.out.println("\n--- Select Team " + teamLetter + " ---");
//        System.out.println("1. Select Existing Team");
//        System.out.println("2. Add New Team");
//        System.out.print("Enter choice: ");
//
//        int choice = -1;
//        try {
//            choice = sc.nextInt();
//        } catch (InputMismatchException ime) {
//            System.err.println("Invalid input. Please enter a number (1 or 2).");
//            sc.nextLine(); // clear buffer
//            return null;
//        }
//
//        if (choice == 1) { // Select Existing Team
//            System.out.println("\nAvailable Teams:");
//            try (Statement st = con.createStatement();
//                 ResultSet rs = st.executeQuery("SELECT team_name FROM Team")) {
//                int teamCount = 0;
//                while (rs.next()) {
//                    System.out.println("- " + rs.getString("team_name"));
//                    teamCount++;
//                }
//                if (teamCount == 0) {
//                    System.out.println("No teams found. Please add a team first.");
//                    return null;
//                }
//            } catch (SQLException e) {
//                System.err.println("SQL error while fetching teams: " + e.getMessage());
//                e.printStackTrace();
//                return null;
//            }
//
//            sc.nextLine(); // Consume newline
//            System.out.print("Enter the name of the team to select: ");
//            String teamSearched = sc.nextLine();
//
//            if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamSearched)) {
//                System.out.println("Error: Cannot select the same team for both A and B.");
//                return null;
//            }
//
//            String sql = "SELECT team_name, city FROM Team WHERE team_name = ?";
//            try (PreparedStatement pst = con.prepareStatement(sql)) {
//                pst.setString(1, teamSearched);
//                ResultSet rs = pst.executeQuery();
//
//                while (rs.next()) { // safe in case multiple rows match
//                    System.out.println("Team '" + teamSearched + "' selected.");
//                    return new Team(
//                            rs.getString("team_name"),
//                            rs.getString("city")
//                    );
//                }
//                System.out.println("Team not found.");
//                return null;
//            } catch (SQLException e) {
//                System.err.println("SQL error while selecting team: " + e.getMessage());
//                e.printStackTrace();
//                return null;
//            }
//
//        } else if (choice == 2) { // Add New Team
//            sc.nextLine(); // Consume newline
//            System.out.print("Enter New Team Name: ");
//            String teamName = sc.nextLine();
//
//            if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamName)) {
//                System.out.println("Error: A team with this name is already selected.");
//                return null;
//            }
//
//            System.out.print("Enter city/town: ");
//            String cityName = sc.nextLine();
////            System.out.print("Enter Team Captain's Name: ");
////            String teamCaptainName = sc.nextLine();
////            System.out.print("Enter Team Captain's Number: ");
////            String teamCaptainNumber = sc.nextLine();
//
//            String sql = "INSERT INTO Team (team_name, city, user_id) VALUES (?, ?, ?)";
//            try (PreparedStatement pst = con.prepareStatement(sql)) {
//                pst.setString(1, teamName);
//                pst.setString(2, cityName);
//                pst.setInt(3, organizerId);
//
//                int inserted = pst.executeUpdate();
//                if (inserted > 0) {
//                    System.out.println("Team '" + teamName + "' added and selected.");
//                    return new Team(teamName, cityName);
//                } else {
//                    System.out.println("Failed to add team. Please try again.");
//                    return null;
//                }
//            } catch (SQLException e) {
//                System.err.println("SQL error while inserting new team: " + e.getMessage());
//                e.printStackTrace();
//                return null;
//            }
//
//        } else {
//            System.out.println("Invalid choice.");
//        }
//
//        return null;
//    }
//
//
//    Team selectOrAddTeamForTournament(Team otherTeam, String teamLetter, String tName) throws Exception {
//        System.out.println("\n--- Select Team " + teamLetter + " ---");
//        System.out.println("1. Tournament Teams");
//        System.out.println("2. My Teams");
//        System.out.println("3. Add New Team");
//        System.out.print("Enter choice: ");
//
//        int choice = -1;
//        try {
//            choice = sc.nextInt();
//        } catch (InputMismatchException ime) {
//            System.err.println("Invalid input. Please enter a number (1, 2, or 3).");
//            sc.nextLine(); // clear buffer
//            return null;
//        }
//
//        if (choice == 1) { // Tournament Teams
//            System.out.println("\nAvailable Teams:");
//            String query = "SELECT DISTINCT team_name, tournament_name " +
//                    "FROM team " +
//                    "JOIN tournament_teams ON team.team_id = tournament_teams.team_id " +
//                    "JOIN tournament ON tournament_teams.tournament_id = tournament.tournament_id " +
//                    "WHERE tournament_name = ?";
//            try (PreparedStatement pst = con.prepareStatement(query)) {
//                pst.setString(1, tName);
//                ResultSet rs = pst.executeQuery();
//
//                int teamCount = 0;
//                while (rs.next()) {
//                    System.out.println("- " + rs.getString("team_name"));
//                    teamCount++;
//                }
//                if (teamCount == 0) {
//                    System.out.println("No teams found for tournament " + tName + ". Please add a team first.");
//                    return null;
//                }
//            } catch (SQLException e) {
//                System.err.println("SQL error while fetching tournament teams: " + e.getMessage());
//                e.printStackTrace();
//                return null;
//            }
//
//            sc.nextLine(); // consume newline
//            System.out.print("Enter the name of the team to select: ");
//            String teamSearched = sc.nextLine();
//
//            if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamSearched)) {
//                System.out.println("Error: Cannot select the same team for both A and B.");
//                return null;
//            }
//
//            String sql = "SELECT team_name, city FROM Team WHERE team_name = ?";
//            try (PreparedStatement pst = con.prepareStatement(sql)) {
//                pst.setString(1, teamSearched);
//                ResultSet rs = pst.executeQuery();
//
//                while (rs.next()) { // safe loop
//                    System.out.println("Team '" + teamSearched + "' selected.");
//                    return new Team(
//                            rs.getString("team_name"),
//                            rs.getString("city")
//                    );
//                }
//                System.out.println("Team not found.");
//                return null;
//            } catch (SQLException e) {
//                System.err.println("SQL error while selecting tournament team: " + e.getMessage());
//                e.printStackTrace();
//                return null;
//            }
//
//        } else if (choice == 3) { // Add New Team
//            sc.nextLine(); // consume newline
//            System.out.print("Enter New Team Name: ");
//            String teamName = sc.nextLine();
//
//            if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamName)) {
//                System.out.println("Error: A team with this name is already selected.");
//                return null;
//            }
//
//            System.out.print("Enter city/town: ");
//            String cityName = sc.nextLine();
//
//            String sql = "INSERT INTO Team (team_name, city, user_id) VALUES (?, ?, ?)";
//            try (PreparedStatement pst = con.prepareStatement(sql)) {
//                pst.setString(1, teamName);
//                pst.setString(2, cityName);
//                pst.setInt(3, organizerId);
//
//                int inserted = pst.executeUpdate();
//                if (inserted > 0) {
//                    System.out.println("Team '" + teamName + "' added and selected.");
//                    return new Team(teamName, cityName);
//                } else {
//                    System.out.println("Failed to add team. Please try again.");
//                    return null;
//                }
//            } catch (SQLException e) {
//                System.err.println("SQL error while inserting new team: " + e.getMessage());
//                e.printStackTrace();
//                return null;
//            }
//
//        } else if (choice == 2) { // My Teams
//            System.out.println("\nAvailable Teams:");
//            try (Statement st = con.createStatement();
//                 ResultSet rs = st.executeQuery("SELECT team_name FROM team")) {
//                int teamCount = 0;
//                while (rs.next()) {
//                    System.out.println("- " + rs.getString("team_name"));
//                    teamCount++;
//                }
//                if (teamCount == 0) {
//                    System.out.println("No teams found. Please add a team first.");
//                    return null;
//                }
//            } catch (SQLException e) {
//                System.err.println("SQL error while fetching my teams: " + e.getMessage());
//                e.printStackTrace();
//                return null;
//            }
//
//            sc.nextLine(); // consume newline
//            System.out.print("Enter the name of the team to select: ");
//            String teamSearched = sc.nextLine();
//
//            if (otherTeam != null && otherTeam.teamName.equalsIgnoreCase(teamSearched)) {
//                System.out.println("Error: Cannot select the same team for both A and B.");
//                return null;
//            }
//
//            String sql = "SELECT team_name, city FROM Team WHERE team_name = ?";
//            try (PreparedStatement pst = con.prepareStatement(sql)) {
//                pst.setString(1, teamSearched);
//                ResultSet rs = pst.executeQuery();
//
//                while (rs.next()) {
//                    System.out.println("Team '" + teamSearched + "' selected.");
//                    return new Team(
//                            rs.getString("team_name"),
//                            rs.getString("city")
//                    );
//                }
//                System.out.println("Team not found.");
//                return null;
//            } catch (SQLException e) {
//                System.err.println("SQL error while selecting my team: " + e.getMessage());
//                e.printStackTrace();
//                return null;
//            }
//        } else {
//            System.out.println("Invalid choice.");
//        }
//
//        return null; // fallback
//    }
//
//
//    public void startMatch() throws Exception {
//        Team teamA = null;
//        Team teamB = null;
//
//        while (teamA == null || teamB == null) {
//            System.out.println("\n--- Match Setup ---");
//            System.out.println("Team A: " + (teamA != null ? teamA.getTeamName() : "Not Selected"));
//            System.out.println("Team B: " + (teamB != null ? teamB.getTeamName() : "Not Selected"));
//            System.out.println("""
//                            1.) Select/Add Team A
//                            2.) Select/Add Team B
//                            3.) Back to previous page
//                            Enter choice: """);
//
//            int ch3 = -1;
//            try {
//                ch3 = sc.nextInt();
//            } catch (InputMismatchException ime) {
//                System.err.println("Invalid input. Please enter 1, 2, or 3.");
//                sc.nextLine(); // clear invalid input
//                continue; // restart loop
//            }
//
//            switch (ch3) {
//                case 1 -> teamA = selectOrAddTeam(teamB, "A");
//                case 2 -> teamB = selectOrAddTeam(teamA, "B");
//                case 3 -> {
//                    return; // Exit the match setup
//                }
//                default -> System.out.println("Invalid choice. Please try again.");
//            }
//        }
//
//        System.out.println("\nMatch Ready: " + teamA.teamName + " vs " + teamB.teamName);
//
//        System.out.println("Enter Squad of team: " + teamA.teamName);
//        teamA.setTeamDetails();
//        teamA.setCaptainDetails();
//        System.out.println("Enter Squad of team: " + teamB.teamName);
//        teamB.setTeamDetails();
//        teamB.setCaptainDetails();
//
//        while (true) {
//            if (teamA.totalPlayers != teamB.totalPlayers) {
//                System.out.println(teamA.teamName + " and " + teamB.teamName +
//                        " squads are not the same size. Are you sure you want to continue?\n" +
//                        "1. Yes, I'm sure\n" +
//                        "2. Update teams");
//                int ch = -1;
//                try {
//                    ch = sc.nextInt();
//                } catch (InputMismatchException ime) {
//                    System.err.println("Invalid input. Please enter 1 or 2.");
//                    sc.nextLine();
//                    continue;
//                }
//
//                if (ch == 1) {
//                    Match m = new Match(teamA, teamB, con);
//                    m.matchTypeDetails();
//                    System.out.println("Toss....");
//                    m.toss();
//                    m.playMatch();
//                    System.out.println("Results");
//                    m.results();
//                    break;
//                } else if (ch == 2) {
//                    System.out.println("Update \n1. " + teamA.teamName +
//                            "\n2. " + teamB.teamName +
//                            "\n(press any other key) Skip update");
//
//                    int ch1 = -1;
//                    try {
//                        ch1 = sc.nextInt();
//                    } catch (InputMismatchException ime) {
//                        System.err.println("Invalid input. Skipping update.");
//                        sc.nextLine();
//                    }
//                    sc.nextLine(); // clear buffer
//
//                    if (ch1 == 1) {
//                        teamA.updateTeamDetails(teamB);
//                    } else if (ch1 == 2) {
//                        teamB.updateTeamDetails(teamA);
//                    } else {
//                        System.out.println("Skipping team update.");
//                    }
//
//                    if (teamA.totalPlayers == teamB.totalPlayers) {
//                        System.out.println("Now both teams have the same squad size.\n\n");
//                    }
//
//                } else {
//                    System.out.println("Invalid choice. Try again.");
//                }
//
//            } else { // squads match
//                Match m = new Match(teamA, teamB, con);
//                m.matchTypeDetails();
//                System.out.println("Toss....");
//                m.toss();
//                m.playMatch();
//                System.out.println("Results");
//                m.results();
//                break;
//            }
//        }
//    }
//
//    void addTournament() throws Exception {
//        sc.nextLine(); // consume leftover newline
//
//        System.out.println("\n--- Add New Tournament ---");
//
//        // 1. Tournament Details
//        System.out.print("Enter tournament/series name: ");
//        String tName = sc.nextLine();
//
//        System.out.print("Enter city: ");
//        String city = sc.nextLine();
//
//        System.out.print("Enter ground name: ");
//        String ground = sc.nextLine();
//
//        System.out.print("Enter organizer's name: ");
//        String organizerName = sc.nextLine();
//
//        System.out.print("Enter organizer's phone no.: ");
//        String organizerNo = sc.nextLine();
//
//        System.out.print("Enter tournament start date (YYYY-MM-DD): ");
//        String startDate = sc.nextLine();
//
//        System.out.print("Enter tournament end date (YYYY-MM-DD): ");
//        String endDate = sc.nextLine();
//
//        // 2. Ball Type
//        String ballType = "Other";
//        while (true) {
//            try {
//                System.out.println("\nSelect Ball Type:");
//                System.out.println("1. Tennis\n2. Leather\n3. Other");
//                System.out.print("Enter choice: ");
//                int bTypeChoice = -1;
//                while (true) {
//                    try {
//                        bTypeChoice = sc.nextInt();
//                        sc.nextLine(); // consume newline
//                        // ✅ validate menu choice range here manually
//                        break;
//                    } catch (InputMismatchException e) {
//                        System.out.println("⚠ Invalid input. Please enter a number.");
//                        sc.nextLine(); // clear buffer
//                    }
//                }
//                ballType = switch (bTypeChoice) {
//                    case 1 -> "Tennis";
//                    case 2 -> "Leather";
//                    default -> "Other";
//                };
//                break;
//            } catch (InputMismatchException ime) {
//                System.err.println("Invalid input. Please enter 1, 2, or 3.");
//                sc.nextLine();
//            }
//        }
//
//        // 3. Pitch Type
//        String[] pType = {"Rough", "Cement", "Turf", "Astroturf", "Matting"};
//        String pitchType = "";
//        while (true) {
//            try {
//                System.out.println("\nSelect Pitch Type:");
//                for (int i = 0; i < pType.length; i++) {
//                    System.out.println((i + 1) + ". " + pType[i]);
//                }
//                System.out.print("Enter choice: ");
//                int pChoice = -1;
//                while (true) {
//                    try {
//                        pChoice = sc.nextInt();
//                        sc.nextLine(); // consume newline
//                        // ✅ validate menu choice range here manually
//                        break;
//                    } catch (InputMismatchException e) {
//                        System.out.println("⚠ Invalid input. Please enter a number.");
//                        sc.nextLine(); // clear buffer
//                    }
//                }
//                if (pChoice >= 1 && pChoice <= pType.length) {
//                    pitchType = pType[pChoice - 1];
//                    break;
//                } else {
//                    System.out.println("Invalid choice. Try again.");
//                }
//            } catch (InputMismatchException ime) {
//                System.err.println("Invalid input. Please enter a number.");
//                sc.nextLine();
//            }
//        }
//
//        // 4. Match Type
//        String[] mType = {"Limited Overs", "Box/Turf Cricket", "Pair Cricket", "Test Match", "The Hundred"};
//        String matchType = "";
//        while (true) {
//            try {
//                System.out.println("\nSelect Match Type:");
//                for (int i = 0; i < mType.length; i++) {
//                    System.out.println((i + 1) + ". " + mType[i]);
//                }
//                System.out.print("Enter choice: ");
//                int mChoice = -1;
//                while (true) {
//                    try {
//                        mChoice = sc.nextInt();
//                        sc.nextLine(); // consume newline
//                        // ✅ validate menu choice range here manually
//                        break;
//                    } catch (InputMismatchException e) {
//                        System.out.println("⚠ Invalid input. Please enter a number.");
//                        sc.nextLine(); // clear buffer
//                    }
//                }
//                if (mChoice >= 1 && mChoice <= mType.length) {
//                    matchType = mType[mChoice - 1];
//                    break;
//                } else {
//                    System.out.println("Invalid choice. Try again.");
//                }
//            } catch (InputMismatchException ime) {
//                System.err.println("Invalid input. Please enter a number.");
//                sc.nextLine();
//            }
//        }
//
//        // 5. Insert Tournament into DB
//        String sql = """
//             INSERT INTO Tournament
//             (tournament_name, city, ground, organizer_name, organizer_contact,
//              start_date, end_date, ball_type, pitch_type, match_type, organizer_id)
//             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
//             """;
//
//        try (PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//            pst.setString(1, tName);
//            pst.setString(2, city);
//            pst.setString(3, ground);
//            pst.setString(4, organizerName);
//            pst.setString(5, organizerNo);
//            pst.setString(6, startDate);
//            pst.setString(7, endDate);
//            pst.setString(8, ballType);
//            pst.setString(9, pitchType);
//            pst.setString(10, matchType);
//            pst.setInt(11, this.organizerId);
//
//            int rowsAffected = pst.executeUpdate();
//
//            if (rowsAffected > 0) {
//                System.out.println("Tournament '" + tName + "' has been added successfully!");
//
//                ResultSet rsKeys = pst.getGeneratedKeys();
//                int newTournamentId = -1;
//                if (rsKeys.next()) {
//                    newTournamentId = rsKeys.getInt(1);
//                }
//
//                Statement st = con.createStatement();
//                int tournamentChoice = 0;
//
//                do {
//                    try {
//                        System.out.println("""
//                            1. About
//                            2. Teams
//                            3. Matches
//                            4. Points Table
//                            5. Exit (back to previous page!)""");
//                        tournamentChoice = sc.nextInt();
//
//                        switch (tournamentChoice) {
//                            case 1 -> {
//                                String orgInfoQuery =
//                                        "SELECT organizer_name, city, COUNT(*) " +
//                                                "FROM tournament WHERE organizer_id=" + this.organizerId +
//                                                " GROUP BY organizer_id";
//                                ResultSet orgInfo = st.executeQuery(orgInfoQuery);
//                                System.out.println("Organizer Details");
//                                while (orgInfo.next()) {
//                                    System.out.println("Name : " + orgInfo.getString(1));
//                                    System.out.println("City : " + orgInfo.getString(2));
//                                    System.out.println("Tournaments Organized : " + orgInfo.getInt(3));
//                                }
//                                System.out.println("Tournament Details");
//                                String tournamentInfoQuery =
//                                        "SELECT tournament_name, start_date, end_date, ground, ball_type, tournament_id " +
//                                                "FROM tournament WHERE tournament_id=" + newTournamentId;
//                                ResultSet tournamentInfo = st.executeQuery(tournamentInfoQuery);
//                                while (tournamentInfo.next()) {
//                                    System.out.println("Name : " + tournamentInfo.getString(1));
//                                    System.out.println("Date : " + tournamentInfo.getString(2) + " to " + tournamentInfo.getString(3));
//                                    System.out.println("Ground : " + tournamentInfo.getString(4));
//                                    System.out.println("Ball Type : " + tournamentInfo.getString(5));
//                                    System.out.println("Tournament ID : " + tournamentInfo.getString(6));
//                                }
//                            }
//                            case 2 -> addTeamsForTournament(newTournamentId);
//                            case 3 -> {
//                                System.out.println("1. Schedule Match");
//                                System.out.println("2. Start Match");
//                                int matchChoice = -1;
//                                while (true) {
//                                    try {
//                                        matchChoice = sc.nextInt();
//                                        sc.nextLine(); // consume newline
//                                        // ✅ validate menu choice range here manually
//                                        break;
//                                    } catch (InputMismatchException e) {
//                                        System.out.println("⚠ Invalid input. Please enter a number.");
//                                        sc.nextLine(); // clear buffer
//                                    }
//                                }
//                                if (matchChoice == 1) {
//                                    addNewRound(tName);
//                                } else if (matchChoice == 2) {
//                                    startTournamentMatch(tName);
//                                }
//                            }
//                            case 4 -> {
//                                // points table logic here
//                            }
//                            case 5 -> System.out.println("Exiting tournament menu...");
//                            default -> System.out.println("Invalid choice. Try again.");
//                        }
//                    } catch (InputMismatchException ime) {
//                        System.err.println("Invalid input. Please enter a number.");
//                        sc.nextLine();
//                    }
//                } while (tournamentChoice != 5);
//
//            } else {
//                System.out.println("Error: Could not add the tournament.");
//            }
//        }
//    }
//
//
//    void startTournament() throws Exception {
//        int tournamentId = 0;
//        Statement st = con.createStatement();
//        ResultSet tournaments = st.executeQuery("SELECT tournament_id, tournament_name FROM tournament;");
//
//        // Store tournamentId → tournamentName
//        HashMap<Integer, String> tournamentsList = new HashMap<>();
//        while (tournaments.next()) {
//            System.out.println(tournaments.getString(2));
//            tournamentsList.put(tournaments.getInt(1), tournaments.getString(2).toLowerCase());
//        }
//
//        sc.nextLine(); // consume newline
//        ArrayList<String> rounds = new ArrayList<>();
//
//        while (true) {
//            System.out.print("Select tournament: ");
//            String tName = sc.nextLine().trim().toLowerCase();
//
//            if (tournamentsList.containsValue(tName)) {
//                // Find the selected tournamentId
//                for (Map.Entry<Integer, String> entry : tournamentsList.entrySet()) {
//                    if (entry.getValue().equals(tName)) {
//                        tournamentId = entry.getKey();
//                    }
//                }
//
//                // Fetch scheduled rounds
//                String sqlRounds = """
//                SELECT round_name
//                FROM tournament_round
//                WHERE tournament_id = (SELECT tournament_id FROM tournament WHERE LOWER(tournament_name) = ?)
//                  AND status = 'scheduled'
//                """;
//                try (PreparedStatement ps = con.prepareStatement(sqlRounds)) {
//                    ps.setString(1, tName);
//                    ResultSet r = ps.executeQuery();
//
//                    boolean hasRounds = false;
//                    while (r.next()) {
//                        hasRounds = true;
//                        rounds.add(r.getString("round_name"));
//                    }
//
//                    if (!hasRounds) {
//                        System.out.println("* You need to add Rounds and Groups to generate a Points Table.");
//                        addNewRound(tName);
//                    } else {
//                        System.out.println("\nAvailable Rounds:");
//                        for (int i = 0; i < rounds.size(); i++) {
//                            System.out.println((i + 1) + ". " + rounds.get(i));
//                        }
//
//                        System.out.print("Enter round number: ");
//                        int roundChoice = -1;
//                        while (true) {
//                            try {
//                                roundChoice = sc.nextInt();
//                                sc.nextLine(); // consume newline
//                                // ✅ validate menu choice range here manually
//                                break;
//                            } catch (InputMismatchException e) {
//                                System.out.println("⚠ Invalid input. Please enter a number.");
//                                sc.nextLine(); // clear buffer
//                            }
//                        }
//                        sc.nextLine(); // consume newline
//                        String roundSelected = rounds.get(roundChoice - 1);
//
//                        // Start the selected tournament match
//                        startTournamentMatch(tName);
//
//                        // Mark round as completed
//                        String updateSql = "UPDATE tournament_round SET status='completed' WHERE round_name=? AND tournament_id=?";
//                        try (PreparedStatement pst = con.prepareStatement(updateSql)) {
//                            pst.setString(1, roundSelected);
//                            pst.setInt(2, tournamentId);
//                            pst.executeUpdate();
//                            rounds.remove(roundSelected);
//                        }
//                    }
//                }
//                break; // Exit main loop after handling tournament
//            } else {
//                System.out.println("Invalid tournament name, please try again.");
//            }
//        }
//    }
//
//
//    void addNewRound(String tName) throws Exception {
//        // Predefined round types
//        String[] robinRounds = {
//                "League Matches", "Pre Quarter Final", "Quarter Final", "Semi Final", "Final",
//                "Super League", "Super Eight", "Super Ten", "Super Six", "Super Four", "Super Three",
//                "Qualifier 1", "Eliminator", "Qualifier 2", "Third Position", "Fourth Position",
//                "Fifth Position", "Warm up Match", "Seven Position", "Nine Position", "Eleven Position",
//                "Relegation Matches", "Super Division Matches",
//                "1st Test", "2nd Test", "3rd Test", "4th Test", "5th Test",
//                "Gold Final", "Silver Final", "Platinum Final"
//        };
//
//        String[] knockoutRounds = {
//                "Super Knockout", "Round One", "Round Two", "Round Three", "Round Four", "Round Five",
//                "Pre Quarter Final", "Quarter Final", "Semi Final", "Final",
//                "Super League", "Super Six",
//                "Third Position", "Fourth Position", "Fifth Position", "Warm up Match",
//                "Seven Position", "Nine Position", "Eleven Position", "Deciding Match",
//                "1st Test", "2nd Test", "3rd Test", "4th Test", "5th Test"
//        };
//
//        int tournamentId = 0;
//        try (PreparedStatement ps = con.prepareStatement(
//                "SELECT tournament_id FROM tournament WHERE tournament_name = ?")) {
//            ps.setString(1, tName);
//            ResultSet rs = ps.executeQuery();
//            if (rs.next()) {
//                tournamentId = rs.getInt("tournament_id");
//            }
//        }
//
//        System.out.println("""
//            Select Round Type:
//            1. Round Robin (League Matches)
//            2. Knock Out
//            """);
//        System.out.print("Enter choice: ");
//        int roundTypeChoice = -1;
//        while (true) {
//            try {
//                roundTypeChoice = sc.nextInt();
//                sc.nextLine(); // consume newline
//                // ✅ validate menu choice range here manually
//                break;
//            } catch (InputMismatchException e) {
//                System.out.println("⚠ Invalid input. Please enter a number.");
//                sc.nextLine(); // clear buffer
//            }
//        }
//        sc.nextLine(); // consume newline
//
//        String[] selectedSet;
//        String roundType;
//
//        if (roundTypeChoice == 1) {
//            selectedSet = robinRounds;
//            roundType = "Round Robin";
//        } else if (roundTypeChoice == 2) {
//            selectedSet = knockoutRounds;
//            roundType = "KnockOut";
//        } else {
//            System.out.println("Invalid choice!");
//            return;
//        }
//
//        // Show available rounds
//        for (int i = 0; i < selectedSet.length; i++) {
//            System.out.println((i + 1) + ". " + selectedSet[i]);
//        }
//
//        System.out.print("Enter total number of rounds you want to keep in your tournament: ");
//        int noOfRounds = -1;
//        while (true) {
//            try {
//                noOfRounds = sc.nextInt();
//                sc.nextLine(); // consume newline
//                // ✅ validate menu choice range here manually
//                break;
//            } catch (InputMismatchException e) {
//                System.out.println("⚠ Invalid input. Please enter a number.");
//                sc.nextLine(); // clear buffer
//            }
//        }
//        sc.nextLine();
//
//        String[] roundsSelected = new String[noOfRounds];
//        String insertSql = "INSERT INTO tournament_round (tournament_id, round_name, round_type) VALUES (?, ?, ?)";
//
//        for (int j = 0; j < noOfRounds; j++) {
//            int choice;
//            while (true) {
//                System.out.print("Select round " + (j + 1) + " from the above list: ");
//                choice = sc.nextInt();
//                sc.nextLine();
//                if (choice >= 1 && choice <= selectedSet.length) break;
//                System.out.println("Invalid selection, try again.");
//            }
//            roundsSelected[j] = selectedSet[choice - 1];
//
//            try (PreparedStatement pst = con.prepareStatement(insertSql)) {
//                pst.setInt(1, tournamentId);
//                pst.setString(2, roundsSelected[j]);
//                pst.setString(3, roundType);
//                pst.executeUpdate();
//            }
//        }
//
//        System.out.println("\n" + noOfRounds + " rounds added to the tournament.");
//        for (int i = 0; i < roundsSelected.length; i++) {
//            System.out.println((i + 1) + ". " + roundsSelected[i]);
//        }
//
//        // Select a round
//        System.out.print("Select round: ");
//        int roundChoice = -1;
//        while (true) {
//            try {
//                roundChoice = sc.nextInt();
//                sc.nextLine(); // consume newline
//                // ✅ validate menu choice range here manually
//                break;
//            } catch (InputMismatchException e) {
//                System.out.println("⚠ Invalid input. Please enter a number.");
//                sc.nextLine(); // clear buffer
//            }
//        }
//        sc.nextLine();
//
//        if (roundChoice < 1 || roundChoice > roundsSelected.length) {
//            System.out.println("Invalid round selection.");
//            return;
//        }
//        String roundSelected = roundsSelected[roundChoice - 1];
//
//        // If KnockOut → mark as completed immediately
//        if (roundType.equals("KnockOut")) {
//            try (PreparedStatement pst = con.prepareStatement(
//                    "UPDATE tournament_round SET status = 'completed' WHERE round_name = ? AND tournament_id = ?")) {
//                pst.setString(1, roundSelected);
//                pst.setInt(2, tournamentId);
//                pst.executeUpdate();
//            }
//        }
//
//        // Start matches for this tournament
//        startTournamentMatch(tName);
//    }
//
//    boolean startTournamentMatch(String tName) throws Exception {
//        Team teamA = null;
//        Team teamB = null;
//        int tournamentId = 0;
//
//        // ✅ Secure query
//        try (PreparedStatement ps = con.prepareStatement(
//                "SELECT tournament_id FROM tournament WHERE tournament_name = ?")) {
//            ps.setString(1, tName);
//            ResultSet rs = ps.executeQuery();
//            if (rs.next()) {
//                tournamentId = rs.getInt("tournament_id");
//            }
//        }
//
//        // 🔄 Match Setup Loop
//        while (teamA == null || teamB == null) {
//            System.out.println("\n--- Match Setup ---");
//            System.out.println("Team A: " + (teamA != null ? teamA.getTeamName() : "Not Selected"));
//            System.out.println("Team B: " + (teamB != null ? teamB.getTeamName() : "Not Selected"));
//            System.out.println("""
//                            1.) Select/Add Team A
//                            2.) Select/Add Team B
//                            3.) Back to previous page
//                            Enter choice: """);
//
//            int choice = -1;
//            while (true) {
//                try {
//                    choice = sc.nextInt();
//                    sc.nextLine(); // consume newline
//                    // ✅ validate menu choice range here manually
//                    break;
//                } catch (InputMismatchException e) {
//                    System.out.println("⚠ Invalid input. Please enter a number.");
//                    sc.nextLine(); // clear buffer
//                }
//            }
//            sc.nextLine(); // consume newline
//
//            switch (choice) {
//                case 1 -> teamA = selectOrAddTeamForTournament(teamB, "A", tName);
//                case 2 -> teamB = selectOrAddTeamForTournament(teamA, "B", tName);
//                case 3 -> { return false; } // Exit to previous page
//                default -> System.out.println("Invalid choice.");
//            }
//        }
//
//        // ✅ Both teams ready
//        System.out.println("\nMatch Ready: " + teamA.getTeamName() + " vs " + teamB.getTeamName());
//
//        // Enter squads
//        System.out.println("Enter Squad of team: " + teamA.getTeamName());
//        teamA.setTeamDetails();
//        System.out.println("Enter Squad of team: " + teamB.getTeamName());
//        teamB.setTeamDetails();
//
//        // 🔄 Match validation loop
//        while (true) {
//            if (teamA.totalPlayers != teamB.totalPlayers) {
//                System.out.println(teamA.getTeamName() + " and " + teamB.getTeamName() +
//                        " squads have different sizes. Continue?");
//                System.out.println("1. Yes, I'm sure\n2. Update teams");
//                int ch = -1;
//                while (true) {
//                    try {
//                        ch = sc.nextInt();
//                        sc.nextLine(); // consume newline
//                        // ✅ validate menu choice range here manually
//                        break;
//                    } catch (InputMismatchException e) {
//                        System.out.println("⚠ Invalid input. Please enter a number.");
//                        sc.nextLine(); // clear buffer
//                    }
//                }
//                sc.nextLine();
//
//                if (ch == 1) {
//                    playMatch(teamA, teamB, tournamentId);
//                    break;
//                } else if (ch == 2) {
//                    System.out.println("Update \n1. " + teamA.getTeamName() +
//                            "\n2. " + teamB.getTeamName() +
//                            "\n(press any other key) Skip update");
//
//                    int updateChoice = -1;
//                    while (true) {
//                        try {
//                            updateChoice = sc.nextInt();
//                            sc.nextLine(); // consume newline
//                            // ✅ validate menu choice range here manually
//                            break;
//                        } catch (InputMismatchException e) {
//                            System.out.println("⚠ Invalid input. Please enter a number.");
//                            sc.nextLine(); // clear buffer
//                        }
//                    }
//                    sc.nextLine();
//
//                    if (updateChoice == 1) {
//                        teamA.updateTeamDetails(teamB);
//                    } else if (updateChoice == 2) {
//                        teamB.updateTeamDetails(teamA);
//                    } else {
//                        System.out.println("Skipping team update.");
//                    }
//
//                    if (teamA.totalPlayers == teamB.totalPlayers) {
//                        System.out.println("Now both teams have equal squad sizes ✅");
//                    }
//                } else {
//                    System.out.println("Invalid choice.");
//                }
//            } else {
//                playMatch(teamA, teamB, tournamentId);
//                break;
//            }
//        }
//
//        return true;
//    }
//
//    private void playMatch(Team teamA, Team teamB, int tournamentId) throws Exception {
//        Match m = new Match(teamA, teamB, con);
//        m.matchTypeDetails();
//        System.out.println("Toss....");
//        m.toss();
//        m.playMatch();
//        System.out.println("Results");
//        m.results(tournamentId, teamA, teamB);
//    }
//
//
//    void addTeamsForTournament(int tournamentId) throws SQLException {
//        String sqlTeamsInTournament = """
//        SELECT team_name
//        FROM team
//        WHERE team_id IN (SELECT team_id FROM tournament_teams WHERE tournament_id = ?)
//    """;
//
//        boolean atLeastOneTeam = false;
//
//        try (PreparedStatement pst = con.prepareStatement(sqlTeamsInTournament)) {
//            pst.setInt(1, tournamentId);
//            ResultSet teams = pst.executeQuery();
//
//            while (teams.next()) {
//                atLeastOneTeam = true;
//                System.out.println(teams.getString("team_name"));
//            }
//        }
//
//        if (!atLeastOneTeam) {
//            System.out.println("No teams added!");
//            System.out.print("How many teams are you expecting for this tournament? ");
//            int noOfTeams = -1;
//            while (true) {
//                try {
//                    noOfTeams = sc.nextInt();
//                    sc.nextLine(); // consume newline
//                    // ✅ validate menu choice range here manually
//                    break;
//                } catch (InputMismatchException e) {
//                    System.out.println("⚠ Invalid input. Please enter a number.");
//                    sc.nextLine(); // clear buffer
//                }
//            }
//            sc.nextLine(); // consume newline
//
//            Set<String> selectedTeams = new HashSet<>(); // store already added team names
//
//            for (int i = 0; i < noOfTeams; i++) {
//                System.out.println("\nAdd/Select team " + (i + 1));
//                System.out.println("1. Select Existing Team");
//                System.out.println("2. Add New Team");
//                System.out.print("Enter choice: ");
//
//                int choice = -1;
//                while (true) {
//                    try {
//                        choice = sc.nextInt();
//                        sc.nextLine(); // consume newline
//                        // ✅ validate menu choice range here manually
//                        break;
//                    } catch (InputMismatchException e) {
//                        System.out.println("⚠ Invalid input. Please enter a number.");
//                        sc.nextLine(); // clear buffer
//                    }
//                }
//                sc.nextLine(); // consume newline
//
//                if (choice == 1) {
//                    selectExistingTeamForTournament(tournamentId, selectedTeams, i);
//                } else if (choice == 2) {
//                    addNewTeamForTournament(tournamentId, selectedTeams, i);
//                } else {
//                    System.out.println("Invalid choice. Try again.");
//                    i--; // retry iteration
//                }
//            }
//        }
//    }
//
//    private void selectExistingTeamForTournament(int tournamentId, Set<String> selectedTeams, int i) throws SQLException {
//        System.out.println("\nAvailable Teams:");
//        try (Statement stm = con.createStatement();
//             ResultSet rs = stm.executeQuery("SELECT team_name FROM Team")) {
//            int teamCount = 0;
//            while (rs.next()) {
//                System.out.println("- " + rs.getString("team_name"));
//                teamCount++;
//            }
//            if (teamCount == 0) {
//                System.out.println("No teams found. Please add a team first.");
//                return; // exit early
//            }
//        }
//
//        System.out.print("Enter the name of the team to select: ");
//        String teamSearched = sc.nextLine().trim();
//
//        if (teamSearched.isEmpty()) {
//            System.out.println("Team name cannot be empty.");
//            return;
//        }
//
//        // Prevent duplicates
//        if (selectedTeams.contains(teamSearched.toLowerCase())) {
//            System.out.println("Error: This team is already added to the tournament.");
//            return;
//        }
//
//        String sql = "SELECT team_id, team_name FROM Team WHERE team_name = ?";
//        try (PreparedStatement pst = con.prepareStatement(sql)) {
//            pst.setString(1, teamSearched);
//            ResultSet rs = pst.executeQuery();
//            if (rs.next()) {
//                int teamId = rs.getInt("team_id");
//                insertTournamentTeam(tournamentId, teamId);
//                selectedTeams.add(teamSearched.toLowerCase());
//                System.out.println("Team '" + teamSearched + "' selected.");
//            } else {
//                System.out.println("Team not found.");
//            }
//        }
//    }
//    private void addNewTeamForTournament(int tournamentId, Set<String> selectedTeams, int i) throws SQLException {
//        System.out.print("Enter New Team Name: ");
//        String teamName = sc.nextLine().trim();
//
//        if (teamName.isEmpty()) {
//            System.out.println("Team name cannot be empty.");
//            return;
//        }
//
//        if (selectedTeams.contains(teamName.toLowerCase())) {
//            System.out.println("Error: This team is already added to the tournament.");
//            return;
//        }
//
//        System.out.print("Enter city/town: ");
//        String cityName = sc.nextLine();
//
//        String sql = "INSERT INTO Team (team_name, city, user_id) VALUES (?, ?, ?)";
//        int newTeamId = -1;
//        try (PreparedStatement pst = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
//            pst.setString(1, teamName);
//            pst.setString(2, cityName);
//            pst.setInt(3, organizerId);
//            pst.executeUpdate();
//
//            ResultSet keys = pst.getGeneratedKeys();
//            if (keys.next()) {
//                newTeamId = keys.getInt(1);
//            }
//        }
//
//        if (newTeamId > 0) {
//            insertTournamentTeam(tournamentId, newTeamId);
//            selectedTeams.add(teamName.toLowerCase());
//            System.out.println("Team '" + teamName + "' added and selected.");
//        }
//    }
//
//
//    void insertTournamentTeam(int tournamentId, int teamId) throws SQLException {
//        String sql = "INSERT INTO Tournament_Teams (tournament_id, team_id) VALUES (?, ?)";
//        PreparedStatement pst = con.prepareStatement(sql);
//        pst.setInt(1, tournamentId);
//        pst.setInt(2, teamId);
//        pst.executeUpdate();
//    }
//}
//
