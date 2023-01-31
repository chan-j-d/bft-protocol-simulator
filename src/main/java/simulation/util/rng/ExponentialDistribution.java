package simulation.util.rng;

import java.util.Random;

public class ExponentialDistribution implements RandomNumberGenerator {

    private static final long SEED = 0;
    public static Random UNIFORM_DISTRIBUTION = new Random(SEED);
    private double lambda;

    public ExponentialDistribution(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public double generateRandomNumber() {
        double randomUniformNumber = UNIFORM_DISTRIBUTION.nextDouble();
        return Math.log(1 - randomUniformNumber) / (-lambda);
    }
}
