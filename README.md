# CricketVerse

CricketVerse is a Java-based digital cricket scoring application designed to automate match recording and statistics management. It provides a structured, queryable system to replace manual scorekeeping, allowing users to track ball-by-ball match data and maintain persistent career statistics for players and teams.

## Purpose
The application streamlines cricket match management by:
* Recording matches ball-by-ball.
* Automatically calculating and updating player career statistics (batting averages, strike rates, bowling economy, wickets, etc.).
* Maintaining a permanent, queryable history of matches, players, and teams in a database.

## Features
* **Live Match Scoring**: Record runs, extras, and wickets via a command-line interface.
* **Automatic Stat Calculation**: Instant updates to career stats for every player involved.
* **Database Persistence**: All data is securely stored using a relational database.
* **Match Lifecycle Management**: Handles everything from toss and innings to winner determination and captaincy record updates.

## Technical Stack
* **Programming Language**: Java
* **Database**: MariaDB / MySQL
* **API/Framework**: JDBC (Java Database Connectivity)
* **IDE**: IntelliJ IDEA

## Data Structures
* **HashMap**: Tracks in-match performance, such as runs scored by batsmen and wickets taken by bowlers.
* **HashSet**: Tracks players to prevent overcounting during innings.
* **ArrayList/List**: Manages collections of players for team rosters and selections.



## How to Run

Follow these steps to set up and run CricketVerse locally on your machine:

## Prerequisites & Dependencies
* **Java Development Kit (JDK):** Version 8 or higher.
* **Database:** MariaDB or MySQL.
* **IDE:** IntelliJ IDEA (or any Java-compatible IDE).
* **Dependencies:**
    * **JDBC Driver:** MySQL Connector/J driver to enable communication between Java and the database. Ensure this is added to your project's build path/dependencies.

## Database Setup
CricketVerse uses MariaDB/MySQL for data persistence.
1. Install MariaDB/MySQL on your local machine.
2. Create a new database named `cricketverse_db`.
3. Configuration
* Open the `database_Connection/DatabaseConnection.java` file.
* Update the connection string, username, and password to match your local database configuration:
  ```java
  String url = "jdbc:mysql://localhost:3306/cricketverse_db";
  String user = "your_username";
  String password = "your_password";

4. The application includes a `createTables` method that automatically initializes the required tables upon first run:
    * `user`, `PlayerProfile`, `Team`, `Team_Players`, `Tournament`, `Tournament_Teams`, `Matches`, `batting_stats`, `bowling_stats`, `fielding_stats`, `captain_stats`, `Score`, `Tournament_Round`, `Tournament_Points`, and `Scoreboard`.
   
## Compiling and Running the Project
Option A: Using an IDE (Recommended, e.g., IntelliJ IDEA)
Open the project folder inside your Java IDE[cite: 4].

Add the MySQL JDBC Driver JAR to your project's module dependencies or build path.

Locate the main entry point file: CricketVerse.java[cite: 2].

Run or Shift+F10 on CricketVerse.java to launch the command-line interface.

Option B: Using the Command Line (Terminal / Command Prompt)
Open your terminal and navigate to the root directory of the project source code.

Compile all the Java files together using javac:

Bash
javac -cp "path/to/mysql-connector-j.jar" CricketVerse.java
Run the application, ensuring you include both the current directory and the JDBC driver path in the classpath:

Bash
java -cp ".;path/to/mysql-connector-j.jar" CricketVerse
(Note: Use : instead of ; as a classpath separator on Linux/macOS).