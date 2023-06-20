package simulation.util.rng;

/**
 * Class representing a random variable following a degenerate random distribution.
 */
public class DegenerateDistribution implements RandomNumberGenerator {

    private double duration;

    public DegenerateDistribution(double duration) {
        this.duration = duration;
    }
    @Override
    public double generateRandomNumber() {
        return duration;
    }
}
