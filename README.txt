CSC301 GROUP ASSIGNMENT - MONOPOLY GAME ("CAPITAL CIRCUIT", Abu Dhabi edition)
==============================================================================
Console-based simplified Monopoly in Java, built to the rules in the CSC301
brief (Dr Zakaria Khiati, ADU, 27 Oct 2026).

REQUIREMENTS (the project is built and run in Eclipse only)
  - JDK 21 or later (developed with Eclipse Temurin JDK 25 LTS). No external
    libraries.
  - Eclipse IDE for Java Developers 2023-12 or later (Java 21 support). With
    JDK 25, Eclipse 2025-09 needs "Java 25 Support" from the Eclipse
    Marketplace; later releases include it.
  - Any AI assistant inside Eclipse switched off (brief 7.3).

OPEN THE PROJECT IN ECLIPSE (once per laptop)
  1. Window > Preferences > Java > Installed JREs: add the JDK (21 or later).
  2. File > Import > General > Existing Projects into Workspace > Next.
  3. Select root directory: this Monopoly folder. Tick "Monopoly". Leave
     "Copy projects into workspace" UNTICKED. Finish.
  Eclipse reads .project, .classpath and .settings\ from this folder: source
  folders code and tests, compiler compliance 21 with --release, UTF-8, output
  to bin\. It builds automatically (Project > Build Automatically).
  Only one project called "Monopoly" can be open in a workspace.

RUN (Run menu > Run History / Favorites, or the green Run button's list)
  The four entries come from eclipse\*.launch and appear after the import:
    Monopoly - Play            asks for player names, then plays on the
                               colour board (Enter = roll, Y/N = buy, Q = quit)
    Monopoly - Play with seed  as Play, also asks for a seed (same seed =
                               same dice and card order, for demos)
    Monopoly - Twist demo      seed 73, plain log lines: T7 RECALL at turn 3,
                               T1 REVERSE at turn 8, T3 EXPAND after turn 20
    Monopoly - Tests           96 tests, then the constraint check (PASS/FAIL)
  Click inside the Console view before typing. To change the options of an
  entry: Run > Run Configurations > (entry) > Arguments.
  Run As > Java Application on Main.java also works: with no file arguments
  the game uses code/data/board.txt and code/data/cards.txt.

  ECLIPSE CONSOLE
  The console shows colours (Window > Preferences > Run/Debug > Console >
  ANSI Support, on by default) but cannot clear the screen, so with
  --no-clear each turn's board is printed below the previous one after a
  "=====" line. The colour board is drawn on its own dark panel, so it is
  readable on the default white console. Board width: 99 columns (113 after
  the T3 expansion); widen or maximise the Console view.

  Options (Program arguments)
    [board file] [card file]  default code/data/board.txt code/data/cards.txt
    --seed N          seed for dice and deck shuffling (same seed = same game)
    --players A,B,C   2 to 6 player names in turn order (otherwise asked)
    --auto            buy every square the player can afford (no prompts)
    --max-turns N     turn limit (default 60)
    --dice 3,4,6,6    scripted dice values used first (tests and demo)
    --log FILE        game log file (default game-log.txt)
    --pause           wait for Enter before each turn

    --text            plain mode: print the Appendix C log lines, no board
    --color / --no-color   force colours on or off (default: detected)
    --no-clear        print a divider instead of clearing (Eclipse console;
                      automatic when there is no real console)
    --ascii           draw the board with + - | characters
    --log-lines       board view shows raw log lines instead of sentences

  Without --auto the game asks "Buy it ...? [Y/N]" whenever a player lands on
  an unowned square they can afford.

SCREEN (default board view)
  The board is drawn as a ring of squares (colour group, price, current rent,
  owner [S], player tokens; # = in Jail). The middle shows each player's cash,
  position, squares owned and net worth, the turn queue, and what happened in
  the last turn. Under each player the centre lists the properties they own.
  Press Enter to roll; Q quits.

END OF GAME / QUIT
  When the game ends (one player left or the turn limit) or you press Q, the
  centre shows the ranking, and a full standings report is printed under the
  board: rank, net worth (= cash + purchase prices of owned squares) and every
  property each player owns, in board order. Quitting with Q does not write an
  END line to the log (END is only LAST_PLAYER or MAX_TURNS, Appendix C).

COLOURS
  Each player has a colour: 1 cyan, 2 pink, 3 yellow, 4 white, 5 orange,
  6 sky blue. A square a player owns is tinted with their colour and shows
  their name; their token is drawn in their colour (the player about to move
  as a solid block). Colour works in the Eclipse console (ANSI Support on).
  If you see codes such as "[1;38;5;51m" instead of colours, switch ANSI
  Support on, or remove --color and add --no-color in the run configuration.

OUTPUT
  Every event is written in the Appendix C log format (TURN|1|Sara, ROLL|...,
  MOVE|..., RENT|..., END|..., RANK|...) to the log file (default
  game-log.txt). With --text the same lines are also printed on the console.

FILES
  code\*.java             game source (27 files, incl. BoardView, QueueTwists, Expansion)
  code\data\board.txt     our 24-square board + 3 EXPAND lines (Appendix B.1 format)
  code\data\cards.txt     our 12 Chance + 12 Community cards (Appendix B.2),
                          incl. REVERSE and RECALL in each deck
  tests\GameTests.java    96 tests incl. the brief 8.2 scenarios (also T1),
                          the twists and the board view; then runs
  tests\ConstraintCheck.java  static check: allowed imports, no banned
                          collections, required methods recursive with no loops
  tests\check_constraints.py  the same check in Python (optional)
  .project, .classpath, .settings\   Eclipse project (import, see above)
  eclipse\*.launch        the four Run menu entries
  design\                 UML class diagram and design notes

DATA STRUCTURES (brief Section 4)
  Board      own CircularDoublyLinkedList<Square>; positions are Node<Square>
  Turn order own LinkedQueue<Player>
  Decks      own LinkedQueue<Card> (Chance, Community)
  ArrayList  owned squares, players for ranking, card list before shuffling
  Recursion  step, takeTurn, landOn, transitRent, netWorth, ownsGroup,
             findByName, findNearest - no for/while/do loops
             Twists: QueueTwists.reverse (T1), QueueTwists.rotate (T7),
             Game.applyExpansions (T3)

TWISTS IMPLEMENTED (brief Appendix A) - Group 4: T1, T3, T7
  T1 Reverse    Card effect REVERSE (cards C11, K11). The waiting players in
                the turn queue are reversed by the recursive
                QueueTwists.reverse(LinkedQueue<T>), which uses only enqueue,
                dequeue and isEmpty. The current player is not in the queue
                during the turn and joins the back as usual afterwards.
                Log: REVERSE|<player> then ORDER|<p1>|<p2>|... (front first).
                Example: A draws REVERSE, queue B,C,D -> D,C,B; next four
                turns are D, C, B, A.
  T3 Expansion  The 3 EXPAND lines at the end of board.txt are applied at the
                end of turn 20, in file order: Al Mina after Al Bateen (BLUE
                becomes a 3-square group), Abu Dhabi Airport Express after
                Metro Green Line (4th TRANSIT), Lulu Island after Hudayriyat
                Island (new last square: its next is GO). Each insertion is
                CircularDoublyLinkedList.insertAfter (O(1)); players do not
                move. The board grows from 24 to 27 squares and the board view
                redraws as an 8 x 8 ring (widen the window to 115 columns).
                Log: EXPAND|<new square>|<after square>.
  T7 Recall     Card effect RECALL (cards C12, K12). After RECALL is drawn,
                the last 3 cards of the same deck move to the front in their
                order, by the recursive QueueTwists.rotate(q, n - 3); RECALL
                then goes to the back. With 3 or fewer other cards in the deck
                nothing changes and no RECALL line is printed.
                Log: RECALL|<deck>|<id1>|<id2>|<id3> (front first).
  Demo of all three twists in one game (same log every time):
    Run menu > Monopoly - Twist demo
    (--players Sara,Omar,Ali,Huda --seed 73 --auto --text --max-turns 24)
    turn 3  Ali draws C12 RECALL   -> RECALL|CHANCE|C3|C1|C2
    turn 8  Huda draws K11 REVERSE -> ORDER|Ali|Omar|Sara (turns 9-12: Ali,
            Omar, Sara, Huda)
    turn 20 end -> EXPAND x3, board grows to 27 squares
  Full rules, pointer diagrams and design decisions: the design document
  (CSC301_Group4_Monopoly_Data_Structures_Report.pdf, Section 3) and
  design\Core-Design-Notes.txt (D22-D27).

BONUS FUNCTIONALITIES (planned, Section 11)
  B4 live pointer visualiser (--debug) and language toggle (--lang en|fr|ar).

GITHUB
  sync-to-github.bat pushes this folder to branch CS-Monopoly of
  https://github.com/cb-beep-netizen/CSC301-Group4-Monopoly (adds and updates
  files, never deletes teammates' files; local-only files are in .gitignore).

AI USE
  All AI use is logged in AI-Usage-log.txt (Claude, Conversations 01-03).
