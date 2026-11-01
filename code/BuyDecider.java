/**
 * BuyDecider (added) - decides whether a player buys an unowned square
 * (brief 3.4: "The player decides whether to buy").
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public interface BuyDecider {
    boolean wantsToBuy(Player player, OwnableSquare square);
}
