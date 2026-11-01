package monopoly;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================================");
        System.out.println("   CSC301 INTERACTIVE MONOPOLY & BONUS ENGINE    ");
        System.out.println("=================================================\n");

        Board board = BoardLoader.loadBoard("src/monopoly/board.txt");
        if (board.squares.size() == 0) {
            System.err.println("Error: Board file could not be loaded or is empty!");
            scanner.close();
            return;
        }
        System.out.println("Board loaded successfully with " + board.squares.size() + " squares.\n");

        LinkedQueue<Player> turnQueue = new LinkedQueue<>();
        ArrayList<Player> allPlayers = new ArrayList<>();

        System.out.print("Enter number of players (2 to 4): ");
        int numPlayers = 3;
        try {
            numPlayers = Integer.parseInt(scanner.nextLine().trim());
            if (numPlayers < 2 || numPlayers > 4) numPlayers = 3;
        } catch (Exception e) {
            numPlayers = 3;
        }

        Node<Square> startNode = board.goNode != null ? board.goNode : board.squares.getHead();

        for (int i = 1; i <= numPlayers; i++) {
            System.out.print("Enter name for Player " + i + ": ");
            String playerName = scanner.nextLine().trim();
            if (playerName.isEmpty()) {
                playerName = "Player" + i;
            }
            Player p = new Player(playerName, startNode);
            turnQueue.enqueue(p);
            allPlayers.add(p);
        }

        LinkedQueue<Card> chanceDeck = new LinkedQueue<>();
        LinkedQueue<Card> communityDeck = new LinkedQueue<>();
        Game game = new Game(board, turnQueue, chanceDeck, communityDeck, 60);
        
        LinkedStack<GameSnapshot> undoStack = new LinkedStack<>();

        System.out.println("\n=================================================");
        System.out.println(" GAME STARTING! Bonus Commands Available:        ");
        System.out.println(" - Press [ENTER] to take turn                    ");
        System.out.println(" - Type 'status' for net worth leaderboard       ");
        System.out.println(" - Type 'undo' to time-travel back 1 turn        ");
        System.out.println(" - Type 'quit' to exit                           ");
        System.out.println("=================================================\n");

        while (game.currentTurnCount < game.maxTurns && turnQueue.size() > 1 && !game.gameOver) {
            game.currentTurnCount++;
            Player current = turnQueue.dequeue();

            printBoardMap(board, allPlayers, current);

            System.out.println("-------------------------------------------------");
            System.out.println("Turn " + game.currentTurnCount + " | Current Player: " + current.name + " (Cash: $" + current.cash + ")");
            System.out.print("Action [ENTER / status / undo / quit]: ");
            
            String input = scanner.nextLine().trim();
            if (input.equalsIgnoreCase("quit")) {
                System.out.println("\nExiting game early.");
                break;
            } else if (input.equalsIgnoreCase("status")) {
                System.out.println("\n--- LIVE PLAYER STATUS ---");
                for (Player p : allPlayers) {
                    int nw = p.cash + p.netWorth(p.owned, 0);
                    System.out.println(p.name + " -> Cash: $" + p.cash + " | Net Worth: $" + nw);
                }
                System.out.println("--------------------------\n");
                turnQueue.enqueue(current);
                game.currentTurnCount--;
                continue;
            } else if (input.equalsIgnoreCase("undo")) {
                if (!undoStack.isEmpty()) {
                    GameSnapshot snap = undoStack.pop();
                    System.out.println("\n⏳ [TIME TRAVEL] Reverting state to Turn " + snap.turnCount + " for " + snap.playerName + "...");
                    current.cash = snap.cash;
                    game.currentTurnCount = snap.turnCount - 1;
                } else {
                    System.out.println("\n⚠️ No previous states saved in stack yet!");
                }
                turnQueue.enqueue(current);
                continue;
            }

            undoStack.push(new GameSnapshot(current.name, current.cash, game.currentTurnCount));

            if (game.currentTurnCount % 10 == 0) {
                game.triggerEconomicShift(board);
            }

            GameLogger.logTurn(game.currentTurnCount, current);

            game.takeTurn(current, 0);

            if (current.cash < 0 && !current.owned.isEmpty()) {
                System.out.println("\n🚨 " + current.name + " is in debt ($" + current.cash + ")! Triggering Emergency Asset Auction...");
                Square sq = current.owned.get(0);
                if (sq instanceof PropertySquare) {
                    PropertySquare propToSell = (PropertySquare) sq;
                    current.emergencyAssetAuction(propToSell, turnQueue, Math.abs(current.cash));
                }
            }

            current.mirageCount = 0;
            current.lastMoveWasCard = false;

            if (current.cash >= 0) {
                turnQueue.enqueue(current);
            } else {
                GameLogger.logBankrupt(current, "BANK");
            }
            
            System.out.println("\n");
        }

        System.out.println("\n=================================================");
        System.out.println("              FINAL GAME RANKINGS                ");
        System.out.println("=================================================");

        String reason = turnQueue.size() == 1 ? "LAST_PLAYER" : "MAX_TURNS";
        GameLogger.logEnd(reason);

        allPlayers.sort((a, b) -> Integer.compare(b.netWorth(b.owned, 0) + b.cash, a.netWorth(a.owned, 0) + a.cash));
        for (int i = 0; i < allPlayers.size(); i++) {
            Player p = allPlayers.get(i);
            int nw = p.cash >= 0 ? (p.cash + p.netWorth(p.owned, 0)) : 0;
            GameLogger.logRank(i + 1, p, nw);
        }

        scanner.close();
    }

    private static void printBoardMap(Board board, ArrayList<Player> allPlayers, Player activePlayer) {
        System.out.println("\n=================== GAME BOARD MAP ===================");
        Node<Square> curr = board.squares.getHead();
        if (curr == null) return;

        int index = 0;
        do {
            Square sq = curr.data;
            String marker = "";

            for (Player p : allPlayers) {
                if (p.position != null && p.position.data != null && p.position.data.equals(sq)) {
                    marker += "[" + p.name.substring(0, Math.min(2, p.name.length())) + "]";
                }
            }

            if (!marker.isEmpty()) {
                marker = " <-- " + marker;
            }

            String squareType = sq.getClass().getSimpleName();
            String activeIndicator = (pInSquare(activePlayer, sq)) ? " *" : "";
            System.out.printf("[%2d] %-18s (%s)%s%s\n", index, sq.getName(), squareType, activeIndicator, marker);

            curr = curr.next;
            index++;
        } while (curr != board.squares.getHead());
        System.out.println("======================================================\n");
    }

    private static boolean pInSquare(Player p, Square sq) {
        return p.position != null && p.position.data != null && p.position.data.equals(sq);
    }
}