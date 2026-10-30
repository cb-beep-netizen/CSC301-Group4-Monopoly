package monopoly;

public class Portal extends Square {
    public Portal otherPortal; // Reference to the matching portal on the board

    public Portal(String name) {
        super(name);
    }

    @Override
    public void land(Game game, Player player) {
        // Count steps forward recursively
        int forwardSteps = countForwardSteps(player.position, otherPortal, 0);
        // Count steps backward recursively
        int backwardSteps = countBackwardSteps(player.position, otherPortal, 0);

        String direction;
        int chosenSteps;
        Node<Square> destination;

        if (forwardSteps <= backwardSteps) { // Tie goes forward[cite: 1]
            direction = "FORWARD";
            chosenSteps = forwardSteps;
            destination = game.board.step(player.position, chosenSteps, player);
        } else {
            direction = "BACKWARD";
            chosenSteps = backwardSteps;
            destination = game.board.step(player.position, -chosenSteps, player); // Negative for backward
        }

        GameLogger.log("PORTAL|" + player.name + "|" + direction + "|" + chosenSteps);
        GameLogger.logMove(player, player.position.data.getName(), destination.data.getName());
        
        player.position = destination;
        // Note: Destination portal is NOT resolved (does not bounce back)[cite: 1]
    }

    // Recursive helper to count steps forward
    private int countForwardSteps(Node<Square> current, PortalSquare target, int steps) {
        if (current.data == target) {
            return steps;
        }
        return countForwardSteps(current.next, target, steps + 1);
    }

    // Recursive helper to count steps backward
    private int countBackwardSteps(Node<Square> current, PortalSquare target, int steps) {
        if (current.data == target) {
            return steps;
        }
        return countBackwardSteps(current.prev, target, steps + 1);
    }
}