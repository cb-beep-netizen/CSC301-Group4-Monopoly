package monopoly;

import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        System.out.println("=================================================");
        System.out.println("   CSC301 INTERACTIVE MONOPOLY BOARD GAME        ");
        System.out.println("=================================================\n");

        // 1. Load board from package source folder
        Board board = BoardLoader.loadBoard("src/monopoly/board.txt");
        if (board.squares.size() == 0) {
            System.err.println("Error: Board file could not be loaded or is empty!");
            scanner.close();
            return;
        }
        System.out.println("Board loaded successfully with " + board.squares.size() + " squares.\n");

        // 2. Dynamic Player Setup chosen by user
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

        // 3. Initialize card decks & Game instance
        LinkedQueue<Card> chanceDeck = new LinkedQueue<>();
        LinkedQueue<Card> communityDeck = new LinkedQueue<>();
        Game game = new Game(board, turnQueue, chanceDeck, communityDeck, 60);

        System.out.println("\n=================================================");
        System.out.println(" GAME STARTING! Press [ENTER] after each turn.   ");
        System.out.println("=================================================\n");

        // 4. Interactive Main Game Loop
        while (game.currentTurnCount < game.maxTurns && turnQueue.size() > 1 && !game.gameOver) {
            game.currentTurnCount++;
            Player current = turnQueue.dequeue();

            // Display visual ASCII game board showing player positions
            printBoardMap(board, allPlayers, current);

            System.out.println("-------------------------------------------------");
            System.out.println("Turn " + game.currentTurnCount + " | Current Player: " + current.name + " (Cash: $" + current.cash + ")");
            System.out.print("Press [ENTER] to roll & take turn (or type 'status' for net worth, 'quit' to exit): ");
            
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
            }

            GameLogger.logTurn(game.currentTurnCount, current);

            // Execute recursive turn
            game.takeTurn(current, 0);

            // Reset turn-specific twist counters
            current.mirageCount = 0;
            current.lastMoveWasCard = false;

            if (current.cash >= 0) {
                turnQueue.enqueue(current);
            } else {
                GameLogger.logBankrupt(current, "BANK");
            }
            
            System.out.println("\n");
        }

        // 5. Game Over & Final Standings
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

    // Helper method to render a visual map of the board and player positions
    private static void printBoardMap(Board board, ArrayList<Player> allPlayers, Player activePlayer) {
        System.out.println("\n=================== GAME BOARD MAP ===================");
        Node<Square> curr = board.squares.getHead();
        if (curr == null) return;

        int index = 0;
        do {
            Square sq = curr.data;
            String marker = "";

            // Check which players are currently standing on this square
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
