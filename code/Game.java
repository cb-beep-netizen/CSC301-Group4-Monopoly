import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

/**
 * Game - the board, the turn queue, the two card decks and the game loop.
 * Implements the rules of brief Section 3 and writes the Appendix C log.
 * Required recursive methods here: takeTurn and landOn (Section 5.1). Every
 * helper they call is recursive or O(1).
 * AI log: Conversation 01, Prompt 01.13, updated in Prompt 01.14 (Claude). Reviewed and understood by: ______
 */
public class Game {
    public static final int DEFAULT_MAX_TURNS = 60;
    public static final int JAIL_FINE = 50;
    public static final int MAX_JAIL_ATTEMPTS = 3;
    public static final int MIN_PLAYERS = 2;
    public static final int MAX_PLAYERS = 6;

    private final Board board;
    private final LinkedQueue<Player> turnQueue = new LinkedQueue<>();
    private final LinkedQueue<Card> chance = new LinkedQueue<>();
    private final LinkedQueue<Card> community = new LinkedQueue<>();
    private final ArrayList<Player> players = new ArrayList<>();
    private final ArrayList<Player> bankruptOrder = new ArrayList<>();
    private final Dice dice;
    private final GameLogger logger;
    private final BuyDecider decider;
    private final int maxTurns;
    private final ArrayList<Card> allCards;   // file order, used by the board view for card texts
    private int turnCount;
    private String endReason;
    private boolean finished;

    /**
     * cards: the card list in file order. If shuffle is true each deck is
     * shuffled once with rng (CHANCE first, then COMMUNITY) before queueing.
     */
    public Game(Board board, ArrayList<Card> cards, Random rng, boolean shuffle,
                Dice dice, GameLogger logger, BuyDecider decider, int maxTurns) {
        this.board = board;
        this.dice = dice;
        this.logger = logger;
        this.decider = decider;
        this.maxTurns = maxTurns;
        this.allCards = new ArrayList<>(cards);
        ArrayList<Card> chanceList = new ArrayList<>();
        ArrayList<Card> communityList = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            if (c.getDeck() == DeckType.CHANCE) {
                chanceList.add(c);
            } else {
                communityList.add(c);
            }
        }
        if (shuffle) {
            Collections.shuffle(chanceList, rng);
            Collections.shuffle(communityList, rng);
        }
        for (int i = 0; i < chanceList.size(); i++) {
            chance.enqueue(chanceList.get(i));
        }
        for (int i = 0; i < communityList.size(); i++) {
            community.enqueue(communityList.get(i));
        }
    }

    // ------------------------------------------------------------------ setup

    /** Adds a player on GO at the back of the turn queue (turn order = join order). */
    public Player addPlayer(String name) {
        if (players.size() >= MAX_PLAYERS) {
            throw new IllegalStateException("A game has at most " + MAX_PLAYERS + " players");
        }
        Player p = new Player(name, players.size() + 1, board.getGo());
        players.add(p);
        turnQueue.enqueue(p);
        return p;
    }

    // ------------------------------------------------------------- game loop

    /** Plays turns until the game ends, then prints END and the ranking. */
    public void play() {
        while (!isOver()) {
            playOneTurn();
        }
        finish();
    }

    public boolean isOver() {
        return endReason != null;
    }

    /** One turn: dequeue, play (jail rules first), re-enqueue unless bankrupt, check the end. */
    public void playOneTurn() {
        if (players.size() < MIN_PLAYERS) {
            throw new IllegalStateException("A game needs at least " + MIN_PLAYERS + " players");
        }
        if (isOver()) {
            return;
        }
        Player p = turnQueue.dequeue();
        turnCount++;
        logger.log("TURN", String.valueOf(turnCount), p.getName());
        if (p.isInJail()) {
            jailTurn(p);
        } else {
            takeTurn(p, 0);
        }
        if (!p.isBankrupt()) {
            turnQueue.enqueue(p);
        }
        if (turnQueue.size() <= 1) {
            endReason = "LAST_PLAYER";
        } else if (turnCount >= maxTurns) {
            endReason = "MAX_TURNS";
        }
    }

    /** Prints END and one RANK line per player (once). */
    public void finish() {
        if (finished || endReason == null) {
            return;
        }
        finished = true;
        logger.log("END", endReason);
        ArrayList<Player> ranking = rank();
        for (int i = 0; i < ranking.size(); i++) {
            Player p = ranking.get(i);
            logger.log("RANK", String.valueOf(i + 1), p.getName(), String.valueOf(p.totalNetWorth()));
        }
    }

    /**
     * Required recursive method. Plays one roll; calls itself after a double.
     * Base cases: third double (doublesCount == 2 before this roll) -> Jail,
     * no move; player jailed or bankrupt after landing; roll not a double.
     * Recursive case: a double -> takeTurn(p, doublesCount + 1).
     * Terminates because doublesCount rises by 1 per call and the third double
     * always stops the recursion.
     */
    public void takeTurn(Player p, int doublesCount) {
        int total = dice.roll();
        boolean isDouble = dice.isDouble();
        logger.log("ROLL", p.getName(), String.valueOf(dice.getD1()), String.valueOf(dice.getD2()));
        if (isDouble && doublesCount == 2) {
            sendToJail(p);
            return;
        }
        movePlayer(p, total);
        landOn(p);
        if (p.isBankrupt() || p.isInJail()) {
            return;
        }
        if (isDouble) {
            takeTurn(p, doublesCount + 1);
        }
    }

    /**
     * Required recursive method. Resolves the square the player stands on and
     * calls itself when a card moved the player to another square.
     * Base case: the square did not move the player by a card, or the player
     * is now in Jail or bankrupt.
     * Recursive case: a card moved the player -> landOn(p) on the new square.
     * Terminates because each recursive call needs a card move, and the card
     * file is checked so that no card move ends on a CHANCE or COMMUNITY
     * square: depth is at most 2.
     */
    public void landOn(Player p) {
        p.setMovedByCard(false);
        p.getPosition().data.land(this, p);
        if (p.isMovedByCard() && !p.isBankrupt() && !p.isInJail()) {
            landOn(p);
        }
    }

    // ------------------------------------------------------------ movement

    /** Moves p by steps (negative = backward); logs MOVE, then SALARY if GO was passed. */
    public void movePlayer(Player p, int steps) {
        Node<Square> from = p.getPosition();
        p.setSalaryPaid(false);
        Node<Square> to = board.step(from, steps, p);
        p.setPosition(to);
        logger.log("MOVE", p.getName(), from.data.getName(), to.data.getName());
        if (p.isSalaryPaid()) {
            logger.log("SALARY", p.getName(), String.valueOf(p.getCash()));
            p.setSalaryPaid(false);
        }
    }

    /** Directly to Jail: no move along the board, no salary; ends the turn. */
    public void sendToJail(Player p) {
        p.setPosition(board.getJail());
        p.setInJail(true);
        p.setJailAttempts(0);
        logger.log("JAIL", p.getName());
    }

    /** Start of a jailed player's turn (brief 3.6). */
    private void jailTurn(Player p) {
        if (p.hasJailFreeCard()) {
            Card card = p.useJailFreeCard();
            logger.log("JAIL_FREE", p.getName());
            deckFor(card.getDeck()).enqueue(card);
            p.setInJail(false);
            p.setJailAttempts(0);
            takeTurn(p, 0);
            return;
        }
        int total = dice.roll();
        String d1 = String.valueOf(dice.getD1());
        String d2 = String.valueOf(dice.getD2());
        if (dice.isDouble()) {
            logger.log("JAIL_ROLL", p.getName(), d1, d2, "RELEASED");
            p.setInJail(false);
            p.setJailAttempts(0);
            movePlayer(p, total);
            landOn(p);               // this double does not earn another roll
            return;
        }
        p.setJailAttempts(p.getJailAttempts() + 1);
        if (p.getJailAttempts() < MAX_JAIL_ATTEMPTS) {
            logger.log("JAIL_ROLL", p.getName(), d1, d2, "STAY");
            return;
        }
        logger.log("JAIL_ROLL", p.getName(), d1, d2, "FINE");
        payBank(p, JAIL_FINE);
        if (p.isBankrupt()) {
            return;
        }
        p.setInJail(false);
        p.setJailAttempts(0);
        movePlayer(p, total);
        landOn(p);
    }

    // ------------------------------------------------- squares and payments

    /** PROPERTY / TRANSIT landing: offer to buy, pay rent, or nothing if own square. */
    public void landOnOwnable(Player p, OwnableSquare s) {
        Player owner = s.getOwner();
        if (owner == null) {
            if (p.getCash() >= s.getPrice() && decider.wantsToBuy(p, s)) {
                p.addCash(-s.getPrice());
                s.setOwner(p);
                p.addOwned(s);
                logger.log("BUY", p.getName(), s.getName(), String.valueOf(p.getCash()));
            }
            return;
        }
        if (owner == p) {
            return;
        }
        payRent(p, owner, s.rent(board));
    }

    /** Rent is collected even when the owner is in Jail. */
    private void payRent(Player payer, Player owner, int amount) {
        int paid = Math.min(amount, payer.getCash());
        payer.addCash(-paid);
        owner.addCash(paid);
        logger.log("RENT", payer.getName(), owner.getName(), String.valueOf(paid),
                String.valueOf(payer.getCash()), String.valueOf(owner.getCash()));
        if (paid < amount) {
            declareBankrupt(payer, owner);
        }
    }

    public void payTax(Player p, int amount) {
        int paid = Math.min(amount, p.getCash());
        p.addCash(-paid);
        logger.log("TAX", p.getName(), String.valueOf(paid), String.valueOf(p.getCash()));
        if (paid < amount) {
            declareBankrupt(p, null);
        }
    }

    /** Card PAY and the Jail fine: pays the bank and prints a CASH line. */
    private void payBank(Player p, int amount) {
        int paid = Math.min(amount, p.getCash());
        p.addCash(-paid);
        logger.log("CASH", p.getName(), "-" + paid, String.valueOf(p.getCash()));
        if (paid < amount) {
            declareBankrupt(p, null);
        }
    }

    // --------------------------------------------------------------- cards

    private LinkedQueue<Card> deckFor(DeckType type) {
        return type == DeckType.CHANCE ? chance : community;
    }

    /** Dequeues the top card, prints CARD, applies it. */
    public void drawCard(Player p, DeckType type) {
        LinkedQueue<Card> deck = deckFor(type);
        if (deck.isEmpty()) {
            return; // only possible if every card of the deck is a held JAIL_FREE card
        }
        Card card = deck.dequeue();
        logger.log("CARD", p.getName(), card.getId());
        applyCard(p, card, deck);
    }

    /**
     * Applies a card. The card goes back to the bottom of its deck once its own
     * effect is done; for a move card that is before the destination square is
     * resolved (design decision D14). JAIL_FREE stays with the player.
     */
    private void applyCard(Player p, Card card, LinkedQueue<Card> deck) {
        switch (card.getEffect()) {
            case JAIL_FREE:
                p.addJailFreeCard(card);
                return;
            case MOVE_TO: {
                Node<Square> dest = board.findByName(board.getHead(), card.getArgument(), board.size());
                movePlayer(p, board.forwardDistance(p.getPosition(), dest));
                p.setMovedByCard(true);
                break;
            }
            case MOVE_BY:
                movePlayer(p, card.getAmount());
                p.setMovedByCard(true);
                break;
            case NEAREST: {
                Node<Square> dest = board.findNearest(p.getPosition().next, card.getArgument(), board.size());
                if (dest != null) {
                    movePlayer(p, board.forwardDistance(p.getPosition(), dest));
                    p.setMovedByCard(true);
                }
                break;
            }
            case COLLECT:
                p.addCash(card.getAmount());
                logger.log("CASH", p.getName(), "+" + card.getAmount(), String.valueOf(p.getCash()));
                break;
            case PAY:
                payBank(p, card.getAmount());
                break;
            case PAY_EACH:
                payEach(p, otherPlayersInQueueOrder(), 0, card.getAmount());
                break;
            case COLLECT_EACH:
                collectEach(p, otherPlayersInQueueOrder(), 0, card.getAmount());
                break;
            case GO_TO_JAIL:
                sendToJail(p);
                break;
            default:
                break;
        }
        deck.enqueue(card);
    }

    /** The other active players in current turn-queue order (the current player is not queued). */
    private ArrayList<Player> otherPlayersInQueueOrder() {
        ArrayList<Player> others = new ArrayList<>();
        turnQueue.copyInto(others);
        return others;
    }

    /** PAY_EACH, recursive. Stops if the payer cannot pay someone in full. */
    private void payEach(Player payer, ArrayList<Player> others, int index, int amount) {
        if (index >= others.size() || payer.isBankrupt()) {
            return;
        }
        Player other = others.get(index);
        int paid = Math.min(amount, payer.getCash());
        payer.addCash(-paid);
        other.addCash(paid);
        logger.log("CASH", payer.getName(), "-" + paid, String.valueOf(payer.getCash()));
        logger.log("CASH", other.getName(), "+" + paid, String.valueOf(other.getCash()));
        if (paid < amount) {
            declareBankrupt(payer, other);
            return;
        }
        payEach(payer, others, index + 1, amount);
    }

    /** COLLECT_EACH, recursive. A player who cannot pay in full goes bankrupt to the collector. */
    private void collectEach(Player collector, ArrayList<Player> others, int index, int amount) {
        if (index >= others.size()) {
            return;
        }
        Player other = others.get(index);
        int paid = Math.min(amount, other.getCash());
        other.addCash(-paid);
        collector.addCash(paid);
        logger.log("CASH", other.getName(), "-" + paid, String.valueOf(other.getCash()));
        logger.log("CASH", collector.getName(), "+" + paid, String.valueOf(collector.getCash()));
        if (paid < amount) {
            declareBankrupt(other, collector);
        }
        collectEach(collector, others, index + 1, amount);
    }

    // ---------------------------------------------------------- bankruptcy

    /**
     * Brief 3.7: remaining cash to the creditor (null = bank); squares back to
     * the bank; JAIL_FREE cards to the back of their decks; removed from the
     * turn queue without changing the order of the others.
     */
    private void declareBankrupt(Player p, Player creditor) {
        int rest = p.getCash();
        if (rest > 0) {
            p.addCash(-rest);
            if (creditor != null) {
                creditor.addCash(rest);
            }
        }
        logger.log("BANKRUPT", p.getName(), creditor == null ? "BANK" : creditor.getName());
        releaseSquares(p.getOwned());
        returnJailFreeCards(p.getJailFreeCards(), 0);
        turnQueue.remove(p);
        p.setBankrupt(true);
        p.setInJail(false);
        bankruptOrder.add(p);
    }

    /** Recursive: removes the last square each call (O(1)) and makes it unowned. */
    private void releaseSquares(ArrayList<Square> owned) {
        if (owned.isEmpty()) {
            return;
        }
        Square s = owned.remove(owned.size() - 1);
        ((OwnableSquare) s).setOwner(null);
        releaseSquares(owned);
    }

    /** Recursive: returns held cards in the order they were drawn, then empties the list. */
    private void returnJailFreeCards(ArrayList<Card> cards, int index) {
        if (index >= cards.size()) {
            clearCards(cards);
            return;
        }
        Card c = cards.get(index);
        deckFor(c.getDeck()).enqueue(c);
        returnJailFreeCards(cards, index + 1);
    }

    private void clearCards(ArrayList<Card> cards) {
        if (cards.isEmpty()) {
            return;
        }
        cards.remove(cards.size() - 1);
        clearCards(cards);
    }

    // ------------------------------------------------------------- ranking

    /**
     * Active players by net worth (highest first, ties by join order), then
     * bankrupt players, most recent bankruptcy first.
     */
    public ArrayList<Player> rank() {
        ArrayList<Player> active = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            if (!players.get(i).isBankrupt()) {
                active.add(players.get(i));
            }
        }
        Collections.sort(active);
        ArrayList<Player> ranking = new ArrayList<>(active);
        for (int i = bankruptOrder.size() - 1; i >= 0; i--) {
            ranking.add(bankruptOrder.get(i));
        }
        return ranking;
    }

    // ------------------------------------------------------------ accessors

    public Board getBoard() { return board; }
    public LinkedQueue<Player> getTurnQueue() { return turnQueue; }
    public LinkedQueue<Card> getChance() { return chance; }
    public LinkedQueue<Card> getCommunity() { return community; }
    public ArrayList<Player> getPlayers() { return players; }
    public GameLogger getLogger() { return logger; }
    public int getTurnCount() { return turnCount; }
    public int getMaxTurns() { return maxTurns; }
    public Dice getDice() { return dice; }
    public ArrayList<Card> getAllCards() { return allCards; }
    public boolean isFinished() { return finished; }
    public String getEndReason() { return endReason; }
}
