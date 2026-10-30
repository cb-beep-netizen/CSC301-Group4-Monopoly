package monopoly;

class TaxSquare extends Square {
    int amount;
    public TaxSquare(String name, int amount) {
        super(name);
        this.amount = amount;
    }
    @Override
    public void land(Game game, Player player) {}
}