/**
 * CornerSquare - GO, JAIL (just visiting), FREE or GO_TO_JAIL.
 * Salary for GO is paid while moving (Board.step), not on landing.
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class CornerSquare extends Square {
    private final CornerType kind;

    public CornerSquare(String name, CornerType kind) {
        super(name);
        this.kind = kind;
    }

    public CornerType getKind() { return kind; }

    @Override
    public String getType() {
        return kind.name();
    }

    @Override
    public void land(Game game, Player player) {
        if (kind == CornerType.GO_TO_JAIL) {
            game.sendToJail(player);
        }
        // GO, JAIL (just visiting) and FREE: nothing happens.
    }
}
