import java.util.Scanner;

/**
 * ConsoleBuyDecider - asks the player on the console (Y/N). Re-asks
 * recursively on invalid input, so no loop runs inside landOn().
 * End of input counts as "no".
 * AI log: Conversation 01, Prompt 01.13, updated in Prompt 01.14 (Claude). Reviewed and understood by: ______
 */
public class ConsoleBuyDecider implements BuyDecider {
    private final Scanner in;

    public ConsoleBuyDecider(Scanner in) {
        this.in = in;
    }

    @Override
    public boolean wantsToBuy(Player player, OwnableSquare square) {
        String what = square.getType().equals("PROPERTY")
                ? ((PropertySquare) square).getColor() + " property, base rent " + ((PropertySquare) square).getBaseRent()
                : "transit line, rent 25 / 50 / 100";
        System.out.println();
        System.out.println("  " + player.getName() + " is on " + square.getName() + " - unowned " + what + ".");
        System.out.print("  > Buy it for " + square.getPrice() + " AED? Cash " + player.getCash() + " -> "
                + (player.getCash() - square.getPrice()) + "  [Y/N]: ");
        return ask();
    }

    private boolean ask() {
        if (!in.hasNextLine()) {
            System.out.println();
            return false;
        }
        String answer = in.nextLine().trim().toUpperCase();
        if (answer.equals("Y") || answer.equals("YES")) {
            return true;
        }
        if (answer.equals("N") || answer.equals("NO")) {
            return false;
        }
        System.out.print("  > Please type Y or N: ");
        return ask();
    }
}
