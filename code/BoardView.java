import java.util.ArrayList;

/**
 * BoardView - draws the game on the console like the prototype screen: the
 * board as a ring of squares (owners, prices, player tokens), and in the middle
 * the players' cash and positions, the turn order and the events of the last
 * turn in plain words.
 *
 * Display only: it reads the game state and never changes it. It walks the
 * board with next from GO and places each square in a display grid; those grid
 * positions are never used for movement (positions stay Node<Square>).
 * The layout is recomputed on every render, so a board that changes size
 * (twists T2/T3) still draws correctly.
 * Colours use ANSI codes when the console supports them (Windows Terminal,
 * VS Code, and the classic Windows 10/11 console once enableWindowsColour()
 * has run); otherwise the same screen is drawn without colour. Each player has
 * a colour; squares they own are tinted with it and show their name.
 * AI log: Conversation 01, Prompt 01.14, updated in Prompts 01.15 and 01.22;
 * twist log lines narrated in Conversation 03, Prompt 03.02 (Claude). Reviewed and understood by: ______
 */
public class BoardView {
    private static final int CW = 13;   // inner width of a square cell
    private static final int CH = 4;    // inner height of a square cell
    private static final String ESC = "\u001B[";
    private static final String RESET = ESC + "0m";

    private final boolean color;
    private final boolean ascii;
    private final boolean rawEvents;
    private char[][] grid;
    private String[][] style;
    private boolean[][] hSeg;
    private boolean[][] vSeg;
    private final ArrayList<String> groupNames = new ArrayList<>();
    private boolean standingsMode;   // true after the players quit: centre shows the standings

    public BoardView(boolean color, boolean ascii, boolean rawEvents) {
        this.color = color;
        this.ascii = ascii;
        this.rawEvents = rawEvents;
    }

    /** When true the centre shows the ranking (used when the game is stopped with Q). */
    public void setStandingsMode(boolean on) {
        standingsMode = on;
    }

    /**
     * True when the console can show ANSI colours. Windows Terminal, VS Code and
     * ConEmu always can. The classic Windows 10/11 console can too, once colour
     * processing is switched on - enableWindowsColour() does that.
     */
    public static boolean ansiSupported() {
        if (System.getenv("WT_SESSION") != null || System.getenv("TERM_PROGRAM") != null
                || "ON".equalsIgnoreCase(System.getenv("ConEmuANSI"))) {
            return true;
        }
        if (isWindows()) {
            return windowsVersion() >= 10 && enableWindowsColour();
        }
        String term = System.getenv("TERM");
        return term != null && !term.equals("dumb");
    }

    /**
     * Switches on ANSI colour processing in the classic Windows console. Running
     * a short cmd.exe command on the same console leaves "virtual terminal"
     * processing enabled after it exits (the same technique Python programs use
     * with os.system("")). Returns false if cmd.exe cannot be started.
     */
    private static boolean enableWindowsColour() {
        try {
            new ProcessBuilder("cmd", "/c", "ver>nul").inheritIO().start().waitFor();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static int windowsVersion() {
        try {
            String v = System.getProperty("os.version", "0");
            int dot = v.indexOf('.');
            return Integer.parseInt(dot < 0 ? v : v.substring(0, dot));
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "").toLowerCase().startsWith("windows");
    }

    public void clearScreen() {
        if (color) {
            System.out.print(ESC + "H" + ESC + "2J" + ESC + "3J");
            System.out.flush();
            return;
        }
        if (isWindows()) {
            try {
                new ProcessBuilder("cmd", "/c", "cls").inheritIO().start().waitFor();
                return;
            } catch (Exception e) {
                // fall through to blank lines
            }
        }
        System.out.println();
        System.out.println();
    }

    /** Clears the console and draws the whole screen, then the footer line (no newline). */
    public void render(Game game, String footer) {
        clearScreen();
        System.out.print(build(game));
        System.out.print(footer);
        System.out.flush();
    }

    // ------------------------------------------------------------- building

    /** Builds the full screen as text. Public so tests can check it. */
    public String build(Game game) {
        Board board = game.getBoard();
        int n = board.size();
        int side = 2;
        while (4 * side - 4 < n) {
            side++;
        }
        int width = side * (CW + 1) + 1;
        int height = side * (CH + 1) + 1;
        grid = new char[height][width];
        style = new String[height][width];
        hSeg = new boolean[height][width];
        vSeg = new boolean[height][width];
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                grid[y][x] = ' ';
            }
        }
        ArrayList<Player> players = game.getPlayers();
        String tokens = tokens(players);
        Player current = currentPlayer(game);

        // walk the ring once from GO: display order only
        Node<Square> node = board.getHead();
        for (int i = 0; i < 4 * side - 4; i++) {
            int row, col;
            if (i < side) {                       // top row, left to right
                row = 0; col = i;
            } else if (i < 2 * side - 1) {        // right column, top to bottom
                row = i - (side - 1); col = side - 1;
            } else if (i < 3 * side - 2) {        // bottom row, right to left
                row = side - 1; col = (side - 1) - (i - (2 * side - 2));
            } else {                              // left column, bottom to top
                row = (side - 1) - (i - (3 * side - 3)); col = 0;
            }
            int y0 = row * (CH + 1);
            int x0 = col * (CW + 1);
            markBox(y0, x0, y0 + CH + 1, x0 + CW + 1);
            if (i < n) {
                drawSquare(node, y0 + 1, x0 + 1, players, tokens, current, board);
                node = node.next;
            }
        }
        drawBorders(height, width);
        drawCentre(game, players, tokens, current, CW + 3, CH + 2, (side - 2) * (CW + 1) - 3, (side - 2) * (CH + 1) - 1);
        return emit(height, width);
    }

    private void markBox(int y0, int x0, int y1, int x1) {
        for (int x = x0; x < x1; x++) {
            hSeg[y0][x] = true;
            hSeg[y1][x] = true;
        }
        for (int y = y0; y < y1; y++) {
            vSeg[y][x0] = true;
            vSeg[y][x1] = true;
        }
    }

    private void drawBorders(int height, int width) {
        String bs = color ? ESC + "38;5;244m" : null;
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                boolean up = y > 0 && vSeg[y - 1][x];
                boolean down = vSeg[y][x];
                boolean left = x > 0 && hSeg[y][x - 1];
                boolean right = hSeg[y][x];
                if (!(up || down || left || right)) {
                    continue;
                }
                grid[y][x] = junction(up, down, left, right);
                style[y][x] = bs;
            }
        }
    }

    private char junction(boolean u, boolean d, boolean l, boolean r) {
        boolean vertical = u || d;
        boolean horizontal = l || r;
        if (ascii) {
            return vertical && horizontal ? '+' : vertical ? '|' : '-';
        }
        if (!vertical) return '─';
        if (!horizontal) return '│';
        if (u && d && l && r) return '┼';
        if (u && d && r) return '├';
        if (u && d) return '┤';
        if (l && r && d) return '┬';
        if (l && r) return '┴';
        if (d && r) return '┌';
        if (d) return '┐';
        if (r) return '└';
        return '┘';
    }

    private void drawSquare(Node<Square> node, int y, int x, ArrayList<Player> players,
                            String tokens, Player current, Board board) {
        Square s = node.data;
        String type = s.getType();
        String header;
        String info;
        String headStyle;
        switch (type) {
            case "PROPERTY": {
                PropertySquare p = (PropertySquare) s;
                header = " " + p.getColor();
                info = p.getPrice() + " r" + (p.getOwner() == null ? p.getBaseRent() : p.rent(board));
                headStyle = groupStyle(p.getColor());
                break;
            }
            case "TRANSIT": {
                TransitSquare t = (TransitSquare) s;
                header = " TRANSIT";
                info = t.getPrice() + (t.getOwner() == null ? " r25+" : " r" + t.rent(board));
                headStyle = bg(240);
                break;
            }
            case "TAX":
                header = " TAX";
                info = "pay " + ((TaxSquare) s).getAmount();
                headStyle = bg(94);
                break;
            case "CHANCE":
                header = " CHANCE ?";
                info = "draw a card";
                headStyle = bg(97);
                break;
            case "COMMUNITY":
                header = " COMMUNITY";
                info = "draw a card";
                headStyle = bg(31);
                break;
            case "GO":
                header = " GO  >>>";
                info = "+200 salary";
                headStyle = bg(252, 16);
                break;
            case "JAIL":
                header = " JAIL";
                info = "visit / held";
                headStyle = bg(252, 16);
                break;
            case "FREE":
                header = " FREE";
                info = "rest";
                headStyle = bg(252, 16);
                break;
            case "GO_TO_JAIL":
                header = " GO TO JAIL";
                info = "-> Jail";
                headStyle = bg(252, 16);
                break;
            default:
                header = " " + type;
                info = "";
                headStyle = bg(240);
        }
        Player owner = (s instanceof OwnableSquare) ? ((OwnableSquare) s).getOwner() : null;
        int ownerIdx = owner == null ? -1 : players.indexOf(owner);
        boolean here = current != null && current.getPosition() == node && !current.isBankrupt();
        // owned squares are tinted with the owner's colour; the current player's
        // square (if unowned) gets a light grey highlight
        String cellBg = !color ? null
                : ownerIdx >= 0 ? ESC + "48;5;" + playerTint(ownerIdx) + "m"
                : here ? ESC + "48;5;237m" : null;
        put(y, x, pad(header, CW), color ? headStyle : null);
        if (ownerIdx >= 0) {
            String chip = "[" + tokens.charAt(ownerIdx) + "]";
            put(y, x + CW - 3, chip, color ? ESC + "1;38;5;16;48;5;" + playerColor(ownerIdx) + "m" : null);
        }
        String nameStyle = ownerIdx >= 0 ? ESC + "1;38;5;" + playerColor(ownerIdx) + "m" : ESC + "1;38;5;255m";
        put(y + 1, x, pad(fit(s.getName(), CW), CW), color ? join(nameStyle, cellBg) : null);
        String infoText = ownerIdx >= 0 ? fit(info + "  " + owner.getName(), CW) : info;
        put(y + 2, x, pad(infoText, CW), color ? join(ESC + "38;5;252m", cellBg) : null);
        put(y + 3, x, pad("", CW), cellBg);
        int tx = x;
        for (int i = 0; i < players.size(); i++) {
            Player p = players.get(i);
            if (p.isBankrupt() || p.getPosition() != node || tx >= x + CW) {
                continue;
            }
            String t = String.valueOf(tokens.charAt(i)) + (p.isInJail() ? "#" : "");
            String tokenStyle = p == current
                    ? ESC + "1;38;5;16;48;5;" + playerColor(i) + "m"          // player to move: solid block
                    : join(ESC + "1;38;5;" + playerColor(i) + "m", cellBg);
            put(y + 3, tx, p == current ? " " + t + " " : t, color ? tokenStyle : null);
            tx += t.length() + (p == current ? 3 : 1);
        }
    }

    private void drawCentre(Game game, ArrayList<Player> players, String tokens, Player current,
                            int x, int y, int w, int h) {
        String dim = color ? ESC + "38;5;245m" : null;
        String ruleStyle = color ? ESC + "38;5;240m" : null;
        int row = 0;
        put(y + row++, x, fit(center("C A P I T A L   C I R C U I T", w), w), color ? ESC + "1;38;5;220m" : null);
        put(y + row++, x, fit(center("Abu Dhabi edition - CSC301 simplified Monopoly", w), w), dim);

        String turnInfo = "Turn " + game.getTurnCount() + " of " + game.getMaxTurns() + "    ";
        put(y + row, x, turnInfo, color ? ESC + "1m" : null);
        if (game.isOver()) {
            put(y + row, x + turnInfo.length(), "GAME OVER (" + game.getEndReason() + ")", color ? ESC + "1;38;5;196m" : null);
        } else if (standingsMode) {
            put(y + row, x + turnInfo.length(), "GAME STOPPED", color ? ESC + "1;38;5;196m" : null);
        } else if (current != null) {
            put(y + row, x + turnInfo.length(), "Next to play: ", null);
            put(y + row, x + turnInfo.length() + 14, " " + current.getName() + " ",
                    color ? ESC + "1;38;5;16;48;5;" + playerColor(players.indexOf(current)) + "m" : null);
        }
        row++;
        put(y + row++, x, rule(w), ruleStyle);

        if (game.isFinished() || standingsMode) {
            put(y + row++, x, (game.isFinished() ? "FINAL RANKING" : "STANDINGS (game stopped)")
                    + "              NET WORTH   CASH", dim);
            ArrayList<Player> ranking = game.rank();
            boolean roomForLists = row + 2 * ranking.size() <= h;
            for (int i = 0; i < ranking.size() && row < h; i++) {
                Player p = ranking.get(i);
                int idx = players.indexOf(p);
                put(y + row, x, (i + 1) + ".", color ? ESC + "1m" : null);
                put(y + row, x + 3, " " + tokens.charAt(idx) + " ", swatch(idx));
                put(y + row, x + 7, pad(fit(p.getName(), 20), 20) + String.format("%7d", p.totalNetWorth())
                        + String.format("%7d", p.isBankrupt() ? 0 : p.getCash())
                        + (p.isBankrupt() ? "   bankrupt" : ""), playerText(idx, p.isBankrupt()));
                row++;
                if (roomForLists && row < h) {
                    put(y + row++, x + 7, propertyLine(game, p, w - 7), ownedText(idx, p.isBankrupt()));
                }
            }
        } else {
            put(y + row++, x, "      NAME        CASH  POSITION          OWN    NET", dim);
            // two lines per player (details + properties) when there is room for a few events too
            boolean roomForLists = row + 2 * players.size() + 2 + 3 <= h;
            for (int i = 0; i < players.size() && row < h; i++) {
                Player p = players.get(i);
                String status = p.isBankrupt() ? "BANKRUPT" : p.isInJail() ? "JAIL " + p.getJailAttempts() + "/3" : "";
                if (p == current) {
                    status = "<- next " + status;
                }
                String pos = p.isBankrupt() ? "-" : fit(p.getPosition().data.getName(), 16);
                put(y + row, x, " " + tokens.charAt(i) + " ", swatch(i));
                put(y + row, x + 4, pad(fit(p.getName(), 9), 9) + String.format("%7d", p.getCash()) + "  "
                        + pad(pos, 16) + String.format("%4d", p.getOwned().size())
                        + String.format("%7d", p.totalNetWorth()) + "  " + status, playerText(i, p.isBankrupt()));
                row++;
                if (roomForLists && row < h) {
                    put(y + row++, x + 4, propertyLine(game, p, w - 4), ownedText(i, p.isBankrupt()));
                }
            }
            ArrayList<Player> order = new ArrayList<>();
            game.getTurnQueue().copyInto(order);
            String label = "Turn queue (front first): ";
            put(y + row, x, label, color ? ESC + "38;5;250m" : null);
            int qx = x + label.length();
            for (int i = 0; i < order.size() && qx < x + w; i++) {
                String name = (i == 0 ? "" : " > ") + order.get(i).getName();
                if (i > 0) {
                    put(y + row, qx, " > ", color ? ESC + "38;5;250m" : null);
                }
                put(y + row, qx + (i == 0 ? 0 : 3), fit(order.get(i).getName(), Math.max(0, x + w - qx - 3)),
                        playerText(players.indexOf(order.get(i)), false));
                qx += name.length();
            }
            row++;
        }
        put(y + row++, x, rule(w), ruleStyle);

        // events of the most recent turn, each in the colour of the player it is about
        ArrayList<String> log = game.getLogger().getLines();
        if (log.isEmpty()) {
            put(y + row, x, "Welcome! Each player has a colour; the squares they buy take that colour.", null);
            return;
        }
        int start = 0;
        for (int i = log.size() - 1; i >= 0; i--) {
            if (log.get(i).startsWith("TURN|")) {
                start = i;
                break;
            }
        }
        int room = h - row;
        int first = game.isFinished() ? Math.max(0, log.size() - room) : Math.max(start, log.size() - room);
        for (int i = first; i < log.size() && row < h; i++) {
            String line = log.get(i);
            String[] f = line.split("\\|", -1);
            String who = f[0].equals("TURN") || f[0].equals("RANK") ? f[2] : f.length > 1 ? f[1] : "";
            int idx = indexOfName(players, who);
            String text = fit(rawEvents ? line : narrate(line, game), w);
            String st;
            if (!color) {
                st = null;
            } else if (f[0].equals("TURN")) {
                st = idx >= 0 ? ESC + "1;38;5;16;48;5;" + playerColor(idx) + "m" : ESC + "1;38;5;220m";
                text = " " + fit(text, w - 2) + " ";
            } else if (f[0].equals("BANKRUPT") || f[0].equals("JAIL")) {
                st = ESC + "1;38;5;196m";
            } else {
                st = idx >= 0 ? ESC + "38;5;" + playerColor(idx) + "m" : ESC + "38;5;153m";
            }
            put(y + row++, x, text, st);
        }
    }

    /** Squares owned by p in board order (walking the ring once from GO). Display only. */
    public ArrayList<String> ownedInBoardOrder(Game game, Player p) {
        ArrayList<String> names = new ArrayList<>();
        Board board = game.getBoard();
        Node<Square> node = board.getHead();
        for (int i = 0; i < board.size(); i++) {
            Square s = node.data;
            if (s instanceof OwnableSquare && ((OwnableSquare) s).getOwner() == p) {
                names.add(s.getName());
            }
            node = node.next;
        }
        return names;
    }

    /** "owns: A, B, C +2 more" shortened to the given width. */
    private String propertyLine(Game game, Player p, int width) {
        if (p.isBankrupt()) {
            return "owns: nothing (squares returned to the bank)";
        }
        ArrayList<String> names = ownedInBoardOrder(game, p);
        if (names.isEmpty()) {
            return "owns: nothing yet";
        }
        StringBuilder sb = new StringBuilder("owns: ");
        for (int i = 0; i < names.size(); i++) {
            String piece = (i == 0 ? "" : ", ") + names.get(i);
            String more = " +" + (names.size() - i) + " more";
            boolean last = i == names.size() - 1;
            if (sb.length() + piece.length() + (last ? 0 : more.length()) > width) {
                sb.append(more);
                break;
            }
            sb.append(piece);
        }
        return fit(sb.toString(), width);
    }

    private String ownedText(int idx, boolean bankrupt) {
        if (!color) {
            return null;
        }
        return bankrupt || idx < 0 ? ESC + "38;5;240m" : ESC + "38;5;" + playerColor(idx) + "m";
    }

    /**
     * Full standings report printed under the board when the game ends or is
     * stopped: rank, net worth (cash + purchase prices) and every property
     * owned, in board order, wrapped to the board width. Display only.
     */
    public String standings(Game game, String heading) {
        ArrayList<Player> players = game.getPlayers();
        String tokens = tokens(players);
        String nl = System.lineSeparator();
        int width = 98;
        StringBuilder out = new StringBuilder();
        out.append(nl).append(styled(" " + heading + " ", color ? ESC + "1;38;5;16;48;5;220m" : null)).append(nl).append(nl);
        ArrayList<Player> ranking = game.rank();
        for (int i = 0; i < ranking.size(); i++) {
            Player p = ranking.get(i);
            int idx = players.indexOf(p);
            int property = p.isBankrupt() ? 0 : p.netWorth(p.getOwned(), 0);
            String head = String.format("%d. ", i + 1);
            out.append(styled(head, color ? ESC + "1m" : null))
               .append(styled(" " + tokens.charAt(idx) + " ", swatch(idx)))
               .append(styled(" " + pad(p.getName(), 14), playerText(idx, p.isBankrupt())))
               .append(styled(p.isBankrupt()
                       ? "net worth 0 (bankrupt)"
                       : "net worth " + p.totalNetWorth() + "  =  cash " + p.getCash() + " + property " + property,
                       playerText(idx, p.isBankrupt())))
               .append(nl);
            ArrayList<String> names = p.isBankrupt() ? new ArrayList<>() : ownedInBoardOrder(game, p);
            String label = "      Properties (" + names.size() + "): ";
            if (names.isEmpty()) {
                out.append(styled(label + (p.isBankrupt() ? "none - returned to the bank" : "none"), ownedText(idx, p.isBankrupt()))).append(nl);
            } else {
                StringBuilder line = new StringBuilder(label);
                for (int k = 0; k < names.size(); k++) {
                    String piece = names.get(k) + (k < names.size() - 1 ? ", " : "");
                    if (line.length() + piece.length() > width) {
                        out.append(styled(line.toString(), ownedText(idx, false))).append(nl);
                        line = new StringBuilder(pad("", label.length()));
                    }
                    line.append(piece);
                }
                out.append(styled(line.toString(), ownedText(idx, false))).append(nl);
            }
        }
        return out.toString();
    }

    private String styled(String text, String st) {
        return st == null ? text : st + text + RESET;
    }

    private static int indexOfName(ArrayList<Player> players, String name) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i).getName().equals(name)) {
                return i;
            }
        }
        return -1;
    }

    /** Solid colour block used for a player's token in the tables. */
    private String swatch(int idx) {
        return color && idx >= 0 ? ESC + "1;38;5;16;48;5;" + playerColor(idx) + "m" : null;
    }

    private String playerText(int idx, boolean bankrupt) {
        if (!color) {
            return null;
        }
        return bankrupt || idx < 0 ? ESC + "38;5;240m" : ESC + "1;38;5;" + playerColor(idx) + "m";
    }

    // ------------------------------------------------------------- narration

    /** Turns one Appendix C log line into a plain-language sentence. */
    public String narrate(String line, Game game) {
        String[] f = line.split("\\|", -1);
        switch (f[0]) {
            case "TURN": return "-- Turn " + f[1] + ": " + f[2] + " --";
            case "ROLL": return f[1] + " rolled " + f[2] + " + " + f[3] + (f[2].equals(f[3]) ? "  (double!)" : "");
            case "MOVE": return f[1] + " moved from " + f[2] + " to " + f[3];
            case "SALARY": return f[1] + " passed GO and collected 200 (cash " + f[2] + ")";
            case "BUY": return f[1] + " bought " + f[2] + " (cash " + f[3] + ")";
            case "RENT": return f[1] + " paid " + f[3] + " rent to " + f[2] + " (cash " + f[4] + ")";
            case "TAX": return f[1] + " paid " + f[2] + " tax (cash " + f[3] + ")";
            case "CARD": return f[1] + " drew " + f[2] + ": \"" + cardText(game, f[2]) + "\"";
            case "CASH": return f[1] + (f[2].startsWith("-") ? " paid " + f[2].substring(1) : " received " + f[2].substring(1))
                    + " (cash " + f[3] + ")";
            case "JAIL": return f[1] + " was sent to Jail";
            case "JAIL_ROLL": {
                String what = f[4].equals("STAY") ? "stays in Jail" : f[4].equals("RELEASED") ? "rolled a double and is released"
                        : "pays the 50 fine and leaves";
                return f[1] + " rolled " + f[2] + " + " + f[3] + " in Jail: " + what;
            }
            case "JAIL_FREE": return f[1] + " used a Get Out of Jail Free card";
            case "BANKRUPT": return f[1] + " is BANKRUPT (creditor: " + f[2] + ")";
            case "END": return "Game over: " + (f[1].equals("LAST_PLAYER") ? "one player left" : "turn limit reached");
            case "RANK": return "#" + f[1] + " " + f[2] + " - net worth " + f[3];
            case "REVERSE": return f[1] + " reversed the turn order (twist T1)";
            case "ORDER": {
                StringBuilder order = new StringBuilder("New turn order: ");
                for (int i = 1; i < f.length; i++) {
                    order.append(i > 1 ? " > " : "").append(f[i]);
                }
                return order.toString();
            }
            case "EXPAND": return "The board grows: " + f[1] + " is built after " + f[2] + " (twist T3)";
            case "RECALL": return f[1] + " deck recalled: " + f[2] + ", " + f[3] + ", " + f[4] + " are now on top (twist T7)";
            default: return line;
        }
    }

    private String cardText(Game game, String id) {
        ArrayList<Card> cards = game.getAllCards();
        for (int i = 0; i < cards.size(); i++) {
            if (cards.get(i).getId().equals(id)) {
                return cards.get(i).getText();
            }
        }
        return id;
    }

    // ---------------------------------------------------------------- helpers

    private Player currentPlayer(Game game) {
        if (game.isOver() || game.getTurnQueue().isEmpty()) {
            return null;
        }
        return game.getTurnQueue().peek();
    }

    /** One letter per player: initials if they are unique, otherwise 1..6. */
    private String tokens(ArrayList<Player> players) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < players.size(); i++) {
            char c = Character.toUpperCase(players.get(i).getName().charAt(0));
            if (sb.indexOf(String.valueOf(c)) >= 0) {
                sb.setLength(0);
                for (int j = 0; j < players.size(); j++) {
                    sb.append((char) ('1' + j));
                }
                return sb.toString();
            }
            sb.append(c);
        }
        return sb.toString();
    }

    /** Player colours (xterm-256): cyan, pink, yellow, white, orange, sky blue. */
    private static final int[] PLAYER_COLOURS = {51, 213, 226, 255, 208, 117};
    /** Dark tint of each player colour, used as the background of squares they own. */
    private static final int[] PLAYER_TINTS = {23, 53, 58, 239, 94, 18};

    private int playerColor(int index) {
        return PLAYER_COLOURS[index % PLAYER_COLOURS.length];
    }

    private int playerTint(int index) {
        return PLAYER_TINTS[index % PLAYER_TINTS.length];
    }

    private String groupStyle(String group) {
        int[] palette = {26, 28, 166, 160, 92, 30, 130, 162, 64, 241};
        String[] known = {"BLUE", "GREEN", "ORANGE", "RED", "PURPLE", "TEAL", "BROWN", "PINK", "OLIVE", "GREY"};
        for (int i = 0; i < known.length; i++) {
            if (known[i].equalsIgnoreCase(group)) {
                return bg(palette[i]);
            }
        }
        if (!groupNames.contains(group)) {
            groupNames.add(group);
        }
        return bg(palette[groupNames.indexOf(group) % palette.length]);
    }

    private String bg(int background) {
        return bg(background, 15);
    }

    private String bg(int background, int foreground) {
        return ESC + "1;38;5;" + foreground + ";48;5;" + background + "m";
    }

    private static String join(String a, String b) {
        return b == null ? a : a + b;
    }

    private void add(ArrayList<String> lines, ArrayList<String> styles, String text, String st) {
        lines.add(text);
        styles.add(st);
    }

    private String rule(int w) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < w; i++) {
            sb.append(ascii ? '-' : '─');
        }
        return sb.toString();
    }

    private static String center(String s, int w) {
        int left = Math.max(0, (w - s.length()) / 2);
        return pad("", left) + s;
    }

    private static String pad(String s, int w) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < w) {
            sb.append(' ');
        }
        return sb.toString();
    }

    /** Shortens s to at most w characters, ending with '.' when cut. */
    private static String fit(String s, int w) {
        if (s.length() <= w) {
            return s;
        }
        return s.substring(0, Math.max(0, w - 1)) + ".";
    }

    private void put(int y, int x, String text, String st) {
        for (int i = 0; i < text.length() && x + i < grid[y].length; i++) {
            grid[y][x + i] = text.charAt(i);
            style[y][x + i] = st;
        }
    }

    private String emit(int height, int width) {
        StringBuilder out = new StringBuilder();
        for (int y = 0; y < height; y++) {
            String current = null;
            for (int x = 0; x < width; x++) {
                String st = color ? style[y][x] : null;
                if (st == null ? current != null : !st.equals(current)) {
                    out.append(RESET);
                    if (st != null) {
                        out.append(st);
                    }
                    current = st;
                }
                out.append(grid[y][x]);
            }
            if (current != null) {
                out.append(RESET);
            }
            out.append(System.lineSeparator());
        }
        return out.toString();
    }
}
