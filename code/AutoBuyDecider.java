/**
 * AutoBuyDecider - buys whenever the player can afford it. Used with --auto
 * for scripted test games (pending instructor confirmation, design note Q2).
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class AutoBuyDecider implements BuyDecider {
    @Override
    public boolean wantsToBuy(Player player, OwnableSquare square) {
        return player.getCash() >= square.getPrice();
    }
}
