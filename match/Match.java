package match;

import scoreCard.Score;
import team.Team;
import dataStructure.S_LinkedList_Int;

import java.sql.*;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
public class Match {
    int matchId;
    Team teamA;
    Team teamB;
    Connection con;
    int tossWinnerChoice;    // 1 or 2
    int winnerChoice;   // 1 = Bat, 2 = Bowl
    int scoreA;
    int scoreB;
    double oversBowledA;
    double oversBowledB;

    MatchType matchType = new MatchType();
    Scanner sc = new Scanner(System.in);
    int tossWinnerId;

    public Match(Team teamA, Team teamB, Connection con) {
        this.teamA = teamA;
        this.teamB = teamB;
        this.con = con;
        this.matchId = getMatchIdByTeams(con, teamA.getTeamId(), teamB.getTeamId());
    }
    public void matchTypeDetails() {
        this.matchId = matchType.matchTypeDetails();
    }

    public void toss() throws SQLException {
        do {
            System.out.println("Select \n1.)Head\n2.)Tails\nEnter Choice: ");
            try {
                int choice  =  0;
                while (true) {
                    try {
                        choice = sc.nextInt();
                        break;
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine(); // clear buffer
                    }
                }
                if(choice == 1 || choice == 2) {
                    break;
                }
                System.out.println("Enter from given choices only");
            }
            catch (InputMismatchException e) {
                System.out.println("Please enter a valid choice");
            }
        }while (true);

        int toss = (int) (Math.random() * 2);
        System.out.println(toss == 0 ? "Head" : "Tails");

        System.out.println("Who won the Toss?\n1. " + teamA.teamName + "\n2. " + teamB.teamName);
        while (true) {
            try {
                tossWinnerChoice = sc.nextInt();
                break;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine(); // clear buffer
            }
        }
        this.tossWinnerId = (tossWinnerChoice == 1) ? teamA.getTeamId() : teamB.getTeamId();
        System.out.println("Winner of the toss elected to?\n1. Bat\n2. Bowl");
        while (true) {
            try {
                winnerChoice = sc.nextInt();
                break;
            } catch (InputMismatchException e) {
                System.out.println("Invalid input. Please enter a number.");
                sc.nextLine(); // clear buffer
            }
        }

        String tossDecision = (winnerChoice == 1) ? "Bat" : "Bowl";
        String query = "UPDATE Matches SET toss_winner_team_id = ?, toss_decision = ? WHERE match_id = ?";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, tossWinnerId);
            pstmt.setString(2, tossDecision);
            pstmt.setInt(3, matchId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        // FIX: Removed the broken stat update logic from here.
        // This is now correctly handled by the Score class.
    }
    public boolean teamExists(Connection con, int teamId) throws SQLException {
        String query = "SELECT 1 FROM Team WHERE team_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, teamId);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();
        }
    }

    public void playMatch(int tournamentId) {
        System.out.println("---------------");
        System.out.println("Start Innings");

        Score score;
        Team battingFirst, bowlingFirst;
        boolean tossWinnerBats = (winnerChoice == 1);

        if ((tossWinnerId == teamA.getTeamId() && tossWinnerBats) || (tossWinnerId == teamB.getTeamId() && !tossWinnerBats)) {
            battingFirst = teamA;
            bowlingFirst = teamB;
        } else {
            battingFirst = teamB;
            bowlingFirst = teamA;
        }
        score = new Score(battingFirst, bowlingFirst, con, matchType.no_of_overs, matchType.overs_per_bowler, matchId);
        int firstInningsScore = score.firstInnings();
        double firstInningsBalls= score.oversBowled;
        int secondInningsScore = score.secondInnings(firstInningsScore + 1, this.tossWinnerId);
        double secondInningsBalls = score.oversBowled;
        if (battingFirst.teamId == teamA.teamId) {
            scoreA = firstInningsScore;
            scoreB = secondInningsScore;
            this.oversBowledA=firstInningsBalls;
            this.oversBowledB=secondInningsBalls;
        } else {
            scoreB = firstInningsScore;
            scoreA = secondInningsScore;
            this.oversBowledB=firstInningsBalls;
            this.oversBowledA=secondInningsBalls;
        }
        String query = "UPDATE Matches SET teamA_id = ?, teamB_id = ?, tournament_id = ? WHERE match_id = ?";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, teamA.getTeamId());
            pstmt.setInt(2, teamB.getTeamId());

            if (tournamentId > 0) {
                pstmt.setInt(3, tournamentId);
            } else {
                pstmt.setNull(3, java.sql.Types.INTEGER);
            }

            pstmt.setInt(4, matchId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void results() {
        // FIX: Removed the broken stat update logic from here.
        // The Score class now handles wins, losses, and matches played for captains.
        if (scoreA > scoreB) {
            System.out.println("Team: " + teamA.teamName + " is winner");
        } else if (scoreB > scoreA) {
            System.out.println("Team: " + teamB.teamName + " is winner");
        } else {
            System.out.println("Draw - Super Over required");
        }

        // This part is still needed to record the match winner in the Matches table.
        String query = "UPDATE Matches SET winner_team_id = ? WHERE match_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            if (scoreA > scoreB) {
                pstmt.setInt(1, teamA.getTeamId());
            } else if (scoreB > scoreA) {
                pstmt.setInt(1, teamB.getTeamId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, matchId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void results(int tournament_id, Team teamA, Team teamB) throws SQLException {
        if (scoreA > scoreB) {
            System.out.println("Team: " + teamA.teamName + " is winner");
            updatePointsTable(tournament_id,teamA,teamB,teamA);
        } else if (scoreB > scoreA) {
            System.out.println("Team: " + teamB.teamName + " is winner");
            updatePointsTable(tournament_id,teamA,teamB,teamB);
        } else {
            System.out.println("Draw - Super Over required");
            updatePointsTable(tournament_id,teamA,teamB,null);
        }

        String query = "UPDATE Matches SET winner_team_id = ? WHERE match_id = ?";

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            if (scoreA > scoreB) {
                pstmt.setInt(1, teamA.getTeamId());
            } else if (scoreB > scoreA) {
                pstmt.setInt(1, teamB.getTeamId());
            } else {
                pstmt.setNull(1, Types.INTEGER);
            }
            pstmt.setInt(2, matchId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }
    void updatePointsTable(int tournament_id,Team tA,Team tB,Team winner) throws SQLException {
        String qtid="select team_id from team where team_name=?";
        try (PreparedStatement pst = con.prepareStatement(qtid)) {
            pst.setString(1,tA.teamName);
            ResultSet rst= pst.executeQuery();
            if (rst.next()) {
                tA.teamId=rst.getInt("team_id");
            }
        }
        try (PreparedStatement pstB = con.prepareStatement(qtid)) {
            pstB.setString(1,tB.teamName);
            ResultSet rstB= pstB.executeQuery();
            if (rstB.next()) {
                tB.teamId=rstB.getInt("team_id");
            }
        }

        PreparedStatement checkForTeamA = con.prepareStatement("SELECT 1 FROM tournament_points WHERE tournament_id=? AND team_id=?");
        checkForTeamA.setInt(1, tournament_id);
        checkForTeamA.setInt(2, tA.teamId);
        ResultSet rs = checkForTeamA.executeQuery();

        if (rs.next()) {
            if (winner==tA) {
                PreparedStatement update = con.prepareStatement("UPDATE tournament_points SET matches_played=matches_played+1,wins=wins+1, points=points+2, runs_scored=runs_scored+"+scoreA+", runs_conceded=runs_conceded+"+scoreB+",overs_faced=overs_faced+"+oversBowledB+",overs_bowled=overs_bowled+"+oversBowledA+" WHERE tournament_id=? AND team_id=?");
                update.setInt(1, tournament_id);
                update.setInt(2, tA.teamId);
                update.executeUpdate();
                setNRR(tournament_id,tA.teamId);
            }
            else if(winner==tB)
            {
                PreparedStatement update = con.prepareStatement("UPDATE tournament_points SET matches_played=matches_played+1,losses=losses+1, runs_scored=runs_scored+"+scoreA+", runs_conceded=runs_conceded+"+scoreB+",overs_faced=overs_faced+"+oversBowledB+",overs_bowled=overs_bowled+"+oversBowledA+" WHERE tournament_id=? AND team_id=?");
                update.setInt(1, tournament_id);
                update.setInt(2, tA.teamId);
                update.executeUpdate();
                setNRR(tournament_id,tA.teamId);
            } else if (winner==null)
            {
                PreparedStatement update = con.prepareStatement("UPDATE tournament_points SET matches_played=matches_played+1,ties=ties+1,points=points+1,runs_scored=runs_scored+"+scoreA+", runs_conceded=runs_conceded+"+scoreB+",overs_faced=overs_faced+"+oversBowledB+",overs_bowled=overs_bowled+"+oversBowledA+" WHERE tournament_id=? AND team_id=?");
                update.setInt(1, tournament_id);
                update.setInt(2, tA.teamId);
                update.executeUpdate();
                setNRR(tournament_id,tA.teamId);
            }
        }
        else {
            if (winner==tA) {
                PreparedStatement insert = con.prepareStatement("INSERT INTO tournament_points (tournament_id, team_id, matches_played, points,wins,runs_scored,runs_conceded,overs_faced,overs_bowled) VALUES (?, ?, 1, 2,1,"+scoreA+","+scoreB+","+oversBowledB+","+oversBowledA+")");
                insert.setInt(1, tournament_id);
                insert.setInt(2, tA.teamId);
                insert.executeUpdate();
                setNRR(tournament_id,tA.teamId);
            }
            else if(winner==tB)
            {
                PreparedStatement insert = con.prepareStatement("INSERT INTO tournament_points (tournament_id, team_id, matches_played,losses,runs_scored,runs_conceded,overs_faced,overs_bowled) VALUES (?, ?, 1,1,"+scoreA+","+scoreB+","+oversBowledB+","+oversBowledA+")");
                insert.setInt(1, tournament_id);
                insert.setInt(2, tA.teamId);
                insert.executeUpdate();
                setNRR(tournament_id,tA.teamId);
            } else if (winner==null) {
                PreparedStatement update = con.prepareStatement("INSERT INTO tournament_points (tournament_id, team_id, matches_played,ties,runs_scored,runs_conceded,overs_faced,overs_bowled,points) VALUES (?, ?, 1,1,"+scoreA+","+scoreB+","+oversBowledB+","+oversBowledA+",1)");
                update.setInt(1, tournament_id);
                update.setInt(2, tA.teamId);
                update.executeUpdate();
                setNRR(tournament_id,tA.teamId);
            }
        }
        PreparedStatement checkForTeamB = con.prepareStatement("SELECT 1 FROM tournament_points WHERE tournament_id=? AND team_id=?");
        checkForTeamB.setInt(1, tournament_id);
        checkForTeamB.setInt(2, tB.teamId);
        ResultSet rsB = checkForTeamB.executeQuery();

        if (rsB.next()) {
            if (winner==tB) {
                PreparedStatement update = con.prepareStatement("UPDATE tournament_points SET matches_played=matches_played+1,wins=wins+1, points=points+2 , runs_scored=runs_scored+"+scoreB+", runs_conceded=runs_conceded+"+scoreA+",overs_faced=overs_faced+"+oversBowledA+",overs_bowled=overs_bowled+"+oversBowledB+" WHERE tournament_id=? AND team_id=?");
                update.setInt(1, tournament_id);
                update.setInt(2, tB.teamId);
                update.executeUpdate();
                setNRR(tournament_id,tB.teamId);
            }
            else if(winner==tA)
            {
                PreparedStatement update = con.prepareStatement("UPDATE tournament_points SET matches_played=matches_played+1,losses=losses+1 , runs_scored=runs_scored+"+scoreB+", runs_conceded=runs_conceded+"+scoreA+",overs_faced=overs_faced+"+oversBowledA+",overs_bowled=overs_bowled+"+oversBowledB+"WHERE tournament_id=? AND team_id=?");
                update.setInt(1, tournament_id);
                update.setInt(2, tB.teamId);
                update.executeUpdate();
                setNRR(tournament_id,tB.teamId);
            }
            else if (winner==null)
            {
                PreparedStatement update = con.prepareStatement("UPDATE tournament_points SET matches_played=matches_played+1,ties=ties+1,points=points+1,runs_scored=runs_scored+"+scoreA+", runs_conceded=runs_conceded+"+scoreB+",overs_faced=overs_faced+"+oversBowledB+",overs_bowled=overs_bowled+"+oversBowledA+" WHERE tournament_id=? AND team_id=?");
                update.setInt(1, tournament_id);
                update.setInt(2, tB.teamId);
                update.executeUpdate();
                setNRR(tournament_id,tB.teamId);
            }
        } else {
            if (winner==tB) {
                PreparedStatement insert = con.prepareStatement("INSERT INTO tournament_points (tournament_id, team_id, matches_played, points,wins,runs_scored,runs_conceded,overs_faced,overs_bowled) VALUES (?, ?, 1, 2,1,"+scoreB+","+scoreA+","+oversBowledA+","+oversBowledB+")");
                insert.setInt(1, tournament_id);
                insert.setInt(2, tB.teamId);
                insert.executeUpdate();
                setNRR(tournament_id,tB.teamId);
            }
            else if (winner==tA)
            {
                PreparedStatement insert = con.prepareStatement("INSERT INTO tournament_points (tournament_id, team_id, matches_played,losses,runs_scored,runs_conceded,overs_faced,overs_bowled) VALUES (?, ?, 1,1,"+scoreB+","+scoreA+","+oversBowledA+","+oversBowledB+")");
                insert.setInt(1, tournament_id);
                insert.setInt(2, tB.teamId);
                insert.executeUpdate();
                setNRR(tournament_id,tB.teamId);
            }
            else if (winner==null)
            {
                PreparedStatement update = con.prepareStatement("INSERT INTO tournament_points (tournament_id, team_id, matches_played,ties,runs_scored,runs_conceded,overs_faced,overs_bowled,points) VALUES (?, ?, 1,1,"+scoreA+","+scoreB+","+oversBowledB+","+oversBowledA+",1)");
                update.setInt(1, tournament_id);
                update.setInt(2, tB.teamId);
                update.executeUpdate();
                setNRR(tournament_id,tB.teamId);
            }
        }
    }
    public static void displayTournamentPoints(Connection con, int tournamentId) {
        String query = "SELECT * FROM Tournament_Points WHERE tournament_id = ?";
        boolean hasData = false;

        try (PreparedStatement pst = con.prepareStatement(query)) {
            pst.setInt(1, tournamentId);
            ResultSet rs = pst.executeQuery();

            // Check if ResultSet has rows
            if (!rs.isBeforeFirst()) {
                System.out.println("No matches have been played yet for Tournament ID: " + tournamentId);
                return;
            }

            // Header
            System.out.printf("%-10s %-10s %-15s %-6s %-6s %-6s %-10s %-6s %-12s %-12s %-12s %-12s %-8s%n",
                    "Tournament_ID", "Team_ID", "Matches_Played", "Wins", "Losses", "Ties",
                    "No_Results", "Points", "Runs_Scored", "Runs_Conceded",
                    "Overs_Faced", "Overs_Bowled", "NRR");
            System.out.println("---------------------------------------------------------------------------------------------------------------------");

            // Data Rows
            while (rs.next()) {
                hasData = true;
                System.out.printf("%-10d %-10d %-15d %-6d %-6d %-6d %-10d %-6d %-12d %-12d %-12.2f %-12.2f %-8.2f%n",
                        rs.getInt("tournament_id"),
                        rs.getInt("team_id"),
                        rs.getInt("matches_played"),
                        rs.getInt("wins"),
                        rs.getInt("losses"),
                        rs.getInt("ties"),
                        rs.getInt("no_results"),
                        rs.getInt("points"),
                        rs.getInt("runs_scored"),
                        rs.getInt("runs_conceded"),
                        rs.getDouble("overs_faced"),
                        rs.getDouble("overs_bowled"),
                        rs.getDouble("nrr"));
            }

            // Extra safety check
            if (!hasData) {
                System.out.println("No points data available for Tournament ID: " + tournamentId);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    void setNRR(int tournamentId,int teamId) {
        double nrr=0;
        int runs_scored=0;
        int runs_conceded=0;
        double overs_faced=0;
        double overs_bowled=0;
        String q="SELECT runs_scored,runs_conceded,overs_faced,overs_bowled FROM tournament_points WHERE tournament_id=? AND team_id=?";
        try (PreparedStatement pst=con.prepareStatement(q)){
            pst.setInt(1, tournamentId);
            pst.setInt(2, teamId);
            ResultSet rs=pst.executeQuery();
            if (rs.next()) {
                runs_scored=rs.getInt("runs_scored");
                runs_conceded=rs.getInt("runs_conceded");
                overs_faced=rs.getDouble("overs_faced");
                overs_bowled=rs.getDouble("overs_bowled");
                if (overs_faced > 0 && overs_bowled > 0) {
                    nrr=(runs_scored/overs_faced)-(runs_conceded/overs_bowled);
                }
                q="UPDATE tournament_points SET nrr=? WHERE tournament_id=? AND team_id=?";
                try (PreparedStatement pst2=con.prepareStatement(q)){
                    pst2.setDouble(1, nrr);
                    pst2.setInt(2, tournamentId);
                    pst2.setInt(3, teamId);
                    pst2.executeUpdate();
                }
                catch (Exception e){}
            }
        }
        catch (SQLException se){}
    }
    int getMatchIdsByTeamsAndTournament(Connection con, int teamAId, int teamBId, int tournamentId) throws SQLException {
        String query = "SELECT match_id FROM Matches WHERE teamA_id = ? AND teamB_id = ? AND (tournament_id = ? OR (? IS NULL AND tournament_id IS NULL))";
        List<Integer> matchIds = new ArrayList<>();
        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, teamAId);
            pstmt.setInt(2, teamBId);
            pstmt.setInt(3, tournamentId);
            pstmt.setObject(4, tournamentId == 0 ? null : tournamentId);


            ResultSet rs = pstmt.executeQuery();
            while (rs.next()) {
                matchIds.add(rs.getInt("match_id"));
            }
            if(matchIds.size()==1){
                return matchIds.get(0);
            }
            else if (matchIds.size() > 1){
                System.out.println("There are more than one match played between these 2 teams, please select the correct id: ");
                int i=1;
                for(Integer id : matchIds) {
                    System.out.println(i + ". Match ID: " + id);
                    i++;
                }
                int ch=0;
                while (true) {
                    try {
                        ch=sc.nextInt();
                        if (ch > 0 && ch <= matchIds.size()) break;
                        else System.out.println("Invalid selection.");
                    } catch (InputMismatchException e) {
                        System.out.println("Invalid input. Please enter a number.");
                        sc.nextLine(); // clear buffer
                    }
                }
                return matchIds.get(ch-1);
            }
        }
        return -1;
    }
    public int getMatchIdByTeams(Connection con, int teamAId, int teamBId) {
        System.out.println("called");
        String query = "SELECT match_id, match_date FROM Matches WHERE teamA_id = ? AND teamB_id = ? order by match_date desc limit 1";
        //List<Integer> matchIds = new ArrayList<>();
        S_LinkedList_Int matchIds = new S_LinkedList_Int();
        List<Timestamp> matchDates = new ArrayList<>();

        try (PreparedStatement pstmt = con.prepareStatement(query)) {
            pstmt.setInt(1, teamAId);
            pstmt.setInt(2, teamBId);

            ResultSet rs = pstmt.executeQuery();
            int i = 1;
            while (rs.next()) {
                matchIds.insertAtLast(rs.getInt("match_id"));
                matchDates.add(rs.getTimestamp("match_date"));
//                System.out.println(i + ". Match Date: " + rs.getTimestamp("match_date"));
                i++;
            }

            if (matchIds.isEmpty()) {
                System.out.println("No matches found between the selected teams.");
                return -1;
            } else if (matchIds.size() == 1) {
                return matchIds.findByPosition(0);
            } else {
                System.out.print("Select the correct match (1-" + matchIds.size() + "): ");
                int choice = sc.nextInt();
                while (choice < 1 || choice > matchIds.size()) {
                    System.out.print("Invalid choice. Try again: ");
                    choice = sc.nextInt();
                }
                return matchIds.findByPosition(choice - 1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return -1;
    }

    public Team getTeamA() {
        return teamA;
    }

    public void setTeamA(Team teamA) {
        this.teamA = teamA;
    }

    public Team getTeamB() {
        return teamB;
    }

    public void setTeamB(Team teamB) {
        this.teamB = teamB;
    }

    public int getMatchId() {
        return matchId;
    }
}
