package stats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BattingStats extends PlayerStat {
    int pid;
    PreparedStatement pst;
    public BattingStats(Connection con,int pid) {
        super(con,pid);
        this.pid = pid;
    }
    public void match() throws SQLException {
        pst = con.prepareStatement("update batting_stats set matches = matches+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void Innings() throws SQLException {
        pst = con.prepareStatement("update batting_stats set Innings = Innings+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void run(int runs) throws SQLException{
        pst = con.prepareStatement("update batting_stats set Runs = Runs+? where pid=?");
        pst.setInt(2,pid);
        pst.setInt(1,runs);
        pst.executeUpdate();
    }
    public void thirties() throws SQLException {
        pst = con.prepareStatement("update batting_stats set thirties = thirties+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void fifties() throws SQLException {
        pst = con.prepareStatement("update batting_stats set fifties = fifties+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void hundred() throws SQLException {
        pst = con.prepareStatement("update batting_stats set hundred = hundred+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void fours() throws SQLException {
        pst = con.prepareStatement("update batting_stats set fours = fours+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void sixes() throws SQLException {
        pst = con.prepareStatement("update batting_stats set sixes = sixes+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void Ducks() throws SQLException {
        pst = con.prepareStatement("update batting_stats set Ducks = Ducks+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void Average() throws SQLException {
        pst = con.prepareStatement("SELECT Innings, Not_outs, Runs FROM batting_stats WHERE pid = ?");
        pst.setInt(1, pid);
        ResultSet rs = pst.executeQuery();

        if (rs.next()) {
            int innings = rs.getInt("Innings");
            int notOuts = rs.getInt("Not_outs");
            int runs = rs.getInt("Runs");

            double avg;
            if ((innings - notOuts) > 0) {
                avg = (double) runs / (innings - notOuts);
            } else {
                avg = runs;  // or 0.0 if you prefer
            }

            pst = con.prepareStatement("UPDATE batting_stats SET avg = ? WHERE pid = ?");
            pst.setDouble(1, avg);
            pst.setInt(2, pid);
            pst.executeUpdate();
        }
    }
    public void Highestruns(int runs) throws SQLException {
        pst = con.prepareStatement("SELECT highest_runs FROM batting_stats WHERE pid = ?");
        pst.setInt(1, pid);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            int currentHigh = rs.getInt(1);
            if (runs > currentHigh) {
                pst = con.prepareStatement("update batting_stats set highest_runs = ? WHERE pid = ?");
                pst.setInt(1, runs);
                pst.setInt(2, pid);
                pst.executeUpdate();
            }
        }
    }
    public void NO() throws SQLException{
        pst = con.prepareStatement("update batting_stats set not_outs = not_outs+1 where pid=? ");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void balls() throws SQLException{
        pst = con.prepareStatement("update batting_stats set Balls = Balls+1 where pid=? ");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void Strikerate() throws SQLException {
        pst = con.prepareStatement("select runs,balls from batting_stats WHERE pid = ?");
        pst.setInt(1, pid);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            int runs = rs.getInt(1);
            int balls = rs.getInt(2);
            double sr = (balls > 0) ? (runs * 100.0) / balls : 0.0;
            pst = con.prepareStatement("update batting_stats set sr = ? WHERE pid = ?");
            pst.setDouble(1, sr);
            pst.setInt(2, pid);
            pst.executeUpdate();
        }
    }
    public void displayStats() throws SQLException{
        pst = con.prepareStatement("select * from batting_stats where pid=?");
        pst.setInt(1,pid);
        ResultSet rs = pst.executeQuery();
        System.out.println("------- Batting Stats -------");
        while (rs.next()) {
            //System.out.println("Player ID     : " + rs.getInt(1));
            System.out.println("Matches       : " + rs.getInt(2));
            System.out.println("Innings       : " + rs.getInt(3));
            System.out.println("Not Outs      : " + rs.getInt(4));
            System.out.println("Highest Runs  : " + rs.getInt(5));
            System.out.println("Runs          : " + rs.getInt(6));
            System.out.println("Balls         : " + rs.getInt(7));
            System.out.println("Average       : " + rs.getDouble(8));
            System.out.println("Strike Rate   : " + rs.getDouble(9));
            System.out.println("30s           : " + rs.getInt(10));
            System.out.println("50s           : " + rs.getInt(11));
            System.out.println("100s          : " + rs.getInt(12));
            System.out.println("Fours (4s)    : " + rs.getInt(13));
            System.out.println("Sixes (6s)    : " + rs.getInt(14));
            System.out.println("Ducks         : " + rs.getInt(15));
            System.out.println("-----------------------------------");
        }}
}

