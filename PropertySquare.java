package monopoly;

class PropertySquare extends Square {
    String color;
    int price;
    int baseRent;
    Player owner;

    public PropertySquare(String name, String color, int price, int baseRent) {
        super(name);
        this.color = color;
        this.price = price;
        this.baseRent = baseRent;
        this.owner = null;
    }

    @Override
    public void land(Game game, Player player) {
        // Implementation handled in Game/Board logic
    }
}