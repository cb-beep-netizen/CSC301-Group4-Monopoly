/**
 * TransitSquare - price, owner. Rent = 25 x 2^(n - 1), n = TRANSIT squares the owner holds.
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class TransitSquare extends OwnableSquare {

    public TransitSquare(String name, int price) {
        super(name, price);
    }

    @Override
    public String getType() {
        return "TRANSIT";
    }

    @Override
    public int rent(Board board) {
        Player owner = getOwner();
        if (owner == null) {
            return 0;
        }
        return transitRent(owner.countOwned("TRANSIT"));
    }

    /**
     * Recursive: 25 x 2^(owned - 1).
     * Base case: owned <= 1 returns 25. Recursive case: 2 x transitRent(owned - 1).
     * Terminates because owned decreases by 1 each call and reaches 1.
     */
    public int transitRent(int owned) {
        if (owned <= 1) {
            return 25;
        }
        return 2 * transitRent(owned - 1);
    }
}
