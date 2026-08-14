package stats;

import database_Connection.DatabaseConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class Player {
    private int player_id;
    private final Connection con = DatabaseConnection.getConnection();
    public String name;
    String gender;
    String playing_role;
    String batting_style;
    String bowling_style;

    private List<PlayerStat> statsList = new ArrayList<>();

    public Player(int pid) {
        this.player_id = pid;
        initStats();
    }
    public Player(int player_id, String name, String gender, String playing_role, String batting_style, String bowling_style) {
        this.player_id = player_id;
        this.name = name;
        this.gender = gender;
        this.playing_role = playing_role;
        this.batting_style = batting_style;
        this.bowling_style = bowling_style;

        initStats();
    }

    private void initStats() {
        statsList.add(new BattingStats(con, player_id));
        statsList.add(new BowlingStats(con, player_id));
        statsList.add(new FieldingStats(con, player_id));
        statsList.add(new CaptainStats(con, player_id));
    }
    public int getId() {
        return player_id;
    }

    public void showStats() throws SQLException {
        Scanner sc = new Scanner(System.in);
        int choice;
        do {
            System.out.println("\n1. Batting Stats");
            System.out.println("2. Bowling Stats");
            System.out.println("3. Fielding Stats");
            System.out.println("4. Captain Stats");
            System.out.println("5. Exit");
            System.out.print("Enter your choice: ");
            choice = sc.nextInt();

            if (choice >= 1 && choice <= 4) {
                statsList.get(choice - 1).displayStats();
            } else if (choice != 5) {
                System.out.println("Invalid choice.");
            }
        } while (choice != 5);
    }
}
