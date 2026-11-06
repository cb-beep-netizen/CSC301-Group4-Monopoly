/**
 * Card - one Chance or Community card: id, text, effect, argument (brief Section 5).
 * The deck field lets a used JAIL_FREE card go back to the deck it came from.
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class Card {
    private final DeckType deck;
    private final String id;
    private final String text;
    private final CardEffect effect;
    private final String argument;

    public Card(DeckType deck, String id, String text, CardEffect effect, String argument) {
        this.deck = deck;
        this.id = id;
        this.text = text;
        this.effect = effect;
        this.argument = argument;
    }

    public DeckType getDeck() { return deck; }
    public String getId() { return id; }
    public String getText() { return text; }
    public CardEffect getEffect() { return effect; }
    public String getArgument() { return argument; }

    /** Numeric argument for MOVE_BY, COLLECT, PAY, PAY_EACH, COLLECT_EACH. */
    public int getAmount() {
        return Integer.parseInt(argument);
    }

    @Override
    public String toString() {
        return id + " (" + text + ")";
    }
}
