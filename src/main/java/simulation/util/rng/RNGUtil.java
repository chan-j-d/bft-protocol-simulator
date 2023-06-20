package simulation.util.rng;

import java.util.Random;

/**
 * Utility methods to perform random number generation.
 */
public class RNGUtil {

    public static long SEED = 0;
    public static Random UNIFORM_DISTRIBUTION = new Random(SEED);

    /**
     * Sets the {@code seed} for the current run for random number generation.
     */
    public static void setSeed(long seed) {
        SEED = seed;
        UNIFORM_DISTRIBUTION = new Random(SEED);
    }

    /**
     * Returns a random integer between {@code startInclusive} and {@endExclusive}.
     */
    public static int getRandomInteger(int startInclusive, int endExclusive) {
        return UNIFORM_DISTRIBUTION.nextInt(endExclusive - startInclusive) + startInclusive;
    }
}
