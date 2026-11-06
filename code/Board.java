/**
 * Board - the CircularDoublyLinkedList<Square>, plus direct references to the
 * GO and JAIL nodes. Holds the required recursive board methods (Section 5.1).
 * No integer indices or % size are used for movement.
 * T3 Expansion: pending EXPAND lines are kept in a LinkedQueue<Expansion>.
 * AI log: Conversation 01, Prompt 01.13; T3 added in Conversation 03, Prompt 03.02 (Claude). Reviewed and understood by: ______
 */
public final class Board {
    public static final int SALARY = 200;

    private final CircularDoublyLinkedList<Square> squares;
    private final Node<Square> goNode;
    private final Node<Square> jailNode;
    /** T3: EXPAND lines in file order, waiting for the end of turn 20 (FIFO). */
    private final LinkedQueue<Expansion> pendingExpansions = new LinkedQueue<>();

    public Board(CircularDoublyLinkedList<Square> squares) {
        this.squares = squares;
        this.goNode = squares.getHead();
        this.jailNode = findNearest(squares.getHead(), "JAIL", squares.size());
    }

    public Node<Square> getHead() { return squares.getHead(); }
    public Node<Square> getGo() { return goNode; }
    public Node<Square> getJail() { return jailNode; }
    public int size() { return squares.size(); }
    public CircularDoublyLinkedList<Square> getSquares() { return squares; }

    // ------------------------------------------------------- T3 Expansion

    /** Called by FileLoader for each EXPAND line, in file order. */
    public void addPendingExpansion(Expansion e) {
        pendingExpansions.enqueue(e);
    }

    public LinkedQueue<Expansion> getPendingExpansions() {
        return pendingExpansions;
    }

    /**
     * T3: links a new square directly after 'after' (CircularDoublyLinkedList
     * insertAfter, O(1)). Four links change: new.prev, new.next, after.next and
     * the old next's prev. No player moves: positions are node references, so
     * nothing has to be renumbered. If 'after' is the last square, the new
     * node's next is GO and GO's prev becomes the new node.
     */
    public Node<Square> insertAfter(Node<Square> after, Square square) {
        return squares.insertAfter(after, square);
    }

    /**
     * Moves steps squares: along next if positive, along prev if negative.
     * Pays the salary (and flags it on p) when moving forward onto or past GO.
     * Base case: steps == 0 returns current.
     * Recursive case: one step towards zero, then recurse with steps - 1 or steps + 1.
     * Terminates because |steps| falls by 1 each call.
     */
    public Node<Square> step(Node<Square> current, int steps, Player p) {
        if (steps == 0) {
            return current;
        }
        if (steps > 0) {
            Node<Square> next = current.next;
            if (next == goNode && p != null) {
                p.addCash(SALARY);
                p.setSalaryPaid(true);
            }
            return step(next, steps - 1, p);
        }
        return step(current.prev, steps + 1, p);
    }

    /**
     * True if p owns every PROPERTY of color, going once round the board.
     * Base cases: a square of that colour not owned by p returns false;
     * reaching the node before start (current.next == start) returns true.
     * Recursive case: check current, then recurse on current.next.
     * Terminates because the ring is finite and the walk stops when it
     * returns to start.
     */
    public boolean ownsGroup(Node<Square> current, Node<Square> start, String color, Player p) {
        Square s = current.data;
        if (s instanceof PropertySquare) {
            PropertySquare ps = (PropertySquare) s;
            if (ps.getColor().equals(color) && ps.getOwner() != p) {
                return false;
            }
        }
        if (current.next == start) {
            return true;
        }
        return ownsGroup(current.next, start, color, p);
    }

    /**
     * Node of the named square, or null after remaining squares.
     * Base cases: remaining == 0 returns null; a name match returns current.
     * Recursive case: recurse on current.next with remaining - 1.
     * Terminates because remaining falls by 1 each call.
     */
    public Node<Square> findByName(Node<Square> current, String name, int remaining) {
        if (remaining <= 0 || current == null) {
            return null;
        }
        if (current.data.getName().equals(name)) {
            return current;
        }
        return findByName(current.next, name, remaining - 1);
    }

    /**
     * Next square of the given type moving forward, starting at current itself
     * (callers pass position.next to skip the square the player stands on).
     * Base cases: remaining == 0 returns null; a type match returns current.
     * Recursive case: recurse on current.next with remaining - 1.
     * Terminates because remaining falls by 1 each call.
     */
    public Node<Square> findNearest(Node<Square> current, String type, int remaining) {
        if (remaining <= 0 || current == null) {
            return null;
        }
        if (current.data.getType().equals(type)) {
            return current;
        }
        return findNearest(current.next, type, remaining - 1);
    }

    /**
     * Number of forward steps from 'from' to 'to' (1..size). If to == from the
     * answer is a full lap (size). Returns -1 if 'to' is not on the board.
     * Recursive helper used by MOVE_TO and NEAREST cards.
     */
    public int forwardDistance(Node<Square> from, Node<Square> to) {
        return countForward(from.next, to, 1);
    }

    private int countForward(Node<Square> current, Node<Square> to, int count) {
        if (current == to) {
            return count;
        }
        if (count >= squares.size()) {
            return -1;
        }
        return countForward(current.next, to, count + 1);
    }
}
