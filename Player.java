package monopoly;

import java.util.ArrayList;

public class Player {
    public String name;
    public int cash;
    public Node<Square> position;
    public ArrayList<Square> owned;
    public int jailAttempts;
    public int jailFreeCards;

    public Player(String name, Node<Square> startPosition) {
        this.name = name;
        this.cash = 1500; //[cite: 1]
        this.position = startPosition;
        this.owned = new ArrayList<>();
        this.jailAttempts = 0;
        this.jailFreeCards = 0;
    }

    // Required recursive net worth method (no loops!)
    public int netWorth(ArrayList<Square> ownedList, int index) {
        if (index >= ownedList.size()) {
            return 0;
        }
        Square sq = ownedList.get(index);
        int val = 0;
        if (sq instanceof PropertySquare) {
            val = ((PropertySquare) sq).price;
        } else if (sq instanceof TransitSquare) {
            val = ((TransitSquare) sq).price;
        }
        return val + netWorth(ownedList, index + 1);
    }
}