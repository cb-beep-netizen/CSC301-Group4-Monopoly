package monopoly;

import java.util.Random;

public class Dice {
    private static final Random random = new Random();

    public static int[] roll() {
        int d1 = random.nextInt(6) + 1;
        int d2 = random.nextInt(6) + 1;
        return new int[] { d1, d2 };
    }
}