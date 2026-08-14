package stats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class CaptainStats extends PlayerStat{
    int pid;
    PreparedStatement pst;
    public CaptainStats(Connection con,int pid) {
        super(con,pid);
        this.pid = pid;
    }
    public void Cmatches() throws SQLException {
        pst = con.prepareStatement("update captain_stats set matches_as_captain = matches_as_captain+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void tosswon() throws SQLException{
        pst = con.prepareStatement("update captain_stats set toss_won = toss_won+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void WinPer(boolean result)throws SQLException{
        if (result){
            pst = con.prepareStatement("update captain_stats set matches_won = matches_won+1 where pid=?");
        }
        else {
            pst = con.prepareStatement("update captain_stats set matches_lost = matches_lost+1 where pid=?");
        }
        pst.setInt(1, pid);
        pst.executeUpdate();
        pst = con.prepareStatement("SELECT matches_as_captain,matches_won, matches_lost FROM captain_stats WHERE pid = ?");
        pst.setInt(1, pid);
        ResultSet rs = pst.executeQuery();
        if (rs.next()) {
            int matches = rs.getInt("matches_as_captain");
            int wonMatches = rs.getInt("matches_won");
            int lostMatches = rs.getInt("matches_lost");
            double winPer = (wonMatches * 100.0) / matches;
            double lossPer = (lostMatches * 100.0) / matches;
            pst = con.prepareStatement("UPDATE captain_stats SET win_percentage = ?, loss_percentage = ? WHERE pid = ?");
            pst.setDouble(1, winPer);
            pst.setDouble(2, lossPer);
            pst.setInt(3, pid);
            pst.executeUpdate();
        }
    }

    @Override
    public void displayStats() throws SQLException {
            pst = con.prepareStatement("select * from captain_stats where pid=?");
            pst.setInt(1,pid);
            ResultSet rs = pst.executeQuery();
            System.out.println("------- Captain Stats -------");
            while(rs.next()){
                System.out.println("Matches          : " + rs.getInt(2));
                System.out.println("Toss Won         : " + rs.getInt(3));
                System.out.println("Matches Won         : " + rs.getInt(4));
                System.out.println("Matches Loss         : " + rs.getInt(5));
                System.out.println("Win Per%         : " + rs.getDouble(6));
                System.out.println("Loss per%        : " + rs.getDouble(7));
            }

    }
}

