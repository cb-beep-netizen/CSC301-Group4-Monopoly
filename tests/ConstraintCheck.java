import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;

/**
 * ConstraintCheck - static check of the CSC301 constraints on code/*.java, in
 * Java so that it runs inside Eclipse (Java port of tests/check_constraints.py,
 * same rules). Uses only ArrayList, Collections and java.io - no regex, which
 * is in java.util.regex and not on the allowed list.
 *  1. Imports: only java.util.ArrayList/Scanner/Random/Collections and java.io.*
 *  2. No banned collections, java.util.LinkedList, Comparator or streams
 *  3. The 8 required recursive methods, plus reverse (T1) and rotate (T7),
 *     contain no for/while/do and call themselves; reverse and rotate use only
 *     enqueue, dequeue and isEmpty on the queue and create no list or array
 *  4. No '% size' style index arithmetic
 * Run: GameTests runs it after the tests (Eclipse: Run > Monopoly - Tests),
 * or on its own: ConstraintCheck <code folder>.
 * AI log: Conversation 03, Prompt 03.04 (Claude). Reviewed and understood by: ______
 */
public class ConstraintCheck {

    private static final String[] ALLOWED_UTIL = {"ArrayList", "Scanner", "Random", "Collections"};
    /** Plain substrings that must not appear. */
    private static final String[] BANNED = {"ArrayDeque", "Stack", "Vector", "PriorityQueue", "Deque", "HashMap",
            "TreeMap", "HashSet", "java.util.LinkedList", "LinkedHashMap", "Hashtable", "TreeSet", "Comparator",
            ".stream(", "java.util.stream"};
    /** Generic type names that must not appear as whole words (so ArrayList< is fine). */
    private static final String[] BANNED_WORDS = {"LinkedList<", "Iterator<", "List<", "Map<", "Set<"};
    private static final String[][] REQUIRED = {
            {"Board.java", "step", "ownsGroup", "findByName", "findNearest"},
            {"Game.java", "takeTurn", "landOn"},
            {"TransitSquare.java", "transitRent"},
            {"Player.java", "netWorth"},
            {"QueueTwists.java", "reverse", "rotate"}};
    private static final String[] QUEUE_OPS = {"enqueue", "dequeue", "isEmpty"};

    public static void main(String[] args) throws IOException {
        System.exit(run(args.length > 0 ? args[0] : "code") ? 0 : 1);
    }

    /** Prints the report; returns true when every rule holds. */
    public static boolean run(String folder) throws IOException {
        ArrayList<String> problems = new ArrayList<>();
        File[] files = new File(folder).listFiles();
        if (files == null) {
            System.out.println("CONSTRAINT CHECK: cannot read folder " + new File(folder).getAbsolutePath());
            return false;
        }
        ArrayList<String> names = new ArrayList<>();
        for (File f : files) {
            if (f.getName().endsWith(".java")) {
                names.add(f.getName());
            }
        }
        Collections.sort(names);
        for (String fn : names) {
            String raw = read(new File(folder, fn));
            String src = strip(raw);
            checkImports(fn, raw, problems);
            String noArrayList = src.replace("ArrayList<", "AL<");
            for (String b : BANNED) {
                if (noArrayList.contains(b)) problems.add(fn + ": banned usage '" + b + "'");
            }
            for (String b : BANNED_WORDS) {
                if (containsWord(noArrayList, b)) problems.add(fn + ": banned usage '" + b + "'");
            }
            if (hasModSize(src)) problems.add(fn + ": '% size' index arithmetic");
            for (String[] req : REQUIRED) {
                if (!req[0].equals(fn)) continue;
                for (int k = 1; k < req.length; k++) {
                    checkMethod(fn, req[k], src, problems);
                }
            }
        }
        System.out.println();
        System.out.println("CONSTRAINT CHECK: " + (problems.isEmpty() ? "PASS" : "FAIL"));
        for (String p : problems) {
            System.out.println("  - " + p);
        }
        return problems.isEmpty();
    }

    private static void checkImports(String fn, String raw, ArrayList<String> problems) {
        for (String line : raw.split("\n")) {
            String t = line.trim();
            if (!t.startsWith("import ") || !t.endsWith(";")) continue;
            String imp = t.substring(7, t.length() - 1).trim();
            if (imp.startsWith("java.util.")) {
                String cls = imp.substring(imp.lastIndexOf('.') + 1);
                boolean ok = false;
                for (String a : ALLOWED_UTIL) if (a.equals(cls)) ok = true;
                if (!ok) problems.add(fn + ": import " + imp + " not allowed");
            } else if (!imp.startsWith("java.io.")) {
                problems.add(fn + ": import " + imp + " (check it is allowed)");
            }
        }
    }

    private static void checkMethod(String fn, String name, String src, ArrayList<String> problems) {
        String body = body(src, name);
        if (body == null) {
            problems.add(fn + ": required method " + name + " not found");
            return;
        }
        boolean loop = containsWord(body, "for") || containsWord(body, "while") || containsWord(body, "do");
        if (loop) problems.add(fn + ": " + name + " contains a loop");
        if (!callsItself(body, name)) problems.add(fn + ": " + name + " does not call itself");
        if (name.equals("reverse") || name.equals("rotate")) {
            for (String op : queueCalls(body)) {
                boolean ok = false;
                for (String q : QUEUE_OPS) if (q.equals(op)) ok = true;
                if (!ok) problems.add(fn + ": " + name + " uses q." + op + "() - only enqueue/dequeue/isEmpty allowed");
            }
            if (containsWord(body, "new") || body.contains("[]")) {
                problems.add(fn + ": " + name + " creates a list/array - not allowed");
            }
        }
        if (!loop) System.out.println("OK   " + fn + ":" + name + " - recursive, no loops");
    }

    // ------------------------------------------------------------ scanning

    private static String read(File f) throws IOException {
        StringBuilder sb = new StringBuilder();
        BufferedReader r = new BufferedReader(new InputStreamReader(new FileInputStream(f), "UTF-8"));
        String line;
        while ((line = r.readLine()) != null) {
            sb.append(line).append('\n');
        }
        r.close();
        return sb.toString();
    }

    /** Removes comments and replaces string and char literals with "" so they are not checked. */
    static String strip(String s) {
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '*') {
                int end = s.indexOf("*/", i + 2);
                i = end < 0 ? s.length() : end + 2;
            } else if (c == '/' && i + 1 < s.length() && s.charAt(i + 1) == '/') {
                int end = s.indexOf('\n', i);
                i = end < 0 ? s.length() : end;
            } else if (c == '"' || c == '\'') {
                int j = i + 1;
                while (j < s.length() && s.charAt(j) != c) {
                    j += s.charAt(j) == '\\' ? 2 : 1;
                }
                out.append(c).append(c);
                i = j + 1;
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    private static boolean isIdent(char c) {
        return Character.isLetterOrDigit(c) || c == '_' || c == '$';
    }

    /** True if word occurs with no identifier character before it (and after it, when word ends in a letter). */
    static boolean containsWord(String s, String word) {
        int from = 0;
        while (true) {
            int i = s.indexOf(word, from);
            if (i < 0) return false;
            boolean startOk = i == 0 || !isIdent(s.charAt(i - 1));
            int e = i + word.length();
            boolean endOk = !isIdent(word.charAt(word.length() - 1)) || e >= s.length() || !isIdent(s.charAt(e));
            if (startOk && endOk) return true;
            from = i + 1;
        }
    }

    /** '%' followed by something.size() or by size: index arithmetic on the board size. */
    static boolean hasModSize(String s) {
        int i = s.indexOf('%');
        while (i >= 0) {
            int j = i + 1;
            while (j < s.length() && Character.isWhitespace(s.charAt(j))) j++;
            int k = j;
            while (k < s.length() && (isIdent(s.charAt(k)) || s.charAt(k) == '.')) k++;
            String token = s.substring(j, k);
            if (token.equals("size") || (token.endsWith("size") && s.startsWith("()", k))) return true;
            i = s.indexOf('%', i + 1);
        }
        return false;
    }

    /** Body of the method declared with public/private/protected and this name, or null. */
    static String body(String src, String name) {
        int from = 0;
        while (true) {
            int i = src.indexOf(name, from);
            if (i < 0) return null;
            from = i + 1;
            if (i > 0 && isIdent(src.charAt(i - 1))) continue;
            int p = i + name.length();
            while (p < src.length() && Character.isWhitespace(src.charAt(p))) p++;
            if (p >= src.length() || src.charAt(p) != '(') continue;
            int stmt = Math.max(src.lastIndexOf(';', i), Math.max(src.lastIndexOf('}', i), src.lastIndexOf('{', i)));
            String head = src.substring(stmt + 1, i);
            if (!(containsWord(head, "public") || containsWord(head, "private") || containsWord(head, "protected"))) continue;
            int close = src.indexOf(')', p);
            int q = close + 1;
            while (q < src.length() && Character.isWhitespace(src.charAt(q))) q++;
            if (close < 0 || q >= src.length() || src.charAt(q) != '{') continue;
            int depth = 1, k = q + 1;
            while (k < src.length() && depth > 0) {
                if (src.charAt(k) == '{') depth++;
                if (src.charAt(k) == '}') depth--;
                k++;
            }
            return src.substring(q + 1, k - 1);
        }
    }

    private static boolean callsItself(String body, String name) {
        int from = 0;
        while (true) {
            int i = body.indexOf(name, from);
            if (i < 0) return false;
            from = i + 1;
            if (i > 0 && isIdent(body.charAt(i - 1))) continue;
            int p = i + name.length();
            while (p < body.length() && Character.isWhitespace(body.charAt(p))) p++;
            if (p < body.length() && body.charAt(p) == '(') return true;
        }
    }

    /** Method names called on the parameter q: q.name( */
    private static ArrayList<String> queueCalls(String body) {
        ArrayList<String> out = new ArrayList<>();
        int i = body.indexOf("q.");
        while (i >= 0) {
            if (i == 0 || !isIdent(body.charAt(i - 1))) {
                int k = i + 2;
                while (k < body.length() && isIdent(body.charAt(k))) k++;
                int p = k;
                while (p < body.length() && Character.isWhitespace(body.charAt(p))) p++;
                if (p < body.length() && body.charAt(p) == '(') out.add(body.substring(i + 2, k));
            }
            i = body.indexOf("q.", i + 1);
        }
        return out;
    }
}
