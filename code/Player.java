import java.util.ArrayList;

/**
 * Player - name, cash, position (a Node<Square>, never an index), owned squares,
 * jail state and held JAIL_FREE cards. Implements java.lang.Comparable for the
 * final ranking (java.util.Comparator is not on the allowed list).
 * Position is changed only by Game.movePlayer and Game.sendToJail.
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class Player implements Comparable<Player> {
    public static final int STARTING_CASH = 1500;

    private final String name;
    private final int joinOrder;
    private int cash;
    private Node<Square> position;
    private final ArrayList<Square> owned = new ArrayList<>();
    private int jailAttempts;
    private final ArrayList<Card> jailFreeCards = new ArrayList<>();
    private boolean inJail;
    private boolean bankrupt;
    private boolean salaryPaid;   // set by Board.step during one move
    private boolean movedByCard;  // set when a card moves the player (landOn recursion)

    public Player(String name, int joinOrder, Node<Square> start) {
        this.name = name;
        this.joinOrder = joinOrder;
        this.cash = STARTING_CASH;
        this.position = start;
    }

    public String getName() { return name; }
    public int getJoinOrder() { return joinOrder; }
    public int getCash() { return cash; }
    public void addCash(int delta) { cash += delta; }
    public void setCash(int cash) { this.cash = cash; }
    public Node<Square> getPosition() { return position; }
    public void setPosition(Node<Square> position) { this.position = position; }
    public ArrayList<Square> getOwned() { return owned; }
    public void addOwned(Square s) { owned.add(s); }
    public int getJailAttempts() { return jailAttempts; }
    public void setJailAttempts(int n) { jailAttempts = n; }
    public ArrayList<Card> getJailFreeCards() { return jailFreeCards; }
    public boolean hasJailFreeCard() { return !jailFreeCards.isEmpty(); }
    public void addJailFreeCard(Card c) { jailFreeCards.add(c); }
    /** Uses the oldest JAIL_FREE card held. */
    public Card useJailFreeCard() { return jailFreeCards.remove(0); }
    public boolean isInJail() { return inJail; }
    public void setInJail(boolean b) { inJail = b; }
    public boolean isBankrupt() { return bankrupt; }
    public void setBankrupt(boolean b) { bankrupt = b; }
    public boolean isSalaryPaid() { return salaryPaid; }
    public void setSalaryPaid(boolean b) { salaryPaid = b; }
    public boolean isMovedByCard() { return movedByCard; }
    public void setMovedByCard(boolean b) { movedByCard = b; }

    /**
     * Recursive: sum of purchase prices from index to the end of the list.
     * Base case: index >= owned.size() returns 0.
     * Recursive case: price at index + netWorth(owned, index + 1).
     * Terminates because index grows by 1 each call up to the list size.
     * The caller adds cash.
     */
    public int netWorth(ArrayList<Square> owned, int index) {
        if (index >= owned.size()) {
            return 0;
        }
        return ((OwnableSquare) owned.get(index)).getPrice() + netWorth(owned, index + 1);
    }

    /** Cash + purchase prices; 0 for a bankrupt player (Appendix C.1). */
    public int totalNetWorth() {
        return bankrupt ? 0 : cash + netWorth(owned, 0);
    }

    /** Number of owned squares of a type, e.g. "TRANSIT". Recursive. */
    public int countOwned(String type) {
        return countOwned(type, 0);
    }

    private int countOwned(String type, int index) {
        if (index >= owned.size()) {
            return 0;
        }
        int here = owned.get(index).getType().equals(type) ? 1 : 0;
        return here + countOwned(type, index + 1);
    }

    /** Higher net worth first; ties broken by join order. */
    @Override
    public int compareTo(Player other) {
        int byWorth = Integer.compare(other.totalNetWorth(), this.totalNetWorth());
        return byWorth != 0 ? byWorth : Integer.compare(this.joinOrder, other.joinOrder);
    }

    @Override
    public String toString() {
        return name;
    }
}
