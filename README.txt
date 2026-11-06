CSC301 GROUP ASSIGNMENT - MONOPOLY GAME ("CAPITAL CIRCUIT", Abu Dhabi edition)
==============================================================================
Console-based simplified Monopoly in Java, built to the rules in the CSC301
brief (Dr Zakaria Khiati, ADU, 27 Oct 2026).

REQUIREMENTS
  JDK 21 or later (developed with Eclipse Temurin JDK 25 LTS; compiled with
  --release 21). No external libraries.

COMPILE
  Windows:  build.bat
  Any OS:   javac --release 21 -encoding UTF-8 -d bin code/*.java

RUN
  Windows:  run.bat [options]
  Any OS:   java -cp bin Main code/data/board.txt code/data/cards.txt [options]

  Options
    --seed N          seed for dice and deck shuffling (same seed = same game)
    --players A,B,C   2 to 6 player names in turn order (otherwise asked)
    --auto            buy every square the player can afford (no prompts)
    --max-turns N     turn limit (default 60)
    --dice 3,4,6,6    scripted dice values used first (tests and demo)
    --log FILE        game log file (default game-log.txt)
    --pause           wait for Enter before each turn

    --text            plain mode: print the Appendix C log lines, no board
    --color / --no-color   force colours on or off (default: detected)
    --ascii           draw the board with + - | characters
    --log-lines       board view shows raw log lines instead of sentences

  Example: run.bat --players Sara,Omar,Ali --seed 42
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
  as a solid block). Colour works in the classic Windows 10/11 console, Windows
  Terminal and the VS Code terminal. If you see codes such as "[1;38;5;51m"
  instead of colours, run with --no-color.

OUTPUT
  Every event is written in the Appendix C log format (TURN|1|Sara, ROLL|...,
  MOVE|..., RENT|..., END|..., RANK|...) to the log file (default
  game-log.txt). With --text the same lines are also printed on the console.

FILES
  code\*.java             game source (27 files, incl. BoardView, QueueTwists, Expansion)
  code\data\board.txt     our 24-square board + 3 EXPAND lines (Appendix B.1 format)
  code\data\cards.txt     our 12 Chance + 12 Community cards (Appendix B.2),
                          incl. REVERSE and RECALL in each deck
  tests\GameTests.java    93 tests incl. the brief 8.2 scenarios (also T1),
                          the twists and the board view
  tests\check_constraints.py  static check: allowed imports, no banned
                          collections, required methods recursive with no loops
  run-tests.bat           builds and runs both
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
    run.bat --players Sara,Omar,Ali,Huda --seed 73 --auto --text --max-turns 24
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
