package monopoly;

public class GameSnapshot {
    String playerName;
    int cash;
    int turnCount;

    public GameSnapshot(String playerName, int cash, int turnCount) {
        this.playerName = playerName;
        this.cash = cash;
        this.turnCount = turnCount;
    }
}

