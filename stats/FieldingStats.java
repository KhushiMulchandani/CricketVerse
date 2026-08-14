package stats;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class FieldingStats extends PlayerStat{
    int pid;
    PreparedStatement pst;
    public FieldingStats(Connection con,int pid) {
        super(con,pid);
        this.pid = pid;
    }
    public void match() throws SQLException {
        pst = con.prepareStatement("update fielding_stats set matches = matches+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void catches() throws SQLException {
        pst = con.prepareStatement("update fielding_stats set catches = catches+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void Runout() throws SQLException {
        pst = con.prepareStatement("update fielding_stats set run_outs = run_outs+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void caughtbehind() throws SQLException {
        pst = con.prepareStatement("update fielding_stats set caught_behind = caught_behind +1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }
    public void stumping() throws SQLException{
        pst = con.prepareStatement("update fielding_stats set stumping = stumping+1 where pid=?");
        pst.setInt(1,pid);
        pst.executeUpdate();
    }

    @Override
    public void displayStats() throws SQLException {
        pst = con.prepareStatement("select * from fielding_stats where pid=?");
        pst.setInt(1,pid);
        ResultSet rs = pst.executeQuery();
        System.out.println("------- Fielding Stats -------");
        while(rs.next()) {
            System.out.println("Matches          : " + rs.getInt(2));
            System.out.println("Catches          : " + rs.getInt(3));
            System.out.println("Run Outs         : " + rs.getInt(4));
            System.out.println("Caught Behind    : " + rs.getInt(5));
            System.out.println("Stumping         : " + rs.getInt(6));
        }
    }
}

