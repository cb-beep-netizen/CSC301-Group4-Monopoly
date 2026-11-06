/** TaxSquare - the player pays a fixed amount to the bank. AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______ */
public class TaxSquare extends Square {
    private final int amount;

    public TaxSquare(String name, int amount) {
        super(name);
        this.amount = amount;
    }

    public int getAmount() { return amount; }

    @Override
    public String getType() {
        return "TAX";
    }

    @Override
    public void land(Game game, Player player) {
        game.payTax(player, amount);
    }
}
