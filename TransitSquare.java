package monopoly;

class TransitSquare extends Square {
    int price;
    Player owner;

    public TransitSquare(String name, int price) {
        super(name);
        this.price = price;
        this.owner = null;
    }

    @Override
    public void land(Game game, Player player) {}

    // Mandatory recursive transit rent computation: 25 * 2^(owned - 1)
    public int transitRent(int owned) {
        if (owned <= 0) return 0;
        return 25 * (int) powerOfTwo(owned - 1);
    }

    // Helper recursive method for powers of 2 (zero loops)
    private int powerOfTwo(int exp) {
        if (exp == 0) return 1;
        return 2 * powerOfTwo(exp - 1);
    }
}