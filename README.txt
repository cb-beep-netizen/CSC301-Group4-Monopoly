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
  code\*.java             game source (25 files, incl. BoardView)
  code\data\board.txt     our 24-square board (Appendix B.1 format)
  code\data\cards.txt     our 10 Chance + 10 Community cards (Appendix B.2)
  tests\GameTests.java    53 tests incl. the brief 8.2 scenarios and the board view
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

TWISTS IMPLEMENTED
  None yet - the group's three assigned twists are being confirmed with the
  instructor. The design leaves extension points (new Square subclass, new
  CardEffect value).

BONUS FUNCTIONALITIES (planned, Section 11)
  B4 live pointer visualiser (--debug) and language toggle (--lang en|fr|ar).

GITHUB
  sync-to-github.bat pushes this folder to branch CS-Monopoly of
  https://github.com/cb-beep-netizen/CSC301-Group4-Monopoly (adds and updates
  files, never deletes teammates' files; local-only files are in .gitignore).

AI USE
  All AI use is logged in AI-Usage-log.txt (Claude, Conversation 01).
