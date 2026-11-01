package monopoly;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class BoardLoader {

    public static Board loadBoard(String filePath) {
        Board board = new Board();
        Portal firstPortal = null; // Used to link T8 Portals if there are two

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                // Skip comments and empty lines
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }

                // Check for EXPAND lines (T3 twist, applied later if needed)
                if (line.startsWith("EXPAND")) {
                    // Handle expansion lines separately or store for turn 20
                    continue;
                }

                String[] parts = line.split("\\|");
                String type = parts[0];
                Square square = null;

                switch (type) {
                    case "GO":
                    case "JAIL":
                    case "FREE":
                    case "GO_TO_JAIL":
                        square = new CornerSquare(parts[1]);
                        break;

                    case "PROPERTY":
                        // Format: PROPERTY|name|color|price|baseRent
                        square = new PropertySquare(parts[1], parts[2], Integer.parseInt(parts[3]), Integer.parseInt(parts[4]));
                        break;

                    case "TRANSIT":
                        // Format: TRANSIT|name|price
                        square = new TransitSquare(parts[1], Integer.parseInt(parts[2]));
                        break;

                    case "TAX":
                        // Format: TAX|name|amount
                        square = new TaxSquare(parts[1], Integer.parseInt(parts[2]));
                        break;

                    case "CHANCE":
                    case "COMMUNITY":
                        // Format: CHANCE|name or COMMUNITY|name
                        square = new CardSquare(parts[1], type);
                        break;

                    case "MIRAGE":
                        square = new Mirage(parts[1]);
                        break;

                    case "PORTAL":
                        Portal portal = new Portal(parts[1]);
                        if (firstPortal == null) {
                            firstPortal = portal;
                        } else {
                            // Link the two portals together for T8
                            firstPortal.otherPortal = portal;
                            portal.otherPortal = firstPortal;
                        }
                        square = portal;
                        break;

                    default:
                        System.out.println("Unknown square type: " + type);
                        continue;
                }

                // Add to the circular doubly linked list
                board.squares.addLast(square);

                // Track GO and JAIL special nodes
                if (square.getName().equals("GO")) {
                    board.goNode = board.squares.getHead().prev; // Points to the newly added tail/GO node
                } else if (square.getName().equals("Jail")) {
                    board.jailNode = board.squares.getHead().prev;
                }
            }

            // Fallback if GO was the head node
            if (board.goNode == null && board.squares.getHead() != null) {
                Node<Square> curr = board.squares.getHead();
                // Traverse or set head if GO is at the very beginning
                if (curr.data.getName().equals("GO")) {
                    board.goNode = curr;
                }
            }

        } catch (IOException e) {
            System.err.println("Error reading board file: " + e.getMessage());
        }

        return board;
    }
}