package scoreCard;

import stats.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;
import team.Team;
import commentatry.Commentary;
public class Score {
    public double oversBowled;
    int matchId;
    Team battingTeam;
    Team bowlingTeam;
    Connection con;
    int overs;
    int overs_per_bowler;
    String curStriker;
    String curNonStriker;
    String curBowler;
    int numberOfPlayers;
    int innings;
    HashMap<String, Integer> batsmanRuns = new HashMap<>();
    HashMap<String, Integer> batsmanBalls = new HashMap<>();
    HashMap<String, Integer> bowlerRunsConceded = new HashMap<>();
    HashMap<String, Integer> bowlerWickets = new HashMap<>();
    HashMap<String, Double> bowlerOversBowled = new HashMap<>();
    HashSet<String> batsmenThisInnings = new HashSet<>();
    HashSet<String> bowlersThisInnings = new HashSet<>();
    int totalRuns = 0;
    int totalWickets = 0;
    int ballsBowled = 0;
    int runsThisOver = 0;
    int currentOver = 0;
    int ballsInCurrentOver = 0;
    boolean inningsOver = false;
    int runsScoredThisBall=0;
    int extraRunsThisBall=0;
    HashMap<String, Integer> bowlerOvers = new HashMap<>();
    HashSet<String> outPlayers = new HashSet<>();

    Scanner sc = new Scanner(System.in);

    public Score(Team battingTeam, Team bowlingTeam, Connection con, int overs, int overs_per_bowler, int matchId) {
        this.battingTeam = battingTeam;
        this.bowlingTeam = bowlingTeam;
        this.con = con;
        this.overs = overs;
        this.overs_per_bowler = overs_per_bowler;
        this.numberOfPlayers = battingTeam.totalPlayers;
        this.matchId = matchId;
    }
    int getPlayerId(String playerName) {
        String query = "SELECT id FROM user WHERE name = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setString(1, playerName);
            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                return rs.getInt("id");
            }
        } catch (SQLException e) {
            System.out.println("Error fetching player ID for " + playerName + ": " + e.getMessage());
        }
        return -1;
    }

    void finalizeBowlingStats(String bowlerName) {
        int pid = getPlayerId(bowlerName);
        if (pid == -1) return;

        try {
            BowlingStats boStats = new BowlingStats(con, pid);
            int runs = bowlerRunsConceded.getOrDefault(bowlerName, 0);
            int wickets = bowlerWickets.getOrDefault(bowlerName, 0);

            // Update career best bowling figures
            boStats.updateBestBowling(wickets, runs);

            // Recalculate career economy, average, and strike rate
            boStats.economy();
            boStats.BowlingAverage();
            boStats.BowlingStrikeRate();
        } catch (SQLException e) {
            System.out.println("Error finalizing bowling stats for " + bowlerName + ": " + e.getMessage());
        }
    }

    void finalizeInningsStats(String curStriker, String curNonStriker, String curBowler) {
        System.out.println("\n--- Finalizing Innings Stats ---");
        // Finalize stats for the batsman who was not out
        if (curStriker!=null && !curStriker.isEmpty() && !outPlayers.contains(curStriker)) {
            finalizeBattingStats(curStriker, true); // isNotOut = true
        }
        if (curNonStriker!=null && !curNonStriker.isEmpty() && !outPlayers.contains(curNonStriker)) {
            finalizeBattingStats(curNonStriker, true); // isNotOut = true
        }

        // Finalize stats for every bowler who bowled in the innings
        for (String bowlerName : bowlersThisInnings) {
            finalizeBowlingStats(bowlerName);
        }
    }
    // Add this method inside your Score class
    void finalizeBattingStats(String batsmanName, boolean isNotOut) {
        int pid = getPlayerId(batsmanName);
        if (pid == -1) return;

        try {
            BattingStats bStats = new BattingStats(con,pid);
            //bStats.Innings(); // Increment innings played
            int runs = batsmanRuns.getOrDefault(batsmanName, 0);
            bStats.Highestruns(runs);
            if (runs >= 100) {
                bStats.hundred();
            } else if (runs >= 50) {
                bStats.fifties();
            } else if (runs >= 30) {
                bStats.thirties();
            }
            if (runs == 0 && !isNotOut) {
                bStats.Ducks();
            }
            if (isNotOut) {
                bStats.NO();
            }
            bStats.Average();
            bStats.Strikerate();

        } catch (SQLException e) {
            System.out.println("Error finalizing batting stats for " + batsmanName + ": " + e.getMessage());
        }
    }
    // Add this method inside your Score class
    void updatePlayerStats(int runs, String ballType, String dismissalInfo) {
        int strikerId = getPlayerId(curStriker);
        int bowlerId = getPlayerId(curBowler);

        if (strikerId == -1 || bowlerId == -1) {
            System.out.println("Error: Could not find player ID. Stats not updated.");
            return;
        }

        try {
            // Initialize stats for the batsman and bowler
            BattingStats bStats = new BattingStats(con, strikerId);
            BowlingStats boStats = new BowlingStats(con, bowlerId);

            // Update innings if player is new
            if (!batsmenThisInnings.contains(curStriker)) {
                bStats.Innings();
                batsmenThisInnings.add(curStriker);
            }
            if (!bowlersThisInnings.contains(curBowler)) {
                boStats.Innings();
                bowlersThisInnings.add(curBowler);
            }

            // Update batsman runs
            batsmanRuns.put(curStriker, batsmanRuns.getOrDefault(curStriker, 0) + runs);

            // For normal ball types (not wide or no-ball)
            if (!ballType.equals("WIDE") && !ballType.equals("NO_BALL")) {
                bStats.balls(); // Batsman faced a ball
                batsmanBalls.put(curStriker, batsmanBalls.getOrDefault(curStriker, 0) + 1);

                // Calculate overs for the bowler
                double oversForBowler = bowlerOversBowled.getOrDefault(curBowler, 0.0);
                oversForBowler += 0.1; // Increment by 0.1 (one ball)

                // Round overs if necessary
                if ((oversForBowler * 10) % 10 == 6) {
                    oversForBowler = Math.round(oversForBowler);
                }
                bowlerOversBowled.put(curBowler, oversForBowler);
                boStats.overs(); // Increment bowler's overs
            }

            // Handle specific run types
            if (runs > 0) {
                bStats.run(runs);
                boStats.runsConceded(runs);

                // Handle fours and sixes
                if (runs == 4) {
                    bStats.fours();
                    boStats.fours(); // 4s conceded
                } else if (runs == 6) {
                    bStats.sixes();
                    boStats.sixes(); // 6s conceded
                }
            } else if (ballType.equals("NORMAL")) {
                boStats.DotBall(); // Dot ball (no runs)
            }

            // Handle wide and no-ball types
            if (ballType.equals("WIDE")) {
                boStats.wides();
                boStats.runsConceded(1 + runs); // Extra runs for wide
            }
            if (ballType.equals("NO_BALL")) {
                boStats.NoBalls();
                boStats.runsConceded(1 + runs); // Extra runs for no-ball
            }

            // Handle player dismissal
            if (ballType.equals("OUT")) {
                finalizeBattingStats(curStriker, false); // Batsman is out

                // Check who gets credit for the wicket
                if (dismissalInfo.startsWith("Bowled") || dismissalInfo.startsWith("Caught") || dismissalInfo.startsWith("LBW") || dismissalInfo.startsWith("Hit Wicket")) {
                    boStats.wickets(); // Assuming this increments wickets
                    int wicketsInMatch = bowlerWickets.getOrDefault(curBowler, 0) + 1;
                    bowlerWickets.put(curBowler, wicketsInMatch);

                    // Notify 5-wicket haul
                    if (wicketsInMatch == 5) {
                        System.out.println("!!! 5-WICKET HAUL for " + curBowler + " !!!");
                        boStats.FiveW();
                    }
                }

                // Fielding stats for catch (if applicable)
                if (dismissalInfo.contains("Caught Behind")) {
                    String catcherName = dismissalInfo.substring(dismissalInfo.indexOf("Caught by ") + 10, dismissalInfo.indexOf(" (bowler:")).trim();
                    int catcherId = getPlayerId(catcherName);
                    if (catcherId != -1) {
                        new FieldingStats(con, catcherId).caughtbehind();
                    }
                }

                // Handle player who was dismissed (default to striker)
                String dismissedPlayerName = curStriker;
                if (dismissalInfo.contains("Run out")) {
                    String fielderName = dismissalInfo.substring(dismissalInfo.lastIndexOf(" by ") + 4).trim();
                    int fielderId = getPlayerId(fielderName);
                    if (fielderId != -1) {
                        new FieldingStats(con, fielderId).Runout();
                    }
                }

                // Finalize batting stats for dismissed player
                finalizeBattingStats(dismissedPlayerName, false);

                // Stumping (if wicketkeeper involved)
                if (dismissalInfo.startsWith("Stumped")) {
                    String keeperName = dismissalInfo.substring("Stumped by ".length()).trim();
                    int keeperId = getPlayerId(keeperName);
                    if (keeperId != -1) {
                        new FieldingStats(con, keeperId).stumping();
                    }
                }
            }
            if (!con.getAutoCommit()) {
                con.commit();
            }

        } catch (SQLException e) {
            System.out.println("An error occurred while updating player stats: " + e.getMessage());
            e.printStackTrace(); // Optionally print the stack trace for debugging purposes
        } catch (Exception e) {
            System.out.println("An unexpected error occurred: " + e.getMessage());
            e.printStackTrace(); // Optionally print the stack trace for debugging purposes
        }
    }

    void resetScores(Team newBatting, Team newBowling) {
        this.battingTeam = newBatting;
        this.bowlingTeam = newBowling;
        totalRuns = 0;
        totalWickets = 0;
        ballsBowled = 0;
        currentOver = 0;
        ballsInCurrentOver = 0;
        numberOfPlayers = newBatting.totalPlayers;
        inningsOver = false;
        bowlerOvers.clear();
        outPlayers.clear();
    }

    public int firstInnings() {
        this.innings=1;
        resetScores(battingTeam, bowlingTeam);
        updateMatchCounts();
        playInnings(-1, -1);
        //this.oversBowled=currentOver+ballsInCurrentOver/10.0;
        return totalRuns;
    }

    void updateMatchCounts() {
        System.out.println("Updating match counts for all players...");
        List<Player> allPlayers = new ArrayList<>();
        allPlayers.addAll(battingTeam.players);
        allPlayers.addAll(bowlingTeam.players);

        for (Player player : allPlayers) {
            int pid = getPlayerId(player.name);
            if (pid != -1) {
                try {
                    new BattingStats(con, pid).match();
                    new BowlingStats(con, pid).match();
                    new FieldingStats(con, pid).match();

                    // Check if the player is a captain and update their match count
                    if (player.name.equalsIgnoreCase(battingTeam.captainName) || player.name.equalsIgnoreCase(bowlingTeam.captainName)) {
                        new CaptainStats(con, pid).Cmatches();
                    }
                } catch (SQLException e) {
                    System.out.println("Error updating match count for " + player.name + ": " + e.getMessage());
                }
            }
        }
    }

    public int secondInnings(int target, int tossWinnerId) {
        this.innings=2;
        resetScores(bowlingTeam, battingTeam);
        playInnings(target, tossWinnerId);
        //this.oversBowled=currentOver+ballsInCurrentOver/10.0;
        return totalRuns;
    }

    void playInnings(int target, int tossWinnerId) {

        startingDetails(battingTeam, bowlingTeam);
        System.out.println("\nInnings Started!" + (target > 0 ? " Target: " + target : ""));

        while (!inningsOver) {
            int ballsInOver=ballsInCurrentOver+1;
            System.out.println("\n--- Over: " + currentOver + "." + ballsInOver + " ---");
            getBallDetails(target);
            if (target > 0 && totalRuns >= target) {
                inningsOver = true;
            }
        }
        if (target > 0) {
            Team winner = null;
            Team loser = null;

            if (totalRuns >= target) {
                winner = battingTeam;
                loser = bowlingTeam;
                System.out.println("\n=== Match Over ===\n" + winner.teamName + " won by chasing successfully!");

            } else if (totalRuns < target - 1) {
                winner = bowlingTeam;
                loser = battingTeam;
                System.out.println("\n=== Match Over ===\n" + winner.teamName + " won by " + (target - 1 - totalRuns) + " runs.");

            } else {
                System.out.println("\n=== Match Over ===\nMatch tied!");
            }

            if (winner != null && loser != null) {
                try {
                    int winnerCaptainId = getPlayerId(winner.captainName);

                    if (winnerCaptainId != -1) {
                        CaptainStats capStats = new CaptainStats(con, winnerCaptainId);
                        if (winner.teamId == tossWinnerId) {
                            capStats.tosswon();
                        }
                        capStats.WinPer(true);
                    }

                    int loserCaptainId = getPlayerId(loser.captainName);

                    if (loserCaptainId != -1) {
                        CaptainStats capStats = new CaptainStats(con, loserCaptainId);
                        if (loser.teamId == tossWinnerId) {
                            capStats.tosswon();
                        }
                        capStats.WinPer(false);
                    }
                    String query = "UPDATE Matches SET winner_team_id = ? WHERE match_id = ?";
                    try(PreparedStatement pstmt = con.prepareStatement(query)) {
                        pstmt.setInt(1, winner.getTeamId());
                        pstmt.setInt(2, matchId);
                        pstmt.executeUpdate();
                    }
                } catch (SQLException e) {
                    System.out.println("Error finalizing captain stats: " + e.getMessage());
                }
            }
        }
        finalizeInningsStats(curStriker,curNonStriker,curBowler);
    }

    void startingDetails(Team batTeam, Team bowlTeam) {
        System.out.println("Batting - " + batTeam.teamName);

        List<String> batNames = batTeam.players.stream().map(p -> p.name).collect(Collectors.toList());
        List<String> bowlNames = bowlTeam.players.stream().map(p -> p.name).collect(Collectors.toList());

        // Select striker
        System.out.println("Players List: ");
        batNames.forEach(System.out::println);

        this.curStriker = null;
        while (this.curStriker == null || !batNames.contains(this.curStriker)) {
            try {
                System.out.println("Select striker:");
                this.curStriker = sc.nextLine().trim();  // Trim to avoid leading/trailing spaces

                if (!batNames.contains(this.curStriker)) {
                    System.out.println("Invalid striker. Please select a valid player from the list.");
                }
            } catch (Exception e) {
                System.out.println("Invalid input. Please try again.");
                sc.nextLine();  // Clear the buffer
            }
        }

        // Select non-striker
        System.out.println("Players List (excluding " + this.curStriker + "): ");
//        String finalCurStriker = this.curStriker;
        batNames.stream()
                .filter(name -> !name.equalsIgnoreCase(this.curStriker))
                .forEach(System.out::println);

        this.curNonStriker = null;
        while (this.curNonStriker == null || !batNames.contains(this.curNonStriker) || this.curNonStriker.equalsIgnoreCase(curStriker)) {
            try {
                System.out.println("Select non-striker:");
                this.curNonStriker = sc.nextLine().trim();

                if (!batNames.contains(this.curNonStriker)) {
                    System.out.println("Invalid non-striker. Please select a valid player from the list.");
                } else if (this.curNonStriker.equalsIgnoreCase(this.curStriker)) {
                    System.out.println("The non-striker cannot be the same as the striker. Please select a different player.");
                }
            } catch (Exception e) {
                System.out.println("Invalid input. Please try again.");
                sc.nextLine();  // Clear the buffer
            }
        }

        // Select bowler
        System.out.println("Bowling - " + bowlTeam.teamName);
        System.out.println("Player List: ");
        bowlNames.forEach(System.out::println);

        this.curBowler = null;
        while (this.curBowler == null || !bowlNames.contains(this.curBowler)) {
            try {
                System.out.println("Select bowler:");
                this.curBowler = sc.nextLine().trim();  // Trim to avoid leading/trailing spaces

                if (!bowlNames.contains(curBowler)) {
                    System.out.println("Invalid bowler. Please select a valid player from the list.");
                }
            } catch (Exception e) {
                System.out.println("Invalid input. Please try again.");
                sc.nextLine();  // Clear the buffer
            }
        }
        bowlerOvers.clear();
        bowlerOvers.put(curBowler, 1);
    }

    void getBallDetails(int target) {
        Commentary c = new Commentary(curStriker, curNonStriker, curBowler);
        int ch;

        while (true) {
            try {
                System.out.println("\n--- Enter Ball Details ---");
                System.out.println("1. Dot   2. 1 run   3. 2 runs   4. 3 runs   5. 4 runs   6. 6 runs");
                System.out.println("7. Wide  8. No Ball 9. Bye     10. Leg Bye 11. OUT     12. 5/7 runs");
                System.out.println("13. UNDO");
                System.out.print("Enter choice: ");
                ch = sc.nextInt();
                sc.nextLine();

                if (ch >= 1 && ch <= 13) {
                    break;
                } else {
                    System.out.println("Invalid choice. Please enter a number between 1 and 13.");
                }
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine();
            }
        }

        switch (ch) {
            case 1 -> {
                c.no_run();
                runsScoredThisBall = 0;
                extraRunsThisBall = 0;
                incrementBall("NORMAL");
                updatePlayerStats(0, "NORMAL", null);
            }
            case 2, 3, 4, 5, 6 -> {
                int runs = (ch == 2) ? 1 : (ch == 3) ? 2 : (ch == 4) ? 3 : (ch == 5) ? 4 : 6;
                c.run(runs);
                handleRun(runs);
                runsScoredThisBall = runs;
                extraRunsThisBall = 0;
                incrementBall("NORMAL");
            }
            case 7 -> {
                int wideRuns = 0;
                while (wideRuns < 0) {
                    try {
                        System.out.print("Enter Extra Runs On Wide: ");
                        wideRuns = sc.nextInt();
                        sc.nextLine();

                        if (wideRuns < 0) {
                            System.out.println("Wide runs cannot be negative. Please enter a valid number.");
                        }
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a valid number for wide runs.");
                        sc.nextLine(); // Clear the buffer
                    }
                }
                c.wide(wideRuns);
                totalRuns += 1 + wideRuns;
                runsThisOver += wideRuns;
                runsScoredThisBall = 0;
                extraRunsThisBall = 1+wideRuns;
                insertScoreRow("WIDE",0,1+wideRuns);
                updatePlayerStats(wideRuns, "WIDE", null);
            }
            case 8 -> {
                int noBallRuns = -1;
                while (noBallRuns < 0) {
                    try {
                        System.out.print("Enter Extra Runs On No-Ball: ");
                        noBallRuns = sc.nextInt();
                        sc.nextLine();

                        if (noBallRuns < 0) {
                            System.out.println("No-ball runs cannot be negative. Please enter a valid number.");
                        }
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a valid number for no-ball runs.");
                        sc.nextLine(); // Clear the buffer
                    }
                }
                c.no_ball(noBallRuns);
                totalRuns += 1 + noBallRuns;
                runsThisOver += 1 + noBallRuns;
                insertScoreRow("NO_BALL",0,1 + noBallRuns);
                updatePlayerStats(noBallRuns, "NO_BALL", null);
            }
            case 9, 10 -> {
                int extra = -1;
                while (extra < 0) {
                    try {
                        System.out.print("Enter runs: ");
                        extra = sc.nextInt();
                        sc.nextLine();

                        if (extra < 0) {
                            System.out.println("Runs cannot be negative. Please enter a valid number of runs.");
                        }
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a valid number of runs for bye/leg-bye.");
                        sc.nextLine(); // Clear the buffer
                    }
                }
                c.byes(ch, extra);
                totalRuns += extra;
                runsThisOver += extra;

                handleRun(extra);
                incrementBall(ch == 9 ? "BYE" : "LEG_BYE");
            }
            case 11 -> {
                runsScoredThisBall=0;
                String outType = outDetails();
                c.out(outType);
                totalWickets++;
                outPlayers.add(curStriker);
                incrementBall("OUT");

                updatePlayerStats(0, "OUT", outType);

                if (totalWickets >= numberOfPlayers - 1) {
                    inningsOver = true;
                    finalizeBattingStats(curNonStriker, true);
                    return;
                }
                selectNextBatsman();
            }
            case 12 -> {
                int special = -1;
                while (special != 5 && special != 7) {
                    try {
                        System.out.print("Enter runs (5 or 7): ");
                        special = sc.nextInt();
                        sc.nextLine();

                        if (special != 5 && special != 7) {
                            System.out.println("Invalid input. Only 5 or 7 runs are allowed.");
                        }
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter 5 or 7 runs.");
                        sc.nextLine(); // Clear the buffer
                    }
                }
                c.run(special);
                handleRun(special);
                incrementBall("SPECIAL");
            }
            default -> System.out.println("Invalid choice");  // This case is unnecessary as we already validate input
        }

        if (target > 0 && totalRuns >= target){ inningsOver = true;}
    }

    void undoLastBall() {
        System.out.print("Are you sure you want to undo the last ball? (Y/N): ");
        String confirmation = sc.nextLine();

        if (!confirmation.equalsIgnoreCase("Y")) {
            System.out.println("No changes made.");
            return;
        }

        try {
            // Step 1: Get the last ball's data
            String fetchQuery = "SELECT * FROM Score WHERE match_id = ? AND innings = ? ORDER BY ball_number DESC LIMIT 1";
            PreparedStatement fetchStmt = con.prepareStatement(fetchQuery);
            fetchStmt.setInt(1, matchId);
            fetchStmt.setInt(2, innings);
            ResultSet rs = fetchStmt.executeQuery();

            if (!rs.next()) {
                System.out.println("No previous ball to undo.");
                return;
            }

            // Store all data from the last ball
            int prevBallNumber = rs.getInt("ball_number");
            int prevRuns = rs.getInt("total_runs");
            int prevWickets = rs.getInt("total_wickets");
            int prevBallsBowled = rs.getInt("balls_bowled");
            int prevOver = rs.getInt("current_over");
            int prevBallsInOver = rs.getInt("balls_in_current_over");
            String prevStriker = rs.getString("striker");
            String prevNonStriker = rs.getString("non_striker");
            String prevBowler = rs.getString("bowler");
            String prevBallType = rs.getString("ball_type");

            // Step 2: Delete the last ball
            String deleteQuery = "DELETE FROM Score WHERE match_id = ? AND innings = ? AND ball_number = ?";
            PreparedStatement deleteStmt = con.prepareStatement(deleteQuery);
            deleteStmt.setInt(1, matchId);
            deleteStmt.setInt(2, innings);
            deleteStmt.setInt(3, prevBallNumber);
            int deleted = deleteStmt.executeUpdate();

            if (deleted <= 0) {
                System.out.println("Undo failed: Could not delete from database.");
                return;
            }

            // Step 3: Revert in-memory data
            ballsBowled--;
            totalRuns = prevRuns;
            totalWickets = prevWickets;
            currentOver = prevOver;
            ballsInCurrentOver = prevBallsInOver;
            curStriker = prevStriker;
            curNonStriker = prevNonStriker;
            curBowler = prevBowler;

            // Step 4: Handle bowler over rollback if needed
            if (prevBallType.equals("NORMAL") || prevBallType.equals("OUT") || prevBallType.equals("BYE") || prevBallType.equals("LEG_BYE") || prevBallType.equals("SPECIAL")) {
                int currentOvers = bowlerOvers.getOrDefault(curBowler, 0);
                if (currentOvers > 0) {
                    bowlerOvers.put(curBowler, currentOvers - 1);
                }
            }

            // Step 5: If undoing the first ball of a new over, revert over-end logic
            if (ballsInCurrentOver == 6) {
                // Swap striker/non-striker back
                String temp = curStriker;
                curStriker = curNonStriker;
                curNonStriker = temp;

                // Find previous bowler by querying second last ball
                String queryPrevBowler = "SELECT bowler FROM Score WHERE match_id = ? AND innings = ? ORDER BY ball_number DESC LIMIT 1 OFFSET 1";
                PreparedStatement stmt = con.prepareStatement(queryPrevBowler);
                stmt.setInt(1, matchId);
                stmt.setInt(2, innings);
                ResultSet rs2 = stmt.executeQuery();
                if (rs2.next()) {
                    String lastOverBowler = rs2.getString("bowler");
                    curBowler = lastOverBowler;

                    // Adjust overs count
                    bowlerOvers.put(curBowler, bowlerOvers.getOrDefault(curBowler, 0));
                }
            }

            // Step 6: Wicket rollback
            if (prevBallType.equals("OUT")) {
                outPlayers.remove(curStriker);  // assumes striker was out
            }

            System.out.println("Last ball undone successfully.");
            printScore();

        } catch (SQLException e) {
            System.out.println("Error during undo: " + e.getMessage());
        }
    }

    void handleRun(int runs) {
        totalRuns += runs;
        runsThisOver += runs;

        // This call is necessary to ensure player stats are updated when runs are scored.
        updatePlayerStats(runs, "NORMAL", null);

        if (runs % 2 != 0) {
            String temp = curStriker;
            curStriker = curNonStriker;
            curNonStriker = temp;
        }
        printScore();
    }

    void incrementBall(String ballType) {
        ballsBowled++;
        ballsInCurrentOver++;
        insertScoreRow(ballType,runsScoredThisBall,extraRunsThisBall);

        if (ballsInCurrentOver == 6) {
            currentOver++;
            ballsInCurrentOver = 0;
            String temp = curStriker;
            curStriker = curNonStriker;
            curNonStriker = temp;
            if (runsThisOver == 0) {
                try {
                    int bowlerId = getPlayerId(curBowler);
                    if (bowlerId != -1) {
                        new BowlingStats(con, bowlerId).maidens();
                        System.out.println("--- Maiden Over for " + curBowler + "! ---");
                    }
                } catch (SQLException e) {
                    System.out.println("Error updating maiden over: " + e.getMessage());
                }
            }
            runsThisOver = 0;

            System.out.println("End of over " + currentOver + ". Striker and non-striker swapped.");

            if (currentOver >= overs) {
                inningsOver = true;
                System.out.println("Innings Over.");
                return;
            }
            selectNextBowler();
        }
    }

    void selectNextBowler() {
        System.out.println("Select new bowler (not same as previous):");

        List<String> eligibleBowlers = new ArrayList<>();
        for (Player p : bowlingTeam.players) {
            if (!p.name.equalsIgnoreCase(curBowler)) {
                eligibleBowlers.add(p.name);
                System.out.println(p.name);
            }
        }

        String nextBowler = sc.nextLine();

        while (!eligibleBowlers.contains(nextBowler)) {
            System.out.println("Invalid selection. Please choose a valid bowler:");
            nextBowler = sc.nextLine();
        }

        if (overs_per_bowler > 0 && bowlerOvers.getOrDefault(nextBowler, 0) >= overs_per_bowler) {
            System.out.println(nextBowler + " has reached their limit. Choose again.");
            selectNextBowler();
            return;
        }

        curBowler = nextBowler;
        bowlerOvers.put(nextBowler, bowlerOvers.getOrDefault(nextBowler, 0) + 1);
    }

    void selectNextBatsman() {
        System.out.println("Select next batsman:");

        List<String> eligibleBatsmen = new ArrayList<>();
        for (Player p : battingTeam.players) {
            if (!p.name.equalsIgnoreCase(curStriker)
                    && !p.name.equalsIgnoreCase(curNonStriker)
                    && !outPlayers.contains(p.name)) {
                eligibleBatsmen.add(p.name);
                System.out.println(p.name);
            }
        }

        String nextBatsman = sc.nextLine();

        while (!eligibleBatsmen.contains(nextBatsman)) {
            System.out.println("Invalid selection. Please choose a valid batsman:");
            nextBatsman = sc.nextLine();
        }

        curStriker = nextBatsman;
    }

    // Note the two new parameters: runsScoredThisBall and extraRunsThisBall
    void insertScoreRow(String ballType, int runsScoredThisBall, int extraRunsThisBall) {
        // Add the new columns to the SQL query
        String query = "INSERT INTO Score (ball_number, match_id, total_runs, total_wickets, balls_bowled, current_over, balls_in_current_over, striker, non_striker, bowler, ball_type, innings, runs_scored, extra_runs) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, ballsBowled);
            pstmt.setInt(2, matchId);
            pstmt.setInt(3, totalRuns);
            pstmt.setInt(4, totalWickets);
            pstmt.setInt(5, ballsBowled);
            pstmt.setInt(6, currentOver);
            pstmt.setInt(7, ballsInCurrentOver);
            pstmt.setString(8, curStriker);
            pstmt.setString(9, curNonStriker);
            pstmt.setString(10, curBowler);
            pstmt.setString(11, ballType);
            pstmt.setInt(12, innings);

            // Add these two new lines to set the values for the new columns
            pstmt.setInt(13, runsScoredThisBall);
            pstmt.setInt(14, extraRunsThisBall);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.out.println("Error inserting score row: " + e.getMessage());
        }
    }

    String outDetails() {
        String[] outTypes = {
                "Bowled", "Caught", "Caught Behind", "Run Out", "LBW",
                "Stumped", "Retired Hurt", "Hit Wicket", "Obstruct The Field", "Hit the Ball Twice"
        };

        System.out.println("Select type of dismissal:");
        for (int i = 0; i < outTypes.length; i++) {
            System.out.println((i + 1) + ". " + outTypes[i]);
        }

        int choice;
        while(true) {
            try {
                System.out.print("Enter choice: ");
                choice = sc.nextInt();
                sc.nextLine(); // consume newline
                if (choice >= 1 && choice <= outTypes.length) {
                    break; // valid input, exit loop
                } else {
                    System.out.println("⚠ Invalid choice. Please enter a number between 1 and " + outTypes.length + ".");
                }
            } catch (InputMismatchException e) {
                System.out.println("⚠ Invalid input. Please enter a number.");
                sc.nextLine(); // clear the invalid input from the scanner
            }
        }

        if (choice < 1 || choice > outTypes.length) {
            return "Invalid dismissal type.";
        }

        String dismissal = outTypes[choice - 1];
        String detail = "";

        // Inside the outDetails() method in Score.java

        switch (dismissal) {
            case "Caught", "Caught Behind" -> { // This case handles both regular catches and caught behind
                System.out.println("Who took the catch? (Enter fielder/wicketkeeper name)");
                String catcher = sc.nextLine();
                if (!isPlayerInTeam(catcher, bowlingTeam.players)) {
                    return "Invalid: " + catcher + " is not in bowling team.";
                }
                detail = "Caught by " + catcher + " (bowler: " + curBowler + ")";
            }
            case "Bowled" -> detail = "Bowled by " + curBowler;
            case "Run Out" -> {
                System.out.println("Who was run out?");
                System.out.println("1. Striker (" + curStriker + ")");
                System.out.println("2. Non-Striker (" + curNonStriker + ")");
                int choice1 = sc.nextInt(); sc.nextLine();
                String dismissedBatsman = (choice1 == 1) ? curStriker : curNonStriker;

                System.out.println("Who assisted the run out? (Enter fielder's name)"); // Ask for the fielder
                String fielder = sc.nextLine();
                detail = "Run out (" + dismissedBatsman + ") by " + fielder; // This string now contains the fielder's name
            }
            case "Stumped" -> {
                System.out.println("Who was the wicketkeeper?"); // Ask for the keeper
                String keeper = sc.nextLine();
                detail = "Stumped by " + keeper; // This string now contains the keeper's name
            }
            case "LBW", "Retired Hurt", "Hit Wicket", "Obstruct The Field", "Hit the Ball Twice" -> detail = dismissal;
        }

        return detail;
    }

    boolean isPlayerInTeam(String playerName, List<Player> teamPlayers) {
        for (Player p : teamPlayers) {
            if (p.name.equalsIgnoreCase(playerName)) return true;
        }
        return false;
    }

    void printScore() {
        System.out.println("Score: " + totalRuns + "/" + totalWickets + " in " + currentOver + "." + ballsInCurrentOver + " overs.");
        System.out.println("Striker: " + curStriker + " | Non-Striker: " + curNonStriker + " | Bowler: " + curBowler);
    }
}
