package monopoly;

import java.util.ArrayList;

public class Player {
    public String name;
    public int cash;
    public Node<Square> position;
    public ArrayList<Square> owned; // <-- This is the list of owned squares
    public int jailAttempts;
    public int jailFreeCards;
    
    // Twist tracking variables
    public int mirageCount;
    public boolean lastMoveWasCard;
    public int lastDiceTotal;

    public Player(String name, Node<Square> startPosition) {
        this.name = name;
        this.cash = 1500;
        this.position = startPosition;
        this.owned = new ArrayList<>(); // <-- Initialized here
        this.jailAttempts = 0;
        this.jailFreeCards = 0;
        this.mirageCount = 0;
        this.lastMoveWasCard = false;
        this.lastDiceTotal = 0;
    }

    // Mandatory recursive net worth calculation (zero loops!)
    public int netWorth(ArrayList<Square> ownedList, int index) {
        if (index >= ownedList.size()) return 0;
        Square sq = ownedList.get(index);
        int val = 0;
        if (sq instanceof PropertySquare) {
            val = ((PropertySquare) sq).price;
        } else if (sq instanceof TransitSquare) {
            val = ((TransitSquare) sq).price;
        }
        return val + netWorth(ownedList, index + 1);
    }
    
  //-------------------------------------------------------------------------------------
  //--------------------------------------BONUS------------------------------------------  
    public boolean emergencyAssetAuction(PropertySquare prop, LinkedQueue<Player> activeQueue, int debtAmount) {
        System.out.println("--- EMERGENCY ASSET AUCTION ---");
        System.out.println(this.name + " is offering " + prop.getName() + " to active players for $" + debtAmount);
        
        int size = activeQueue.size();
        for (int i = 0; i < size; i++) {
            Player buyer = activeQueue.dequeue();
            activeQueue.enqueue(buyer); // Keep queue order unchanged
            
            if (!buyer.equals(this) && buyer.cash >= debtAmount) {
                // Transfer cash and ownership
                buyer.cash -= debtAmount;
                this.cash += debtAmount;
                this.owned.remove(prop);
                buyer.owned.add(prop);
                prop.setOwner(buyer);
                
                System.out.println("AUCTION SUCCESS: " + buyer.name + " purchased " + prop.getName() + "!");
                return true;
            }
        }
        System.out.println("AUCTION FAILED: No player could afford the asset.");
        return false;
    }
    //-------------------------------------BONUS-----------------------------------------
}