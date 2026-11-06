import java.util.ArrayList;
import java.util.Random;

/**
 * Dice - rolls two six-sided dice from a seeded Random. Optionally takes a
 * scripted list of values (used for tests and demo scenarios); once the script
 * runs out it falls back to the Random.
 * AI log: Conversation 01, Prompt 01.13 (Claude). Reviewed and understood by: ______
 */
public class Dice {
    private final Random rng;
    private final ArrayList<Integer> scripted;
    private int scriptIndex;
    private int d1;
    private int d2;

    public Dice(Random rng) {
        this(rng, null);
    }

    public Dice(Random rng, ArrayList<Integer> scripted) {
        this.rng = rng;
        this.scripted = scripted;
    }

    /** Rolls both dice and returns the total. */
    public int roll() {
        d1 = nextValue();
        d2 = nextValue();
        return d1 + d2;
    }

    private int nextValue() {
        if (scripted != null && scriptIndex < scripted.size()) {
            return scripted.get(scriptIndex++);
        }
        return rng.nextInt(6) + 1;
    }

    public int getD1() { return d1; }
    public int getD2() { return d2; }
    public boolean isDouble() { return d1 == d2; }
}
