package stats;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BowlingStats extends PlayerStat{
    int pid;
    PreparedStatement pst;
    public BowlingStats(Connection con,int pid) {
        super(con,pid);
        this.pid = pid;
    }
    public void match() throws SQLException {
        pst = con.prepareStatement("update bowling_stats set matches = matches+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void Innings() throws SQLException {
        pst = con.prepareStatement("update bowling_stats set Innings = Innings+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void maidens() throws SQLException{
        pst = con.prepareStatement("update bowling_stats set maidens = maidens+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void wickets() throws SQLException{
        pst = con.prepareStatement("update bowling_stats set wickets = wickets+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void wides() throws SQLException{
        pst = con.prepareStatement("update bowling_stats set wides = wides+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void NoBalls() throws SQLException{
        pst = con.prepareStatement("update bowling_stats set no_balls = no_balls+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void DotBall() throws SQLException{
        pst = con.prepareStatement("update bowling_stats set dot_balls = dot_balls+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void fours() throws SQLException {
        pst = con.prepareStatement("update bowling_stats set 4s_conceded = 4s_conceded+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void sixes() throws SQLException {
        pst = con.prepareStatement("update bowling_stats set 6s_conceded = 6s_conceded+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void overs() throws SQLException{
        pst = con.prepareStatement("update bowling_stats set balls = balls+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void FiveW() throws SQLException{
        pst = con.prepareStatement("update bowling_stats set 5_wickets_haul = 5_wickets_haul+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void economy() throws SQLException {
        pst = con.prepareStatement("SELECT runs, balls FROM bowling_stats WHERE pid = ?");
        pst.setInt(1, pid);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            int runs = rs.getInt("runs");
            int balls = rs.getInt("balls");
            double economy = 0.0;
            if (balls > 0) {
                double overs = balls / 6.0;
                economy = runs / overs;
            }
            pst = con.prepareStatement("UPDATE bowling_stats SET economy = ? WHERE pid = ?");
            pst.setDouble(1, economy);
            pst.setInt(2, pid);
            pst.executeUpdate();
        }
    }
    public void BowlingStrikeRate() throws SQLException {
        pst = con.prepareStatement("select balls,wickets from bowling_stats where pid = ?");
        pst.setInt(1, pid);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            //double overs = rs.getDouble(1);
            int wickets = rs.getInt(2);
            int balls = rs.getInt(1);
            double sr = (wickets > 0) ? (double) balls / wickets : 0;
            pst = con.prepareStatement("update bowling_stats set sr = ? where pid = ?");
            pst.setDouble(1, sr);
            pst.setInt(2, pid);
            pst.executeUpdate();
        }
    }
    public void BowlingAverage() throws SQLException {
        pst = con.prepareStatement("select runs,wickets from bowling_stats where pid = ?");
        pst.setInt(1, pid);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            int runs = rs.getInt(1);
            int wickets = rs.getInt(2);
            double avg = (wickets > 0) ? (double) runs / wickets : 0;
            pst = con.prepareStatement("update bowling_stats set avg = ? where pid = ?");
            pst.setDouble(1, avg);
            pst.setInt(2, pid);
            pst.executeUpdate();
        }
    }
    public void runsConceded(int runs) throws SQLException {
        pst = con.prepareStatement("update bowling_stats set runs = runs + ? where pid = ?");
        pst.setInt(1, runs);
        pst.setInt(2, pid);
        pst.executeUpdate();
    }
    public void updateBestBowling(int currentWickets, int currentRuns) throws SQLException {
        pst = con.prepareStatement("SELECT best_bowling FROM bowling_stats WHERE pid = ?");
        pst.setInt(1, pid);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            String b = rs.getString("best_bowling");
            int bestWickets = 0, bestRuns = 0;
            if (b != null && b.contains("/")) {
                String[] parts = b.split("/");
                bestWickets = Integer.parseInt(parts[0]);
                bestRuns = Integer.parseInt(parts[1]);
            }
            boolean flag = false;
            if (currentWickets > bestWickets) {
                flag = true;
            } else if (currentWickets == bestWickets && currentRuns < bestRuns) {
                flag = true;
            }
            if (flag) {
                String best = currentWickets + "/" + currentRuns;
                pst = con.prepareStatement("update bowling_stats set best_bowling = ? where pid = ?");
                pst.setString(1, best);
                pst.setInt(2, pid);
                pst.executeUpdate();
            }
        }
    }

    @Override
    public void displayStats() throws SQLException {
        pst = con.prepareStatement("select * from bowling_stats where pid=?");
        pst.setInt(1,pid);
        ResultSet rs = pst.executeQuery();
        System.out.println("------- Bowling Stats -------");
        while (rs.next()) {
            //System.out.println("Player ID     : " + rs.getInt(1));
            System.out.println("Matches          : " + rs.getInt(2));
            System.out.println("Innings          : " + rs.getInt(3));
            System.out.println("overs            : " + rs.getDouble(4));
            System.out.println("Maidens          : " + rs.getInt(5));
            System.out.println("Wickets          : " + rs.getInt(6));
            System.out.println("Runs             : " + rs.getInt(7));
            System.out.println("Best Bowling     : " + rs.getString(8));
            System.out.println("5 Wickets Haul   : " + rs.getInt(9));
            System.out.println("Economy          : " + rs.getDouble(10));
            System.out.println("Strike Rate      : " + rs.getDouble(11));
            System.out.println("Average          : " + rs.getDouble(12));
            System.out.println("Wides            : " + rs.getInt(13));
            System.out.println("No Balls         : " + rs.getInt(14));
            System.out.println("Dot Balls        : " + rs.getInt(15));
            System.out.println("Fours (4s)       : " + rs.getInt(16));
            System.out.println("Sixes (6s)       : " + rs.getInt(17));
            System.out.println("-----------------------------------");
        }
    }
}
