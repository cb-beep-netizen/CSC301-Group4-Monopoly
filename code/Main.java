import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;
import java.util.Scanner;

/**
 * Main - reads the options, loads the board and card files, adds the players
 * and runs the game.
 *
 * Usage:
 *   java -cp bin Main <board file> <card file> [options]
 * Options:
 *   --seed N         seed for dice and deck shuffling (same seed = same game)
 *   --players A,B,C  player names in turn order (otherwise asked on the console)
 *   --auto           buy whenever affordable (no Y/N prompts)
 *   --max-turns N    turn limit (default 60)
 *   --dice 3,4,6,6   scripted dice values, used first (tests and demos)
 *   --log FILE       log file (default game-log.txt)
 *   --pause          wait for Enter before each turn (always on in board view unless --auto)
 *   --text           plain mode: print the Appendix C log lines instead of the board view
 *   --color / --no-color   force colours on or off (default: detected)
 *   --ascii          draw the board with + - | instead of box-drawing characters
 *   --log-lines      board view shows raw log lines instead of plain sentences
 * AI log: Conversation 01, Prompt 01.13, updated in Prompts 01.14 and 01.22 (Claude). Reviewed and understood by: ______
 */
public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: java -cp bin Main <board file> <card file> [--seed N] [--players A,B,C]");
            System.out.println("       [--auto] [--max-turns N] [--dice 3,4,6,6] [--log FILE] [--pause]");
            System.out.println("       [--text] [--color | --no-color] [--ascii] [--log-lines]");
            System.exit(1);
        }
        String boardPath = args[0];
        String cardPath = args[1];
        long seed = System.nanoTime();
        boolean seedGiven = false;
        String playerList = null;
        boolean auto = false;
        boolean pause = false;
        boolean text = false;
        boolean ascii = false;
        boolean logLines = false;
        Boolean colorChoice = null;
        int maxTurns = Game.DEFAULT_MAX_TURNS;
        String logPath = "game-log.txt";
        ArrayList<Integer> scriptedDice = null;

        try {
            for (int i = 2; i < args.length; i++) {
                String a = args[i];
                if (a.equals("--seed")) {
                    seed = Long.parseLong(value(args, ++i, a));
                    seedGiven = true;
                } else if (a.equals("--players")) {
                    playerList = value(args, ++i, a);
                } else if (a.equals("--auto")) {
                    auto = true;
                } else if (a.equals("--pause")) {
                    pause = true;
                } else if (a.equals("--text")) {
                    text = true;
                } else if (a.equals("--ascii")) {
                    ascii = true;
                } else if (a.equals("--log-lines")) {
                    logLines = true;
                } else if (a.equals("--color")) {
                    colorChoice = Boolean.TRUE;
                } else if (a.equals("--no-color")) {
                    colorChoice = Boolean.FALSE;
                } else if (a.equals("--max-turns")) {
                    maxTurns = Integer.parseInt(value(args, ++i, a));
                } else if (a.equals("--log")) {
                    logPath = value(args, ++i, a);
                } else if (a.equals("--dice")) {
                    scriptedDice = parseDice(value(args, ++i, a));
                } else {
                    throw new IllegalArgumentException("unknown option " + a);
                }
            }
        } catch (RuntimeException e) {
            System.out.println("Error in options: " + e.getMessage());
            System.exit(1);
        }

        Scanner in = new Scanner(System.in);
        GameLogger logger = null;
        try {
            Board board = FileLoader.loadBoard(boardPath);
            ArrayList<Card> cards = FileLoader.loadCards(cardPath, board);
            Random rng = new Random(seed);
            Dice dice = new Dice(rng, scriptedDice);
            logger = new GameLogger(logPath, text);
            BuyDecider decider = auto ? new AutoBuyDecider() : new ConsoleBuyDecider(in);
            Game game = new Game(board, cards, rng, true, dice, logger, decider, maxTurns);

            System.out.println("=== Capital Circuit - CSC301 simplified Monopoly ===");
            System.out.println("Board: " + board.size() + " squares, " + cards.size() + " cards. Seed: " + seed
                    + (seedGiven ? "" : "  (replay this game with --seed " + seed + ")"));
            addPlayers(game, playerList, in);
            System.out.println("Log file: " + logPath);
            System.out.println();

            if (text) {
                runText(game, in, pause);
            } else {
                boolean color = colorChoice != null ? colorChoice : BoardView.ansiSupported();
                runBoardView(game, new BoardView(color, ascii, logLines), in, auto && !pause, logPath);
            }
        } catch (IOException e) {
            System.out.println("Cannot read file: " + e.getMessage());
            System.exit(1);
        } catch (IllegalArgumentException | IllegalStateException e) {
            System.out.println("Error: " + e.getMessage());
            System.exit(1);
        } finally {
            if (logger != null) {
                logger.close();
            }
        }
    }

    /** Plain mode: the Appendix C log lines are printed as the game runs. */
    private static void runText(Game game, Scanner in, boolean pause) {
        while (!game.isOver()) {
            if (pause) {
                System.out.print("  > Press Enter for turn " + (game.getTurnCount() + 1) + "... ");
                if (in.hasNextLine()) {
                    in.nextLine();
                }
            }
            game.playOneTurn();
        }
        game.finish();
        System.out.print(new BoardView(false, true, false).standings(game, "FINAL STANDINGS"));
    }

    /**
     * Board view: before each turn the board is drawn with the previous turn's
     * events; the player presses Enter to roll (Q quits). With --auto and no
     * --pause the game runs straight through and only the final screen is drawn.
     */
    private static void runBoardView(Game game, BoardView view, Scanner in, boolean runThrough, String logPath) {
        while (!game.isOver()) {
            if (!runThrough) {
                Player next = game.getTurnQueue().peek();
                String jail = next.isInJail() ? " (in Jail)" : "";
                view.render(game, System.lineSeparator() + "  > " + next.getName() + jail
                        + ": press Enter to roll the dice (Q to quit) ");
                if (!in.hasNextLine() || in.nextLine().trim().equalsIgnoreCase("Q")) {
                    view.setStandingsMode(true);
                    view.render(game, view.standings(game, "GAME STOPPED after turn " + game.getTurnCount()
                            + " - standings by net worth") + System.lineSeparator()
                            + "  The game log so far is in " + logPath + System.lineSeparator());
                    return;
                }
            }
            game.playOneTurn();
        }
        game.finish();
        String reason = "LAST_PLAYER".equals(game.getEndReason()) ? "one player left" : "turn limit reached";
        view.render(game, view.standings(game, "GAME OVER (" + reason + ") - final ranking")
                + System.lineSeparator() + "  The full game log is in " + logPath + System.lineSeparator());
    }

    private static String value(String[] args, int i, String option) {
        if (i >= args.length) {
            throw new IllegalArgumentException(option + " needs a value");
        }
        return args[i];
    }

    private static ArrayList<Integer> parseDice(String text) {
        ArrayList<Integer> values = new ArrayList<>();
        String[] parts = text.split(",");
        for (int i = 0; i < parts.length; i++) {
            int v = Integer.parseInt(parts[i].trim());
            if (v < 1 || v > 6) {
                throw new IllegalArgumentException("dice values must be 1 to 6: " + v);
            }
            values.add(v);
        }
        return values;
    }

    private static void addPlayers(Game game, String playerList, Scanner in) {
        ArrayList<String> names = new ArrayList<>();
        if (playerList != null) {
            String[] parts = playerList.split(",");
            for (int i = 0; i < parts.length; i++) {
                addName(names, parts[i].trim());
            }
        } else {
            int count = askCount(in);
            for (int i = 1; i <= count; i++) {
                while (true) {
                    System.out.print("Name of player " + i + ": ");
                    if (!in.hasNextLine()) {
                        throw new IllegalStateException("input ended while reading player names");
                    }
                    try {
                        addName(names, in.nextLine().trim());
                        break;
                    } catch (IllegalArgumentException e) {
                        System.out.println("  " + e.getMessage());
                    }
                }
            }
        }
        if (names.size() < Game.MIN_PLAYERS || names.size() > Game.MAX_PLAYERS) {
            throw new IllegalArgumentException("a game needs 2 to 6 players, got " + names.size());
        }
        for (int i = 0; i < names.size(); i++) {
            game.addPlayer(names.get(i));
        }
        System.out.println("Players (turn order): " + String.join(", ", names));
    }

    private static void addName(ArrayList<String> names, String name) {
        if (name.isEmpty() || name.contains("|")) {
            throw new IllegalArgumentException("a name must not be empty or contain |");
        }
        if (names.contains(name)) {
            throw new IllegalArgumentException("names must be unique: " + name);
        }
        names.add(name);
    }

    private static int askCount(Scanner in) {
        while (true) {
            System.out.print("Number of players (2-6): ");
            if (!in.hasNextLine()) {
                throw new IllegalStateException("input ended while reading the number of players");
            }
            try {
                int n = Integer.parseInt(in.nextLine().trim());
                if (n >= Game.MIN_PLAYERS && n <= Game.MAX_PLAYERS) {
                    return n;
                }
            } catch (NumberFormatException e) {
                // fall through and ask again
            }
            System.out.println("  Please enter a number from 2 to 6.");
        }
    }
}
