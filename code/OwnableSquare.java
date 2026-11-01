/**
 * OwnableSquare (abstract, added by the group) - price and owner shared by
 * PropertySquare and TransitSquare. An owner of null means the bank owns it.
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public abstract class OwnableSquare extends Square {
    private final int price;
    private Player owner;

    protected OwnableSquare(String name, int price) {
        super(name);
        this.price = price;
    }

    public int getPrice() { return price; }
    public Player getOwner() { return owner; }
    public void setOwner(Player owner) { this.owner = owner; }

    /** Rent owed by a visitor, given the current owner. */
    public abstract int rent(Board board);

    @Override
    public void land(Game game, Player player) {
        game.landOnOwnable(player, this);
    }
}
