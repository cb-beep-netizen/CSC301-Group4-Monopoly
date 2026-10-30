package monopoly;

public class CardSquare extends Square {
    public String deckType; // "CHANCE" or "COMMUNITY"

    public CardSquare(String name, String deckType) {
        super(name);
        this.deckType = deckType;
    }

    @Override
    public void land(Game game, Player player) {
        // Handled in Game/Card execution logic
    }
}