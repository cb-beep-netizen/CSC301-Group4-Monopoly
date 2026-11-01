package monopoly;

public class Mirage extends Square {
    public Mirage(String name) {
        super(name);
    }

    @Override
    public void land(Game game, Player player) {
        // Track mirage triggers in Player or Game state if needed. 
        // Assuming player tracks mirageCount per turn:
        if (player.mirageCount >= 3) {
            return; // 4th landing has no effect[cite: 1]
        }
        player.mirageCount++;

        int stepsBack = player.lastMoveWasCard ? 3 : player.lastDiceTotal;
        GameLogger.log("MIRAGE|" + player.name + "|" + stepsBack);

        // Move backward recursively (steps < 0 invokes backward movement in board.step)
        String oldSquareName = player.position.data.getName();
        Node<Square> destination = game.board.step(player.position, -stepsBack, player);
        String newSquareName = destination.data.getName();

        GameLogger.logMove(player, oldSquareName, newSquareName);
        player.position = destination;

        // Resolve the new square
        game.landOn(player);
    }
}