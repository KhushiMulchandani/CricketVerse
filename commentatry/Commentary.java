package commentatry;

public class Commentary{
    String striker;
    String non_stricker;
    String bowler;
    String bowl;
    String[] wagon_wheel={"Third man","Deep_point","Deep cover","Long off","Long on","Deep Mid Wicket","Deep square leg","Deep FIne Leg"};
    // String[][] wagon_wheel={{"Third man","Depence","PUnch","Straight Drive","OFF DRIVE","LOFTED SHOT","HELICOPTER"},{"Deep_point",""},{"Deep cover",""},{"Long off",""},{"Long on",""},{"Deep Mid Wicket",""},{"Deep square leg",""},{"Deep FIne Leg",""}};
    String[] shot_type_long_off={"Depence","PUnch","Straight Drive","OFF DRIVE","LOFTED SHOT","HELICOPTER"};
    String[] shot_type_deep_point={"Depence","LATE CUT","CUT SHOT","BACK FOOT PUNCH","SQUARE DRIVE","SLASH","UPPER CUT","REVERSE SWEEP"};
    String[] shot_type_deep_cover={"Depence","PUnch","Drive","BACK FOOT PUNCH","INSIDE OUT","SWITCH HIT"};
    String[] shot_type_long_on={"Depence","PUnch","Straight Drive","ON DRIVE","LOFTED SHOT","HELICOPTER"};
    String[] shot_type_deep_mid_wicket={"Depence","PUnch","Drive","HELICOPTER","SLOG SWEEP","LOFTED SHOT","PULL","FLICK"};
    String[] shot_type_deep_square_leg={"Depence","PUnch","SWEEP","INSIDE EDGE","PULL","FLICK"};
    String[] shot_type_deep_fine_leg={"DILSCOOP-RAMP SHOT","HOOK","TOP EDGE","INSIDE EDGE","PULL","LEG GLANCE"};
    String[] shot_type_third_man={"TOP EDGE","REVERSE SWEEP","LATE CUT","UPPER CUT","OUTSIDE EDGE","REVERSE SCOOP"};
    public Commentary(String stricker, String non_sticker, String bowler){
        this.bowler=bowler;
        this.non_stricker=non_sticker;
        this.striker=stricker;
    }
    public void no_run(){
        System.out.println(bowler+" to "+striker+", no run");
    }
    public void run(int runs){
        if(runs==4){
            System.out.println(bowler+" to "+striker+", FOUR");
        }
        else if(runs==6){
            System.out.println(bowler+" to "+striker+", SIX");
        }
        else{
            System.out.println(bowler+" to "+striker+", "+runs+" run");
        }
    }
    public void wide(int extra){
        if(extra==0){
            System.out.println(bowler+" to "+striker+", wide");
        }
        else{
            System.out.println(bowler+" to "+striker+", wide, "+extra+" runs");
        }
    }
    public void no_ball(int extra){
        if(extra==0){
            System.out.println(bowler+" to "+striker+", (no ball)");
        }
        else{
            System.out.println(bowler+" to "+striker+", (no ball), "+extra+" runs");
        }
    }
    public void byes(int ch, int runs){
        if(ch==9){
            System.out.println(bowler+" to "+striker+", bye , "+runs+" runs");
        }
        else{
            System.out.println(bowler+" to "+striker+", leg bye , "+runs+" runs");
        }
    }
    public void out(String dismissalType) {
        switch (dismissalType.toLowerCase()) {
            case "bowled":
                System.out.println(bowler + " bowls " + striker + " out bowled");
                break;
            case "caught":
                System.out.println(striker + " is caught out by fielder off " + bowler);
                break;
            case "lbw":
                System.out.println(striker + " is out LBW by " + bowler);
                break;
            case "run out":
                System.out.println(striker + " is run out");
                break;
            case "stumped":
                System.out.println(striker + " is stumped by wicketkeeper off " + bowler);
                break;
            case "hit wicket":
                System.out.println(striker + " is out hit wicket");
                break;
            case "retired hurt":
                System.out.println(striker + " has retired hurt");
                break;
            default:
                System.out.println(striker + " is out (" + dismissalType + ")");
        }
    }

}