/**
 * Expansion (added for twist T3) - one EXPAND line of the board file: the name
 * of the existing square to insert after, and the new square already built
 * from the rest of the line. EXPAND lines are not part of the starting board;
 * the Board keeps them in a LinkedQueue (file order = FIFO order) until the
 * end of turn 20, when Game.applyExpansions dequeues and inserts them.
 * AI log: Conversation 03, Prompt 03.02 (Claude). Reviewed and understood by: ______
 */
public class Expansion {
    private final String afterName;
    private final Square square;

    public Expansion(String afterName, Square square) {
        this.afterName = afterName;
        this.square = square;
    }

    public String getAfterName() { return afterName; }
    public Square getSquare() { return square; }
}
