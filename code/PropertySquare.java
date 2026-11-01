/**
 * PropertySquare - colour, price, base rent, owner. Rent is the base rent,
 * doubled when the owner holds every PROPERTY of the colour (Board.ownsGroup).
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class PropertySquare extends OwnableSquare {
    private final String color;
    private final int rent;

    public PropertySquare(String name, String color, int price, int rent) {
        super(name, price);
        this.color = color;
        this.rent = rent;
    }

    public String getColor() { return color; }
    public int getBaseRent() { return rent; }

    @Override
    public String getType() {
        return "PROPERTY";
    }

    @Override
    public int rent(Board board) {
        Player owner = getOwner();
        if (owner == null) {
            return 0;
        }
        boolean fullGroup = board.ownsGroup(board.getHead(), board.getHead(), color, owner);
        return fullGroup ? rent * 2 : rent;
    }
}
