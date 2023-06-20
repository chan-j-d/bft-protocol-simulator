package simulation.util.rng;

/**
 * Class representing a random variable following an exponential distribution.
 */
public class ExponentialDistribution implements RandomNumberGenerator {

    private double lambda;

    public ExponentialDistribution(double lambda) {
        this.lambda = lambda;
    }

    @Override
    public double generateRandomNumber() {
        double randomUniformNumber = RNGUtil.UNIFORM_DISTRIBUTION.nextDouble();
        return Math.log(1 - randomUniformNumber) / (-lambda);
    }
}
