package simulation.util.rng;

import java.util.Random;

public class RNGUtil {

    public static long SEED = 0;
    public static Random UNIFORM_DISTRIBUTION = new Random(SEED);

    public static void setSeed(long seed) {
        SEED = seed;
        UNIFORM_DISTRIBUTION = new Random(SEED);
    }

    public static int getRandomInteger(int startInclusive, int endExclusive) {
        return UNIFORM_DISTRIBUTION.nextInt(endExclusive - startInclusive) + startInclusive;
    }
}
