package monopoly;

public class Game {
    public Board board;
    public LinkedQueue<Player> turnQueue;
    public LinkedQueue<Card> chance;
    public LinkedQueue<Card> community;
    public int maxTurns;
    public int currentTurnCount;
    public boolean gameOver;

    public Game(Board board, LinkedQueue<Player> turnQueue, LinkedQueue<Card> chance, LinkedQueue<Card> community, int maxTurns) {
        this.board = board;
        this.turnQueue = turnQueue;
        this.chance = chance;
        this.community = community;
        this.maxTurns = maxTurns;
        this.currentTurnCount = 0;
        this.gameOver = false;
    }

    public void takeTurn(Player p, int doublesCount) {
        if (doublesCount >= 3) {
            GameLogger.logJail(p);
            p.position = board.jailNode;
            p.jailAttempts = 1;
            return;
        }

        int[] rolls = Dice.roll();
        int d1 = rolls[0];
        int d2 = rolls[1];
        boolean isDouble = (d1 == d2);

        if (p.jailAttempts > 0) {
            GameLogger.logJailRoll(p, d1, d2, isDouble ? "RELEASED" : (p.cash >= 50 ? "FINE" : "STAY"));
            
            if (isDouble) {
                System.out.println(p.name + " rolled doubles (" + d1 + "," + d2 + ") and escaped Jail!");
                p.jailAttempts = 0;
            } else if (p.cash >= 50) {
                p.cash -= 50;
                GameLogger.logCash(p, "-50", p.cash);
                p.jailAttempts = 0;
                System.out.println(p.name + " paid $50 fine to escape Jail!");
            } else {
                System.out.println(p.name + " failed to roll doubles and cannot afford the fine. Stays in Jail.");
                return;
            }
        } else {
            GameLogger.logRoll(p, d1, d2);
        }

        p.lastDiceTotal = d1 + d2;
        String oldSquareName = p.position.data.getName();

        Node<Square> destination = board.step(p.position, p.lastDiceTotal, p);
        String newSquareName = destination.data.getName();

        GameLogger.logMove(p, oldSquareName, newSquareName);
        p.position = destination;

        landOn(p);

        if (isDouble && !gameOver && p.jailAttempts == 0) {
            takeTurn(p, doublesCount + 1);
        }
    }

    public void landOn(Player p) {
        Square sq = p.position.data;

        if (sq instanceof PropertySquare) {
            PropertySquare prop = (PropertySquare) sq;
            if (prop.owner == null) {
                if (p.cash >= prop.price) {
                    p.cash -= prop.price;
                    prop.owner = p;
                    p.owned.add(prop);
                    GameLogger.logBuy(p, prop.getName(), p.cash);
                }
            } else if (prop.owner != p) {
                int rent = prop.baseRent;
                if (board.ownsGroup(board.squares.getHead(), board.squares.getHead(), prop.color, prop.owner)) {
                    rent *= 2;
                }
                p.cash -= rent;
                prop.owner.cash += rent;
                GameLogger.logRent(p, prop.owner, rent, p.cash, prop.owner.cash);
            }
        } else if (sq instanceof TaxSquare) {
            TaxSquare tax = (TaxSquare) sq;
            p.cash -= tax.amount;
            GameLogger.logTax(p, tax.amount, p.cash);
        } else if (sq instanceof CornerSquare) {
            if (sq.getName().equals("GO_TO_JAIL")) {
                GameLogger.logJail(p);
                p.position = board.jailNode;
                p.jailAttempts = 1;
            }
        } else {
            sq.land(this, p);
        }
    }
    
    public void triggerEconomicShift(Board board) {
        double multiplier = (Math.random() > 0.5) ? 1.25 : 0.80;
        String eventName = (multiplier > 1.0) ? "Economic Boom (Prices & Rent UP!)" : "Market Recession (Prices & Rent DOWN!)";
        
        System.out.println("\n📈 MACROECONOMIC EVENT: " + eventName);
        
        Node<Square> curr = board.squares.getHead();
        if (curr == null) return;
        
        do {
            Square sq = curr.data;
            if (sq instanceof PropertySquare) {
                PropertySquare prop = (PropertySquare) sq;
                prop.baseRent = (int)(prop.baseRent * multiplier);
            }
            curr = curr.next;
        } while (curr != board.squares.getHead());
    }
}