package monopoly;

public abstract class Square {
	protected String name;
    public Square(String name) { this.name = name; }
    public String getName() { return name; }
    public abstract void land(Game game, Player player);
}
