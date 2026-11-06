# Capital Circuit – Monopoly in Java (CSC301 Group 4)

A console Monopoly game on our own **Abu Dhabi board**, written in Java for CSC301 Data Structures and Algorithms at Abu Dhabi University.

The board is our own **circular doubly linked list**. The turn order and the card decks are our own **linked queue**. **ArrayList** stores data that needs an index and a size that changes, such as each player's squares. Movement, doubles, landing, rent and searches use **recursion**, with no loops.

## Our three twists

| Twist | What happens in the game |
|---|---|
| **T1 Reverse** | A *REVERSE* card turns the playing order the other way round. |
| **T3 Expansion** | At the end of turn 20, three new squares are added to the board (24 → 27 squares). |
| **T7 Recall** | A *RECALL* card moves the last 3 cards of that deck to the top. |

## How to run it (Eclipse only)

**You need:** Java (JDK) **21 or newer** and **Eclipse IDE for Java Developers**.

1. **Get the project folder** onto your computer: click the green **Code** button on GitHub › **Download ZIP**, then unzip it.
2. Open Eclipse. Go to **File › Import… › General › Existing Projects into Workspace** and click **Next**.
3. Next to *Select root directory*, click **Browse** and choose the **whole project folder**: the one that contains the `code`, `tests` and `eclipse` folders. **Do not choose `code`.**
4. Make sure **Monopoly** is ticked and **"Copy projects into workspace" is NOT ticked**. Click **Finish**.
5. Open the **Run** menu › **Run Configurations…** › **Java Application**, pick one of the entries below and click **Run**.

| Run entry | What it does |
|---|---|
| **Monopoly - Play** | Asks for player names, then you play on the colour board. |
| **Monopoly - Play with seed** | Same as Play, but the same seed always gives the same game. Good for demos. |
| **Monopoly - Twist demo** | Plays a full game by itself that shows all three twists. |
| **Monopoly - Tests** | Runs 96 tests. You should see `96 passed, 0 failed` and `CONSTRAINT CHECK: PASS`. |

**Tip:** run **Monopoly - Tests** first. If it passes, everything is set up correctly.

## How to play

- **Click inside the Console** at the bottom of Eclipse before typing.
- Press **Enter** to roll the dice.
- Type **Y** or **N** when asked if you want to buy a square.
- Type **Q** to stop the game and see who is winning.
- Each turn's board is printed below the previous one, because Eclipse cannot clear its console.

The full rules (rent, cards, Jail, bankruptcy, how the winner is chosen) are in **`CSC301_Group4_Monopoly_Rulebook.pdf`**.

## If something goes wrong

| Problem | Fix |
|---|---|
| The four **Monopoly** entries are not in the Run menu | The project was not imported from the right folder. In Package Explorer, delete the project (leave **"Delete project contents on disk" unticked**), then do steps 2–4 again with the **whole** folder. |
| `Cannot find code/data/board.txt` | Use the Run entries above. They already start in the right folder. |
| You see codes like `[1;38;5;51m` instead of colours | In Eclipse, go to **Window › Preferences › Run/Debug › Console › ANSI Support** and tick **Enable**. |
| "A project with this name already exists" | Delete the old **Monopoly** project from Eclipse (leave **"Delete project contents on disk" unticked**), then import again. |

## What is in this repository

| Folder or file | Contents |
|---|---|
| `code/` | The game's Java source code. |
| `code/data/` | Our board (`board.txt`) and cards (`cards.txt`). |
| `tests/` | The automated tests and the constraint check. |
| `eclipse/` | The four Run menu entries. |
| `design/` | UML class diagram and design notes. |
| `README.txt` | The full README for the assignment: every option and every twist rule. |
| `CSC301_Group4_Monopoly_Rulebook.pdf` | Rules, data-structure drawings and demo revision. |
| `CSC301_Group4_Monopoly_Data_Structures_Report.pdf` | The design document. |
| `AI-Usage-log.txt` | Every AI conversation used for this assignment (brief Section 7). |
