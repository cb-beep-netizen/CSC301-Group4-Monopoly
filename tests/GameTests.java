import java.util.ArrayList;
import java.util.Random;

/**
 * GameTests - scenario tests for the CSC301 Monopoly core (not submitted as game code).
 * Uses scripted dice and unshuffled decks so every expected log line is known.
 * Uses only ArrayList and Random from java.util, like the game code.
 * Run in Eclipse: Run menu > Monopoly - Tests (arguments: <test-board> <board> <cards> [code folder]).
 * After the tests it runs ConstraintCheck on the code folder.
 * AI log: Conversation 01, Prompt 01.13, updated in Prompts 01.14, 01.15 and 01.22;
 * twist tests (T1, T3, T7) added in Conversation 03, Prompt 03.02; Eclipse
 * changes (ConstraintCheck run, no-clear, defaults) in Prompt 03.04 (Claude).
 */
public class GameTests {
    static int pass = 0, fail = 0;
    static String TEST_BOARD, BOARD, CARDS, CODE;

    static void check(String name, boolean ok, String detail) {
        if (ok) { pass++; System.out.println("PASS  " + name); }
        else { fail++; System.out.println("FAIL  " + name + "\n      " + detail); }
    }

    static void eqLines(String name, ArrayList<String> actual, String... expected) {
        ArrayList<String> exp = list(expected);
        check(name, actual.equals(exp), "expected " + exp + "\n      actual   " + actual);
    }

    @SafeVarargs
    static <T> ArrayList<T> list(T... xs) {
        ArrayList<T> out = new ArrayList<>();
        for (T x : xs) out.add(x);
        return out;
    }

    static BuyDecider NEVER = (p, s) -> false;
    static BuyDecider ALWAYS = (p, s) -> p.getCash() >= s.getPrice();

    static Game game(String boardPath, ArrayList<Card> cards, BuyDecider d, Integer[] dice, String... names) throws Exception {
        Board b = FileLoader.loadBoard(boardPath);
        Dice dc = new Dice(new Random(1), list(dice));
        Game g = new Game(b, cards, new Random(1), false, dc, new GameLogger(null, false), d, 60);
        for (String n : names) g.addPlayer(n);
        return g;
    }

    static Node<Square> at(Game g, String name) {
        Board b = g.getBoard();
        return b.findByName(b.getHead(), name, b.size());
    }

    static void own(Game g, Player p, String square) {
        OwnableSquare s = (OwnableSquare) at(g, square).data;
        s.setOwner(p);
        p.addOwned(s);
    }

    static ArrayList<Card> cards(Card... cs) { return list(cs); }
    static Card ch(String id, CardEffect e, String a) { return new Card(DeckType.CHANCE, id, id, e, a); }
    static Card cm(String id, CardEffect e, String a) { return new Card(DeckType.COMMUNITY, id, id, e, a); }

    static <T> ArrayList<T> contents(LinkedQueue<T> q) { ArrayList<T> l = new ArrayList<>(); q.copyInto(l); return l; }
    static ArrayList<String> names(LinkedQueue<Player> q) {
        ArrayList<String> out = new ArrayList<>();
        for (Player p : contents(q)) out.add(p.getName());
        return out;
    }

    public static void main(String[] args) throws Exception {
        TEST_BOARD = args[0]; BOARD = args[1]; CARDS = args[2];
        CODE = args.length > 3 ? args[3] : "code";
        dataStructures();
        scenarioGoBack3();
        scenarioRentBankruptWithJailFree();
        scenarioCollectEach();
        scenarioJailThirdAttemptBankrupt();
        scenarioJailThirdAttemptPays();
        scenarioThreeDoubles();
        scenarioTwoDoublesThenNormal();
        scenarioPayEachBankrupt();
        scenarioJailDoubleNoExtraRoll();
        scenarioJailFreeCardUse();
        scenarioMoveToPassesGo();
        scenarioRealBoardCards();
        scenarioRents();
        scenarioTaxBankrupt();
        scenarioGoToJailOnDouble();
        scenarioCardReturnsToBack();
        scenarioRanking();
        scenarioLoaderErrors();
        boardView();
        twistQueueMethods();
        twistT1Reverse();
        twistT7Recall();
        twistT3Expansion();
        twistT3LoaderErrors();
        twistCardMovesAvoidCardSquares();
        eclipseSupport();
        determinismAndFuzz();
        System.out.println();
        System.out.println("RESULT: " + pass + " passed, " + fail + " failed");
        boolean constraints = ConstraintCheck.run(args.length > 3 ? args[3] : "code");   // static check, same run
        System.exit(fail == 0 && constraints ? 0 : 1);
    }

    static void dataStructures() {
        LinkedQueue<String> q = new LinkedQueue<>();
        String msg = "";
        try { q.dequeue(); } catch (IllegalStateException e) { msg = e.getMessage(); }
        check("queue: dequeue on empty throws a clear message", msg.contains("empty"), msg);
        try { q.peek(); msg = ""; } catch (IllegalStateException e) { msg = e.getMessage(); }
        check("queue: peek on empty throws a clear message", msg.contains("empty"), msg);
        q.enqueue("A"); q.enqueue("B"); q.enqueue("C"); q.enqueue("D");
        check("queue: FIFO peek/size", q.peek().equals("A") && q.size() == 4, "");
        q.remove("C");
        check("queue: remove middle keeps order", contents(q).equals(list("A", "B", "D")), contents(q).toString());
        check("queue: remove missing returns false", !q.remove("Z") && q.size() == 3, "");

        CircularDoublyLinkedList<String> l = new CircularDoublyLinkedList<>();
        l.addLast("GO"); l.addLast("X"); l.addLast("Y");
        Node<String> h = l.getHead();
        check("list: last.next == head and head.prev == last", h.prev.data.equals("Y") && h.prev.next == h, "");
        Node<String> n = l.insertAfter(h.next, "X2");
        check("list: insertAfter links both ways", h.next.next == n && n.prev == h.next && n.next.data.equals("Y") && l.size() == 4, "");
        l.remove(h);
        check("list: removing head moves head and keeps the ring", l.getHead().data.equals("X") && l.getHead().prev.data.equals("Y")
                && l.getHead().prev.next == l.getHead() && l.size() == 3, "");
    }

    // Brief 8.2: two squares before GO, roll 5, land on Chance, "Go back 3" -> one salary only.
    static void scenarioGoBack3() throws Exception {
        Game g = game(TEST_BOARD, cards(ch("C2", CardEffect.MOVE_BY, "-3")), NEVER, new Integer[]{2, 3}, "A", "B");
        Player a = g.getPlayers().get(0);
        a.setPosition(at(g, "P9"));
        g.playOneTurn();
        eqLines("8.2 go back 3 past GO: one salary, backward move pays nothing", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|2|3", "MOVE|A|P9|Chance", "SALARY|A|1700", "CARD|A|C2", "MOVE|A|Chance|GO");
    }

    // Brief 8.2: Sara owes 300 rent, has 120 and a JAIL_FREE card.
    static void scenarioRentBankruptWithJailFree() throws Exception {
        Card k5 = cm("K5", CardEffect.JAIL_FREE, "");
        Card k1 = cm("K1", CardEffect.COLLECT, "200");
        Game g = game(TEST_BOARD, cards(k1), NEVER, new Integer[]{1, 3}, "Sara", "Omar", "Ali");
        Player sara = g.getPlayers().get(0), omar = g.getPlayers().get(1);
        sara.setPosition(at(g, "P2")); sara.setCash(120); sara.addJailFreeCard(k5);
        own(g, sara, "P1"); own(g, omar, "P3");
        g.playOneTurn();
        eqLines("8.2 Sara: rent shows amount actually paid, then BANKRUPT", g.getLogger().getLines(),
                "TURN|1|Sara", "ROLL|Sara|1|3", "MOVE|Sara|P2|P3", "RENT|Sara|Omar|120|0|1620", "BANKRUPT|Sara|Omar");
        check("8.2 Sara: her square returns to the bank", ((OwnableSquare) at(g, "P1").data).getOwner() == null && sara.getOwned().isEmpty(), "");
        ArrayList<Card> deck = contents(g.getCommunity());
        check("8.2 Sara: JAIL_FREE card back at the bottom of its deck", deck.size() == 2 && deck.get(1) == k5 && sara.getJailFreeCards().isEmpty(), deck.toString());
        check("8.2 Sara: turn queue Omar, Ali (Sara not re-queued)", names(g.getTurnQueue()).equals(list("Omar", "Ali")), names(g.getTurnQueue()).toString());
    }

    // Brief 8.2: Sara draws COLLECT_EACH 10; queue Omar (5 AED), Ali.
    static void scenarioCollectEach() throws Exception {
        Game g = game(TEST_BOARD, cards(cm("K3", CardEffect.COLLECT_EACH, "10")), NEVER, new Integer[]{1, 2}, "Sara", "Omar", "Ali");
        Player sara = g.getPlayers().get(0), omar = g.getPlayers().get(1);
        sara.setPosition(at(g, "Tax")); omar.setCash(5);
        g.playOneTurn();
        eqLines("8.2 COLLECT_EACH: queue order, payer line first, Omar bankrupt to Sara", g.getLogger().getLines(),
                "TURN|1|Sara", "ROLL|Sara|1|2", "MOVE|Sara|Tax|Community", "CARD|Sara|K3",
                "CASH|Omar|-5|0", "CASH|Sara|+5|1505", "BANKRUPT|Omar|Sara", "CASH|Ali|-10|1490", "CASH|Sara|+10|1515");
        check("8.2 COLLECT_EACH: queue after the turn is Ali, Sara", names(g.getTurnQueue()).equals(list("Ali", "Sara")), names(g.getTurnQueue()).toString());
    }

    // Brief 8.2: third failed attempt, roll 2 and 3, 30 AED.
    static void scenarioJailThirdAttemptBankrupt() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{2, 3}, "A", "B");
        Player a = g.getPlayers().get(0);
        a.setPosition(at(g, "Jail")); a.setInJail(true); a.setJailAttempts(2); a.setCash(30);
        g.playOneTurn();
        eqLines("8.2 Jail third attempt with 30 AED: FINE, CASH -30, BANKRUPT", g.getLogger().getLines(),
                "TURN|1|A", "JAIL_ROLL|A|2|3|FINE", "CASH|A|-30|0", "BANKRUPT|A|BANK");
        check("8.2 Jail third attempt: game ends LAST_PLAYER", "LAST_PLAYER".equals(g.getEndReason()), String.valueOf(g.getEndReason()));
    }

    static void scenarioJailThirdAttemptPays() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{2, 3}, "A", "B");
        Player a = g.getPlayers().get(0);
        a.setPosition(at(g, "Jail")); a.setInJail(true); a.setJailAttempts(2);
        g.playOneTurn();
        eqLines("Jail fine order: JAIL_ROLL FINE, CASH -50, MOVE", g.getLogger().getLines(),
                "TURN|1|A", "JAIL_ROLL|A|2|3|FINE", "CASH|A|-50|1450", "MOVE|A|Jail|P6");
        check("Jail fine: player released", !a.isInJail() && a.getJailAttempts() == 0, "");
    }

    static void scenarioThreeDoubles() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{1, 1, 2, 2, 3, 3}, "A", "B");
        g.playOneTurn();
        Player a = g.getPlayers().get(0);
        eqLines("Third double: ROLL then JAIL, no MOVE", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|1", "MOVE|A|GO|P2", "ROLL|A|2|2", "MOVE|A|P2|P3", "ROLL|A|3|3", "JAIL|A");
        check("Third double: player is in Jail on the JAIL node", a.isInJail() && a.getPosition() == g.getBoard().getJail(), "");
    }

    // Brief 8.2: trace takeTurn() for two doubles then a non-double.
    static void scenarioTwoDoublesThenNormal() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{1, 1, 2, 2, 1, 2}, "A", "B");
        g.playOneTurn();
        eqLines("8.2 two doubles then a non-double: three moves, turn ends", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|1", "MOVE|A|GO|P2", "ROLL|A|2|2", "MOVE|A|P2|P3", "ROLL|A|1|2", "MOVE|A|P3|P4");
    }

    static void scenarioPayEachBankrupt() throws Exception {
        Game g = game(TEST_BOARD, cards(cm("K2", CardEffect.PAY_EACH, "50")), NEVER, new Integer[]{1, 2}, "Sara", "Omar", "Ali", "Bea");
        Player sara = g.getPlayers().get(0);
        sara.setPosition(at(g, "Tax")); sara.setCash(60);
        g.playOneTurn();
        eqLines("PAY_EACH: partial payment to Ali, Sara bankrupt, Bea not paid", g.getLogger().getLines(),
                "TURN|1|Sara", "ROLL|Sara|1|2", "MOVE|Sara|Tax|Community", "CARD|Sara|K2",
                "CASH|Sara|-50|10", "CASH|Omar|+50|1550", "CASH|Sara|-10|0", "CASH|Ali|+10|1510", "BANKRUPT|Sara|Ali");
        check("PAY_EACH: queue Omar, Ali, Bea", names(g.getTurnQueue()).equals(list("Omar", "Ali", "Bea")), names(g.getTurnQueue()).toString());
    }

    static void scenarioJailDoubleNoExtraRoll() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{2, 2, 6, 6}, "A", "B");
        Player a = g.getPlayers().get(0);
        a.setPosition(at(g, "Jail")); a.setInJail(true); a.setJailAttempts(1);
        g.playOneTurn();
        eqLines("Jail double: RELEASED, move, no extra roll", g.getLogger().getLines(),
                "TURN|1|A", "JAIL_ROLL|A|2|2|RELEASED", "MOVE|A|Jail|P5");
    }

    static void scenarioJailFreeCardUse() throws Exception {
        Card c8 = ch("C8", CardEffect.JAIL_FREE, "");
        Card c7 = ch("C7", CardEffect.COLLECT, "50");
        Game g = game(TEST_BOARD, cards(c7), NEVER, new Integer[]{1, 2}, "A", "B");
        Player a = g.getPlayers().get(0);
        a.setPosition(at(g, "Jail")); a.setInJail(true); a.addJailFreeCard(c8);
        g.playOneTurn();
        eqLines("JAIL_FREE card: used, then a normal turn", g.getLogger().getLines(),
                "TURN|1|A", "JAIL_FREE|A", "ROLL|A|1|2", "MOVE|A|Jail|T2");
        ArrayList<Card> deck = contents(g.getChance());
        check("JAIL_FREE card: returned to the back of its deck", deck.size() == 2 && deck.get(1) == c8, deck.toString());
    }

    static void scenarioMoveToPassesGo() throws Exception {
        Game g = game(TEST_BOARD, cards(ch("C1", CardEffect.MOVE_TO, "P1")), NEVER, new Integer[]{1, 2}, "A", "B");
        g.playOneTurn();
        eqLines("MOVE_TO behind the player: moves forward round the board, salary paid", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|2", "MOVE|A|GO|Chance", "CARD|A|C1", "MOVE|A|Chance|P1", "SALARY|A|1700");
    }

    static void scenarioRealBoardCards() throws Exception {
        Board b = FileLoader.loadBoard(BOARD);
        ArrayList<Card> all = FileLoader.loadCards(CARDS, b);
        check("Real files: 24 squares (+3 EXPAND), 24 cards", b.size() == 24 && all.size() == 24
                && b.getPendingExpansions().size() == 3, b.size() + " " + all.size());
        Card c3 = null, k9 = null;
        for (Card c : all) { if (c.getId().equals("C3")) c3 = c; if (c.getId().equals("K9")) k9 = c; }
        Game g = game(BOARD, cards(c3), NEVER, new Integer[]{1, 2}, "A", "B");
        g.getPlayers().get(0).setPosition(at(g, "Mussafah"));
        g.playOneTurn();
        eqLines("C3 NEAREST from Chance Desk wraps past GO to Metro Red Line", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|2", "MOVE|A|Mussafah|Chance Desk", "CARD|A|C3", "MOVE|A|Chance Desk|Metro Red Line", "SALARY|A|1700");
        Game g2 = game(BOARD, cards(k9), NEVER, new Integer[]{1, 1, 3, 4}, "A", "B");
        g2.playOneTurn();
        eqLines("K9 MOVE_TO Corniche Road from Community Chest: full lap, salary", g2.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|1", "MOVE|A|GO|Community Chest", "CARD|A|K9", "MOVE|A|Community Chest|Corniche Road", "SALARY|A|1700",
                "ROLL|A|3|4", "MOVE|A|Corniche Road|Saadiyat Beach");
    }

    static void scenarioRents() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{}, "A", "B");
        Player a = g.getPlayers().get(0);
        TransitSquare t1 = (TransitSquare) at(g, "T1").data;
        check("transitRent(1..3) = 25, 50, 100", t1.transitRent(1) == 25 && t1.transitRent(2) == 50 && t1.transitRent(3) == 100, "");
        own(g, a, "T1"); own(g, a, "T2");
        check("transit rent with 2 owned = 50", t1.rent(g.getBoard()) == 50, String.valueOf(t1.rent(g.getBoard())));
        own(g, a, "P1");
        PropertySquare p1 = (PropertySquare) at(g, "P1").data;
        check("property rent without full group = base", p1.rent(g.getBoard()) == 2, String.valueOf(p1.rent(g.getBoard())));
        own(g, a, "P2");
        check("property rent with full colour group = double", p1.rent(g.getBoard()) == 4, String.valueOf(p1.rent(g.getBoard())));
        check("netWorth sums purchase prices recursively", a.netWorth(a.getOwned(), 0) == 520 && a.totalNetWorth() == 2020, String.valueOf(a.netWorth(a.getOwned(), 0)));
    }

    static void scenarioTaxBankrupt() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{1, 3}, "A", "B", "C");
        Player a = g.getPlayers().get(0);
        a.setCash(40);
        g.playOneTurn();
        eqLines("TAX bankruptcy: amount actually paid, then BANKRUPT to BANK", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|3", "MOVE|A|GO|Tax", "TAX|A|40|0", "BANKRUPT|A|BANK");
    }

    static void scenarioGoToJailOnDouble() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{2, 2, 5, 6}, "A", "B");
        g.getPlayers().get(0).setPosition(at(g, "Free"));
        g.playOneTurn();
        eqLines("Sent to Jail on a double: turn ends, no extra roll", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|2|2", "MOVE|A|Free|Go To Jail", "JAIL|A");
    }

    static void scenarioCardReturnsToBack() throws Exception {
        Card c7 = ch("C7", CardEffect.COLLECT, "50"), c4 = ch("C4", CardEffect.PAY, "15");
        Game g = game(TEST_BOARD, cards(c7, c4), NEVER, new Integer[]{1, 2}, "A", "B");
        g.playOneTurn();
        eqLines("COLLECT card: CARD then CASH +50", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|2", "MOVE|A|GO|Chance", "CARD|A|C7", "CASH|A|+50|1550");
        ArrayList<Card> deck = contents(g.getChance());
        check("Drawn card goes to the back of its deck", deck.get(0) == c4 && deck.get(1) == c7, deck.toString());
    }

    static void scenarioRanking() throws Exception {
        Game g = game(TEST_BOARD, cards(), NEVER, new Integer[]{}, "A", "B", "C", "D");
        ArrayList<Player> ps = g.getPlayers();
        own(g, ps.get(1), "P1");           // B: 1500 + 60
        ps.get(0).setCash(1560);           // A: 1560, same as B, joined first
        ps.get(2).setBankrupt(true); ps.get(3).setBankrupt(true);
        // C bankrupt first, D second -> D listed before C
        java.lang.reflect.Field f = Game.class.getDeclaredField("bankruptOrder");
        f.setAccessible(true);
        @SuppressWarnings("unchecked") ArrayList<Player> order = (ArrayList<Player>) f.get(g);
        order.add(ps.get(2)); order.add(ps.get(3));
        ArrayList<String> r = new ArrayList<>();
        for (Player p : g.rank()) r.add(p.getName());
        check("Ranking: tie broken by join order; latest bankruptcy first", r.equals(list("A", "B", "D", "C")), r.toString());
    }

    static void scenarioLoaderErrors() throws Exception {
        java.io.File tmp = java.io.File.createTempFile("bad", ".txt");
        java.io.PrintWriter w = new java.io.PrintWriter(tmp);
        w.println("GO|GO"); w.println("JAIL|Jail"); w.println("JAIL|Jail2");
        w.close();
        String msg = "";
        try { FileLoader.loadBoard(tmp.getPath()); } catch (IllegalArgumentException e) { msg = e.getMessage(); }
        check("Loader rejects a board with too few squares / two JAILs", msg.contains("squares") || msg.contains("JAIL"), msg);
        tmp.delete();
    }

    static void boardView() throws Exception {
        Game g = game(BOARD, cards(), ALWAYS, new Integer[]{3, 4, 1, 2}, "Sara", "Omar", "Ali");
        g.playOneTurn();
        g.playOneTurn();
        BoardView plain = new BoardView(false, true, false);
        String[] rows = plain.build(g).split(System.lineSeparator());
        boolean sameWidth = true;
        for (String r : rows) if (r.length() != 99) sameWidth = false;
        check("Board view (24 squares): 36 rows x 99 columns, fits a 120-column console", rows.length == 36 && sameWidth, rows.length + " rows");
        String screen = plain.build(g);
        check("Board view shows squares, owner chip, tokens and events",
                screen.contains("Yas Island    [S]".substring(0, 10)) && screen.contains("GREEN    [S]") && screen.contains("Omar rolled 1 + 2")
                && screen.contains("Turn queue (front first): Ali > Sara > Omar"), screen);
        String coloured = new BoardView(true, false, false).build(g);
        check("Board view in colour uses ANSI codes and box characters", coloured.contains("\u001B[") && coloured.contains("\u253C"), "");
        Game small = game(TEST_BOARD, cards(), NEVER, new Integer[]{}, "A", "B");
        String[] smallRows = plain.build(small).split(System.lineSeparator());
        check("Board view adapts to a 20-square board (6 x 6 ring)", smallRows.length == 31 && smallRows[0].length() == 85, smallRows.length + "x" + smallRows[0].length());
        Game done = game(TEST_BOARD, cards(), NEVER, new Integer[]{2, 3}, "A", "B");
        Player a = done.getPlayers().get(0);
        a.setPosition(at(done, "Jail")); a.setInJail(true); a.setJailAttempts(2); a.setCash(30);
        done.playOneTurn();
        done.finish();
        String end = plain.build(done);
        check("Board view final screen shows GAME OVER and the ranking", end.contains("GAME OVER (LAST_PLAYER)") && end.contains("1.  B ") && end.contains("bankrupt"), end);
        // properties owned: centre panel, quit standings, final report
        Game g3 = game(BOARD, cards(), ALWAYS, new Integer[]{3, 4, 1, 2, 2, 3}, "Sara", "Omar", "Ali");
        g3.playOneTurn(); g3.playOneTurn(); g3.playOneTurn();
        String live = plain.build(g3);
        check("Centre panel lists each player's properties", live.contains("owns: Yas Island") && live.contains("owns: Al Bateen")
                && live.contains("owns: Metro Red Line"), live);
        BoardView stop = new BoardView(false, true, false);
        stop.setStandingsMode(true);
        String stopped = stop.build(g3);
        check("Quit screen: centre shows STANDINGS with net worth and properties", stopped.contains("GAME STOPPED")
                && stopped.contains("STANDINGS (game stopped)") && stopped.contains("owns: Yas Island"), stopped);
        String report = stop.standings(g3, "GAME STOPPED after turn 3");
        check("Standings report: rank, net worth = cash + property, property list",
                report.contains("1.") && report.contains("net worth 1500  =  cash 1300 + property 200")
                && report.contains("Properties (1): Metro Red Line"), report);
        check("Narration of a log line", plain.narrate("RENT|Sara|Omar|12|1318|1252", g).equals("Sara paid 12 rent to Omar (cash 1318)"), "");
    }

    // ================================================================ twists

    static <T> String q(LinkedQueue<T> queue) { return contents(queue).toString(); }

    static void twistQueueMethods() {
        LinkedQueue<Integer> a = new LinkedQueue<>();
        for (int i = 1; i <= 5; i++) a.enqueue(i);
        QueueTwists.reverse(a);
        check("T1 reverse(1..5) = 5,4,3,2,1", q(a).equals("[5, 4, 3, 2, 1]") && a.size() == 5, q(a));
        LinkedQueue<Integer> e = new LinkedQueue<>();
        QueueTwists.reverse(e);
        check("T1 reverse(empty) stays empty", e.isEmpty(), "");
        LinkedQueue<Integer> one = new LinkedQueue<>(); one.enqueue(7);
        QueueTwists.reverse(one);
        check("T1 reverse(single) unchanged", q(one).equals("[7]"), q(one));
        LinkedQueue<Integer> r = new LinkedQueue<>();
        for (int i = 1; i <= 5; i++) r.enqueue(i);
        QueueTwists.rotate(r, 2);
        check("T7 rotate(1..5, 2) = 3,4,5,1,2", q(r).equals("[3, 4, 5, 1, 2]"), q(r));
        QueueTwists.rotate(r, 0);
        check("T7 rotate(q, 0) unchanged", q(r).equals("[3, 4, 5, 1, 2]"), q(r));
        QueueTwists.rotate(r, 5);
        check("T7 rotate by size() is a full turn", q(r).equals("[3, 4, 5, 1, 2]"), q(r));
        r.enqueue(6);
        check("Queue still FIFO after reverse/rotate (rear pointer intact)", q(r).equals("[3, 4, 5, 1, 2, 6]") && r.size() == 6, q(r));
    }

    // Brief 8.2 (T1): A draws REVERSE while the queue holds B, C, D. Who plays the next four turns?
    static void twistT1Reverse() throws Exception {
        Game g = game(TEST_BOARD, cards(ch("R1", CardEffect.REVERSE, "")), NEVER,
                new Integer[]{1, 2, 2, 3, 2, 3, 2, 3, 2, 3}, "A", "B", "C", "D");
        g.playOneTurn();
        eqLines("T1 REVERSE: CARD, REVERSE, then ORDER of the queue after reversal", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|2", "MOVE|A|GO|Chance", "CARD|A|R1", "REVERSE|A", "ORDER|D|C|B");
        check("T1 8.2: queue after A's turn is D, C, B, A", names(g.getTurnQueue()).equals(list("D", "C", "B", "A")), names(g.getTurnQueue()).toString());
        g.playOneTurn(); g.playOneTurn(); g.playOneTurn(); g.playOneTurn();
        ArrayList<String> turns = new ArrayList<>();
        for (String l : g.getLogger().getLines()) if (l.startsWith("TURN|") && !l.equals("TURN|1|A")) turns.add(l);
        check("T1 8.2: the next four turns are D, C, B, A", turns.equals(list("TURN|2|D", "TURN|3|C", "TURN|4|B", "TURN|5|A")), turns.toString());
        ArrayList<Card> deck = contents(g.getChance());
        check("T1 REVERSE card returns to the back of its deck", deck.size() == 1 && deck.get(0).getId().equals("R1"), deck.toString());

        // two players: reversing a one-player queue changes nothing, ORDER still printed
        Game g2 = game(TEST_BOARD, cards(ch("R1", CardEffect.REVERSE, "")), NEVER, new Integer[]{1, 2}, "A", "B");
        g2.playOneTurn();
        eqLines("T1 with two players: ORDER lists the one waiting player", g2.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|2", "MOVE|A|GO|Chance", "CARD|A|R1", "REVERSE|A", "ORDER|B");

        // REVERSE before PAY_EACH: payments follow the new queue order
        Game g3 = game(TEST_BOARD, cards(ch("R1", CardEffect.REVERSE, ""), cm("K2", CardEffect.PAY_EACH, "10")), NEVER,
                new Integer[]{1, 2, 1, 2}, "A", "B", "C");
        g3.playOneTurn();                                         // queue now C, B, A
        g3.getPlayers().get(2).setPosition(at(g3, "Tax"));
        g3.playOneTurn();                                         // C: Tax + 3 -> Community
        ArrayList<String> l3 = g3.getLogger().getLines();
        check("T1 then PAY_EACH: C pays in the reversed queue order (B, then A)",
                l3.contains("CASH|B|+10|1510") && l3.indexOf("CASH|B|+10|1510") < l3.indexOf("CASH|A|+10|1510") && l3.contains("TURN|2|C"), l3.toString());
    }

    static void twistT7Recall() throws Exception {
        Card rc = ch("RC", CardEffect.RECALL, "");
        Card c1 = ch("C1", CardEffect.COLLECT, "1"), c2 = ch("C2", CardEffect.COLLECT, "2"), c3 = ch("C3", CardEffect.COLLECT, "3"),
             c4 = ch("C4", CardEffect.COLLECT, "4"), c5 = ch("C5", CardEffect.COLLECT, "5");
        Game g = game(TEST_BOARD, cards(rc, c1, c2, c3, c4, c5), NEVER, new Integer[]{1, 2}, "A", "B");
        g.playOneTurn();
        eqLines("T7 RECALL: CARD, then RECALL with the three cards now at the front", g.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|2", "MOVE|A|GO|Chance", "CARD|A|RC", "RECALL|CHANCE|C3|C4|C5");
        check("T7 deck after RECALL: C3, C4, C5, C1, C2, then RC at the back",
                contents(g.getChance()).equals(list(c3, c4, c5, c1, c2, rc)), contents(g.getChance()).toString());

        Game g2 = game(TEST_BOARD, cards(rc, c1, c2, c3), NEVER, new Integer[]{1, 2}, "A", "B");
        g2.playOneTurn();
        eqLines("T7 with 3 other cards: nothing changes, no RECALL line", g2.getLogger().getLines(),
                "TURN|1|A", "ROLL|A|1|2", "MOVE|A|GO|Chance", "CARD|A|RC");
        check("T7 with 3 other cards: deck C1, C2, C3, RC", contents(g2.getChance()).equals(list(c1, c2, c3, rc)), contents(g2.getChance()).toString());

        // 4 other cards, one of them held by a player as JAIL_FREE: only 3 left in the deck -> no change
        Card jf = ch("JF", CardEffect.JAIL_FREE, "");
        Game g3 = game(TEST_BOARD, cards(rc, c1, c2, c3), NEVER, new Integer[]{1, 2}, "A", "B");
        g3.getPlayers().get(1).addJailFreeCard(jf);
        g3.playOneTurn();
        check("T7 counts only cards in the deck (held JAIL_FREE cards are not)", !g3.getLogger().getLines().toString().contains("RECALL|"), "");

        // exactly 4 other cards: rotate 1 -> last three at the front
        Game g4 = game(TEST_BOARD, cards(rc, c1, c2, c3, c4), NEVER, new Integer[]{1, 2}, "A", "B");
        g4.playOneTurn();
        check("T7 with 4 other cards: RECALL|CHANCE|C2|C3|C4", g4.getLogger().getLines().contains("RECALL|CHANCE|C2|C3|C4")
                && contents(g4.getChance()).equals(list(c2, c3, c4, c1, rc)), contents(g4.getChance()).toString());
    }

    static void twistT3Expansion() throws Exception {
        Game g = game(BOARD, cards(), NEVER, new Integer[]{}, "Sara", "Omar", "Ali");
        Board b = g.getBoard();
        Player sara = g.getPlayers().get(0);
        own(g, sara, "Corniche Road"); own(g, sara, "Al Bateen");
        PropertySquare corniche = (PropertySquare) at(g, "Corniche Road").data;
        int rentBefore = corniche.rent(b);
        Node<Square> lastBefore = b.getGo().prev;
        for (int i = 0; i < 19; i++) g.playOneTurn();
        check("T3: no expansion before the end of turn 20", b.size() == 24 && !g.getLogger().getLines().toString().contains("EXPAND|"), "");
        int before = g.getLogger().getLines().size();
        g.playOneTurn();
        ArrayList<String> t20 = new ArrayList<>(g.getLogger().getLines().subList(before, g.getLogger().getLines().size()));
        ArrayList<String> exp = new ArrayList<>();
        for (String l : t20) if (l.startsWith("EXPAND|")) exp.add(l);
        check("T3: three EXPAND lines in file order at the end of turn 20",
                exp.equals(list("EXPAND|Al Mina|Al Bateen", "EXPAND|Abu Dhabi Airport Express|Metro Green Line", "EXPAND|Lulu Island|Hudayriyat Island"))
                && t20.get(t20.size() - 1).startsWith("EXPAND|") && t20.get(0).equals("TURN|20|Omar"), t20.toString());
        check("T3: board now has 27 squares, pending queue empty", b.size() == 27 && b.getPendingExpansions().isEmpty(), String.valueOf(b.size()));
        Game gm = game(BOARD, cards(), NEVER, new Integer[]{}, "A", "B", "C");
        Player pa = gm.getPlayers().get(0), pb = gm.getPlayers().get(1), pc = gm.getPlayers().get(2);
        Node<Square> na = at(gm, "Al Bateen"), nb = at(gm, "Hudayriyat Island"), nc = at(gm, "Municipality Fee");
        pa.setPosition(na); pb.setPosition(nb); pc.setPosition(nc);
        int cashA = pa.getCash();
        gm.applyExpansions();
        check("T3: players are not moved by the insertion (same nodes, no salary)", pa.getPosition() == na && pb.getPosition() == nb
                && pc.getPosition() == nc && pa.getCash() == cashA && na.next.data.getName().equals("Al Mina") && nc.prev.data.getName().equals("Al Mina"), "");
        Node<Square> bateen = at(g, "Al Bateen"), mina = at(g, "Al Mina"), lulu = at(g, "Lulu Island");
        check("T3: Al Mina linked both ways between Al Bateen and Municipality Fee",
                bateen.next == mina && mina.prev == bateen && mina.next.data.getName().equals("Municipality Fee") && mina.next.prev == mina, "");
        check("T3: Lulu Island inserted after the last square: Hudayriyat.next = Lulu, Lulu.next = GO, GO.prev = Lulu",
                lastBefore.next == lulu && lulu.prev == lastBefore && lulu.next == b.getGo() && b.getGo().prev == lulu && b.getHead() == b.getGo(), "");
        check("T3: Airport Express after Metro Green Line, before Free Parking",
                at(g, "Metro Green Line").next.data.getName().equals("Abu Dhabi Airport Express")
                && at(g, "Abu Dhabi Airport Express").next.data.getName().equals("Free Parking"), "");
        check("T3: BLUE group now has 3 squares - Sara's doubled rent drops back to base",
                rentBefore == 8 && corniche.rent(b) == 4, rentBefore + " -> " + corniche.rent(b));
        // salary still paid at GO across the new last square
        Node<Square> mid = b.step(lastBefore, 1, sara);
        int cash = sara.getCash();
        Node<Square> land = b.step(lastBefore, 2, sara);
        check("T3: Hudayriyat +1 = Lulu Island (no salary), +2 = GO (salary)", mid == lulu && land == b.getGo() && sara.getCash() == cash + 200, "");
        g.applyExpansions();
        check("T3: applying again does nothing (queue already empty)", b.size() == 27, "");
        // max-turns 20: EXPAND lines come before END
        Game g2 = new Game(FileLoader.loadBoard(BOARD), new ArrayList<>(), new Random(5), false,
                new Dice(new Random(5)), new GameLogger(null, false), NEVER, 20);
        g2.addPlayer("A"); g2.addPlayer("B");
        g2.play();
        ArrayList<String> l2 = g2.getLogger().getLines();
        check("T3 with --max-turns 20: EXPAND lines, then END|MAX_TURNS", l2.indexOf("END|MAX_TURNS") == l2.indexOf("EXPAND|Lulu Island|Hudayriyat Island") + 1, "");
        BoardView plain = new BoardView(false, true, false);
        String[] rows = plain.build(g).split(System.lineSeparator());
        check("T3: board view redraws the 27-square ring (8 x 8, 41 rows x 113 columns)", rows.length == 41 && rows[0].length() == 113
                && plain.build(g).contains("Lulu Island") && plain.build(g).contains("Al Mina"), rows.length + "x" + rows[0].length());
        check("Narration of the twist log lines", plain.narrate("EXPAND|Al Mina|Al Bateen", g).contains("Al Mina is built after Al Bateen")
                && plain.narrate("ORDER|D|C|B", g).equals("New turn order: D > C > B")
                && plain.narrate("RECALL|CHANCE|C3|C4|C5", g).contains("C3, C4, C5"), "");
    }

    static String loadError(String... boardLines) throws Exception {
        java.io.File tmp = java.io.File.createTempFile("exp", ".txt");
        java.io.PrintWriter w = new java.io.PrintWriter(tmp);
        java.io.BufferedReader r = new java.io.BufferedReader(new java.io.FileReader(TEST_BOARD));
        String line;
        while ((line = r.readLine()) != null) w.println(line);
        r.close();
        for (String l : boardLines) w.println(l);
        w.close();
        String msg = "OK";
        try {
            Board b = FileLoader.loadBoard(tmp.getPath());
            msg = "OK " + b.getPendingExpansions().size();
        } catch (IllegalArgumentException e) { msg = e.getMessage(); }
        tmp.delete();
        return msg;
    }

    static void twistT3LoaderErrors() throws Exception {
        check("T3 loader: EXPAND may name a square added by an earlier EXPAND line",
                loadError("EXPAND|P1|PROPERTY|P1b|BROWN|60|2", "EXPAND|P1b|TAX|Fee2|20").equals("OK 2"), loadError("EXPAND|P1|PROPERTY|P1b|BROWN|60|2", "EXPAND|P1b|TAX|Fee2|20"));
        check("T3 loader: unknown anchor square rejected", loadError("EXPAND|Nowhere|TAX|Fee2|20").contains("not on the board"), "");
        check("T3 loader: EXPAND cannot add a second JAIL", loadError("EXPAND|P1|JAIL|Jail2").contains("cannot add a JAIL"), "");
        check("T3 loader: duplicate new square name rejected", loadError("EXPAND|P1|TAX|Tax|20").contains("duplicate"), "");
        check("T3 loader: a normal square after an EXPAND line is rejected",
                loadError("EXPAND|P1|TAX|Fee2|20", "TAX|Fee3|10").contains("must come after"), "");
        check("T3 loader: more than 28 squares in total rejected", loadError("EXPAND|P1|TAX|E1|1", "EXPAND|P1|TAX|E2|1", "EXPAND|P1|TAX|E3|1",
                "EXPAND|P1|TAX|E4|1", "EXPAND|P1|TAX|E5|1", "EXPAND|P1|TAX|E6|1", "EXPAND|P1|TAX|E7|1", "EXPAND|P1|TAX|E8|1",
                "EXPAND|P1|TAX|E9|1").contains("exceed 28"), "");
    }

    // Design note D9: no card move ends on a CHANCE or COMMUNITY square, before and after T3 (landOn depth <= 2).
    static void twistCardMovesAvoidCardSquares() throws Exception {
        Game g = game(BOARD, cards(), NEVER, new Integer[]{}, "A", "B");
        ArrayList<Card> all = FileLoader.loadCards(CARDS, g.getBoard());
        String before = cardSquareProblem(g.getBoard(), all);
        g.applyExpansions();
        String after = cardSquareProblem(g.getBoard(), all);
        check("D9: no card move ends on a card square (24-square board)", before == null, String.valueOf(before));
        check("D9: no card move ends on a card square (27 squares after T3)", after == null, String.valueOf(after));
    }

    static String cardSquareProblem(Board b, ArrayList<Card> all) {
        Node<Square> n = b.getHead();
        for (int i = 0; i < b.size(); i++, n = n.next) {
            if (!(n.data instanceof CardSquare)) continue;
            DeckType deck = ((CardSquare) n.data).getDeck();
            for (Card c : all) {
                if (c.getDeck() != deck) continue;
                Node<Square> dest = null;
                if (c.getEffect() == CardEffect.MOVE_BY) dest = b.step(n, c.getAmount(), null);
                if (c.getEffect() == CardEffect.MOVE_TO) dest = b.findByName(b.getHead(), c.getArgument(), b.size());
                if (c.getEffect() == CardEffect.NEAREST) dest = b.findNearest(n.next, c.getArgument(), b.size());
                if (dest != null && dest.data instanceof CardSquare) return c.getId() + " from " + n.data.getName() + " -> " + dest.data.getName();
            }
        }
        return null;
    }

    // Eclipse: colour board on its own dark panel; the Java constraint check catches violations.
    static void eclipseSupport() throws Exception {
        Game g = game(BOARD, cards(), NEVER, new Integer[]{}, "Sara", "Omar");
        String coloured = new BoardView(true, false, false).build(g);
        boolean everyRowOnPanel = true;
        for (String r : coloured.split(System.lineSeparator())) if (!r.startsWith("\u001B[48;5;234;38;5;252m")) everyRowOnPanel = false;
        check("Colour board: every row starts on the dark panel (readable on Eclipse's white console)", everyRowOnPanel, "");
        String plainView = new BoardView(false, false, false).build(g);
        check("Plain board (no colour) has no escape codes", !plainView.contains("\u001B["), "");
        java.io.File tmp = java.nio.file.Files.createTempDirectory("cc").toFile();
        java.io.File src = new java.io.File(CODE);
        for (java.io.File f : src.listFiles()) {
            if (!f.getName().endsWith(".java")) continue;
            String text = new String(java.nio.file.Files.readAllBytes(f.toPath()), "UTF-8");
            if (f.getName().equals("QueueTwists.java")) text = text.replace("if (q.isEmpty()) {", "if (q.size() == 0) {");
            if (f.getName().equals("BoardView.java")) text = "import java.util.Arrays;\n" + text;
            if (f.getName().equals("Player.java")) text = text.replace("public int netWorth(", "public int netWorthOld(");
            java.nio.file.Files.write(new java.io.File(tmp, f.getName()).toPath(), text.getBytes("UTF-8"));
        }
        java.io.PrintStream out = System.out;
        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream();
        System.setOut(new java.io.PrintStream(buf, true, "UTF-8"));
        boolean ok = ConstraintCheck.run(tmp.getPath());
        System.setOut(out);
        String report = buf.toString("UTF-8");
        for (java.io.File f : tmp.listFiles()) f.delete();
        tmp.delete();
        check("ConstraintCheck reports q.size() in reverse, import java.util.Arrays and a missing required method", !ok
                && report.contains("reverse uses q.size()") && report.contains("import java.util.Arrays not allowed")
                && report.contains("required method netWorth not found"), report);
    }

    static void determinismAndFuzz() throws Exception {
        Board b0 = FileLoader.loadBoard(BOARD);
        ArrayList<Card> c0 = FileLoader.loadCards(CARDS, b0);
        ArrayList<String> run1 = fullGame(42, 3, false);
        ArrayList<String> run2 = fullGame(42, 3, false);
        check("Same seed gives an identical log", run1.equals(run2) && !run1.isEmpty(), run1.size() + " vs " + run2.size());
        int games = 0, problems = 0, lastPlayer = 0;
        String firstProblem = "";
        for (int seed = 1; seed <= 1000; seed++) {
            int n = 2 + seed % 5;
            ArrayList<String> log;
            try {
                log = fullGame(seed, n, true);
            } catch (Exception e) {
                problems++; if (firstProblem.isEmpty()) firstProblem = "seed " + seed + ": " + e; continue;
            }
            games++;
            String p = invariantProblem(log, n);
            if (p != null) { problems++; if (firstProblem.isEmpty()) firstProblem = "seed " + seed + ": " + p; }
            if (log.contains("END|LAST_PLAYER")) lastPlayer++;
        }
        check("1000 seeded games (2-6 players): no exceptions, invariants hold", problems == 0, firstProblem);
        System.out.println("      (" + games + " games, " + lastPlayer + " ended LAST_PLAYER)");
    }

    static ArrayList<String> fullGame(long seed, int players, boolean randomBuys) throws Exception {
        Board b = FileLoader.loadBoard(BOARD);
        ArrayList<Card> cs = FileLoader.loadCards(CARDS, b);
        Random rng = new Random(seed);
        Random choice = new Random(seed * 31 + 7);
        BuyDecider d = randomBuys ? (p, s) -> p.getCash() >= s.getPrice() && choice.nextInt(4) != 0 : ALWAYS;
        Game g = new Game(b, cs, rng, true, new Dice(rng), new GameLogger(null, false), d, 200);
        for (int i = 1; i <= players; i++) g.addPlayer("P" + i);
        g.play();
        return g.getLogger().getLines();
    }

    /** Checks the log: cash never negative, END once, one RANK per player, TURN numbers consecutive. */
    static String invariantProblem(ArrayList<String> log, int players) {
        int turn = 0, ends = 0, ranks = 0;
        for (String line : log) {
            String[] f = line.split("\\|", -1);
            switch (f[0]) {
                case "TURN": if (Integer.parseInt(f[1]) != ++turn) return "turn numbers skip at " + line; break;
                case "BUY": case "TAX": case "CASH": if (Integer.parseInt(f[f.length - 1]) < 0) return "negative cash: " + line; break;
                case "SALARY": if (Integer.parseInt(f[2]) < 0) return "negative cash: " + line; break;
                case "RENT": if (Integer.parseInt(f[4]) < 0 || Integer.parseInt(f[5]) < 0) return "negative cash: " + line; break;
                case "END": ends++; break;
                case "RANK": ranks++; break;
                default: break;
            }
            if (line.contains("||") || line.contains(" |") || line.contains("| ")) return "bad field separator: " + line;
        }
        if (ends != 1) return "END count " + ends;
        if (ranks != players) return "RANK count " + ranks;
        return null;
    }
}
