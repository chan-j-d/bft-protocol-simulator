package simulation.util.rng;

public class BernoulliDistribution {

    private final double p;

    /**
     * @param p Probability of success.
     */
    public BernoulliDistribution(double p) {
        this.p = p;
    }

    public boolean generateResult() {
        return RNGUtil.UNIFORM_DISTRIBUTION.nextDouble() >= p;
    }
}
