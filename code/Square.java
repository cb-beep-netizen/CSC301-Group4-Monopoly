/**
 * Square (abstract) - one board square. Each subclass decides what happens
 * when a player lands on it, through land(Game, Player).
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public abstract class Square {
    private final String name;

    protected Square(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    /** Square type as written in the board file, e.g. "PROPERTY", "TRANSIT", "JAIL". */
    public abstract String getType();

    /** Resolves a landing by player on this square. */
    public abstract void land(Game game, Player player);

    @Override
    public String toString() {
        return name;
    }
}
