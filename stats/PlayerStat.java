package stats;

import java.sql.Connection;
import java.sql.SQLException;

public abstract class PlayerStat {
    protected int pid;
    protected Connection con;

    public PlayerStat(Connection con, int pid) {
        this.con = con;
        this.pid = pid;
    }

    public abstract void displayStats() throws SQLException;
}