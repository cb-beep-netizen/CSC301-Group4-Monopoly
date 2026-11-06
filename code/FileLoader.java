import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

/**
 * FileLoader (added) - reads and validates the board file and the card file
 * (Appendix B). Lines starting with # and blank lines are ignored. Every error
 * names the file and line number.
 * Data flow: file -> Square objects -> CircularDoublyLinkedList (addLast);
 *            EXPAND lines (T3) -> Expansion objects -> Board's LinkedQueue
 *            (inserted at the end of turn 20, not part of the starting board);
 *            file -> Card objects -> ArrayList (shuffled later, then queued).
 * AI log: Conversation 01, Prompt 01.13; EXPAND, REVERSE and RECALL added in
 * Conversation 03, Prompt 03.02 (Claude). Reviewed and understood by: ______
 */
public class FileLoader {
    public static final int MIN_SQUARES = 20;
    public static final int MAX_SQUARES = 28;

    /** Reads non-comment lines; element 0 of each entry is the line number. */
    private static ArrayList<String[]> readLines(String path) throws IOException {
        ArrayList<String[]> result = new ArrayList<>();
        BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path), "UTF-8"));
        try {
            String line;
            int lineNo = 0;
            while ((line = reader.readLine()) != null) {
                lineNo++;
                if (lineNo == 1 && line.length() > 0 && line.charAt(0) == '﻿') {
                    line = line.substring(1); // strip a UTF-8 byte-order mark
                }
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                String[] fields = trimmed.split("\\|", -1);
                String[] withNo = new String[fields.length + 1];
                withNo[0] = String.valueOf(lineNo);
                for (int i = 0; i < fields.length; i++) {
                    withNo[i + 1] = fields[i].trim();
                }
                result.add(withNo);
            }
        } finally {
            reader.close();
        }
        return result;
    }

    private static int toInt(String value, String where) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where + ": '" + value + "' is not a whole number");
        }
    }

    private static void need(String[] f, int fields, String where, String format) {
        if (f.length - 1 != fields) {
            throw new IllegalArgumentException(where + ": expected " + format);
        }
    }

    public static Board loadBoard(String path) throws IOException {
        CircularDoublyLinkedList<Square> list = new CircularDoublyLinkedList<>();
        ArrayList<String> names = new ArrayList<>();
        ArrayList<Expansion> expansions = new ArrayList<>();
        int go = 0, jail = 0, goToJail = 0;
        ArrayList<String[]> lines = readLines(path);
        for (int i = 0; i < lines.size(); i++) {
            String[] f = lines.get(i);
            String where = path + " line " + f[0];
            String type = f[1].toUpperCase();
            if (type.equals("EXPAND")) {
                // T3: EXPAND|<existing square>|<full square definition>
                if (f.length < 4 || f[2].isEmpty()) {
                    throw new IllegalArgumentException(where + ": expected EXPAND|after square|square definition");
                }
                String after = f[2];
                if (!names.contains(after)) {
                    throw new IllegalArgumentException(where + ": EXPAND names '" + after
                            + "', which is not on the board (or not added by an earlier EXPAND line)");
                }
                String[] def = new String[f.length - 2];   // [line no, type, fields...]
                def[0] = f[0];
                for (int k = 3; k < f.length; k++) {
                    def[k - 2] = f[k];
                }
                String newType = def[1].toUpperCase();
                if (newType.equals("GO") || newType.equals("JAIL") || newType.equals("GO_TO_JAIL") || newType.equals("EXPAND")) {
                    throw new IllegalArgumentException(where + ": EXPAND cannot add a " + newType
                            + " square (the board needs exactly one GO, JAIL and GO_TO_JAIL)");
                }
                Square s = parseSquare(def, where, names);
                expansions.add(new Expansion(after, s));
                names.add(s.getName());
                continue;
            }
            if (!expansions.isEmpty()) {
                throw new IllegalArgumentException(where + ": EXPAND lines must come after every square of the starting board");
            }
            Square s = parseSquare(f, where, names);
            if (list.isEmpty() && !type.equals("GO")) {
                throw new IllegalArgumentException(where + ": the first square must be GO");
            }
            if (type.equals("GO")) go++;
            if (type.equals("JAIL")) jail++;
            if (type.equals("GO_TO_JAIL")) goToJail++;
            list.addLast(s);
            names.add(s.getName());
        }
        if (list.size() < MIN_SQUARES || list.size() > MAX_SQUARES) {
            throw new IllegalArgumentException(path + ": board has " + list.size() + " squares; it must have "
                    + MIN_SQUARES + " to " + MAX_SQUARES);
        }
        if (list.size() + expansions.size() > MAX_SQUARES) {
            throw new IllegalArgumentException(path + ": " + list.size() + " squares + " + expansions.size()
                    + " EXPAND lines would exceed " + MAX_SQUARES + " squares");
        }
        if (go != 1 || jail != 1 || goToJail != 1) {
            throw new IllegalArgumentException(path + ": board needs exactly one GO, one JAIL and one GO_TO_JAIL (found "
                    + go + ", " + jail + ", " + goToJail + ")");
        }
        Board board = new Board(list);
        for (int i = 0; i < expansions.size(); i++) {
            board.addPendingExpansion(expansions.get(i));   // file order = queue order
        }
        return board;
    }

    /**
     * Builds one square from f = [line no, type, name, fields...]. Used for the
     * starting board and for the square definition inside an EXPAND line.
     */
    private static Square parseSquare(String[] f, String where, ArrayList<String> names) {
        String type = f[1].toUpperCase();
        if (f.length < 3 || f[2].isEmpty()) {
            throw new IllegalArgumentException(where + ": missing square name");
        }
        String name = f[2];
        if (names.contains(name)) {
            throw new IllegalArgumentException(where + ": duplicate square name '" + name + "'");
        }
        switch (type) {
            case "GO":
                need(f, 2, where, "GO|name");
                return new CornerSquare(name, CornerType.GO);
            case "JAIL":
                need(f, 2, where, "JAIL|name");
                return new CornerSquare(name, CornerType.JAIL);
            case "FREE":
                need(f, 2, where, "FREE|name");
                return new CornerSquare(name, CornerType.FREE);
            case "GO_TO_JAIL":
                need(f, 2, where, "GO_TO_JAIL|name");
                return new CornerSquare(name, CornerType.GO_TO_JAIL);
            case "CHANCE":
                need(f, 2, where, "CHANCE|name");
                return new CardSquare(name, DeckType.CHANCE);
            case "COMMUNITY":
                need(f, 2, where, "COMMUNITY|name");
                return new CardSquare(name, DeckType.COMMUNITY);
            case "PROPERTY":
                need(f, 5, where, "PROPERTY|name|color|price|rent");
                return new PropertySquare(name, f[3].toUpperCase(), toInt(f[4], where), toInt(f[5], where));
            case "TRANSIT":
                need(f, 3, where, "TRANSIT|name|price");
                return new TransitSquare(name, toInt(f[3], where));
            case "TAX":
                need(f, 3, where, "TAX|name|amount");
                return new TaxSquare(name, toInt(f[3], where));
            case "EARTHQUAKE":
            case "MIRAGE":
            case "PORTAL":
                throw new IllegalArgumentException(where + ": square type " + type
                        + " belongs to a twist this group does not implement (our twists: T1, T3, T7)");
            default:
                throw new IllegalArgumentException(where + ": unknown square type '" + f[1] + "'");
        }
    }

    /** Card list before shuffling, in file order (brief 4: ArrayList). */
    public static ArrayList<Card> loadCards(String path, Board board) throws IOException {
        ArrayList<Card> cards = new ArrayList<>();
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String[]> lines = readLines(path);
        for (int i = 0; i < lines.size(); i++) {
            String[] f = lines.get(i);
            String where = path + " line " + f[0];
            if (f.length - 1 < 4 || f.length - 1 > 5) {
                throw new IllegalArgumentException(where + ": expected deck|id|text|effect|argument");
            }
            DeckType deck;
            if (f[1].equalsIgnoreCase("CHANCE")) {
                deck = DeckType.CHANCE;
            } else if (f[1].equalsIgnoreCase("COMMUNITY")) {
                deck = DeckType.COMMUNITY;
            } else {
                throw new IllegalArgumentException(where + ": deck must be CHANCE or COMMUNITY");
            }
            String id = f[2];
            if (id.isEmpty() || ids.contains(id)) {
                throw new IllegalArgumentException(where + ": card id '" + id + "' is empty or duplicated");
            }
            String effectName = f[4].toUpperCase();
            CardEffect effect;
            try {
                effect = CardEffect.valueOf(effectName);
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException(where + ": unknown card effect '" + f[4] + "'");
            }
            String arg = (f.length - 1 == 5) ? f[5] : "";
            switch (effect) {
                case MOVE_TO:
                    if (board.findByName(board.getHead(), arg, board.size()) == null) {
                        throw new IllegalArgumentException(where + ": MOVE_TO square '" + arg + "' is not on the board");
                    }
                    break;
                case NEAREST:
                    if (!arg.equalsIgnoreCase("TRANSIT")) {
                        throw new IllegalArgumentException(where + ": NEAREST supports TRANSIT only");
                    }
                    arg = "TRANSIT";
                    break;
                case MOVE_BY:
                case COLLECT:
                case PAY:
                case PAY_EACH:
                case COLLECT_EACH:
                    int value = toInt(arg, where);
                    if (effect != CardEffect.MOVE_BY && value < 0) {
                        throw new IllegalArgumentException(where + ": amount must not be negative");
                    }
                    break;
                default:
                    arg = "";
            }
            cards.add(new Card(deck, id, f[3], effect, arg));
            ids.add(id);
        }
        return cards;
    }
}
