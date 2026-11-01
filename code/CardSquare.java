/** CardSquare - draws the top card of its deck (CHANCE or COMMUNITY). AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______ */
public class CardSquare extends Square {
    private final DeckType deck;

    public CardSquare(String name, DeckType deck) {
        super(name);
        this.deck = deck;
    }

    public DeckType getDeck() { return deck; }

    @Override
    public String getType() {
        return deck.name();
    }

    @Override
    public void land(Game game, Player player) {
        game.drawCard(player, deck);
    }
}
