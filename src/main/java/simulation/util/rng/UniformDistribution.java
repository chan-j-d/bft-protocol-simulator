package simulation.util.rng;

public class UniformDistribution implements RandomNumberGenerator {

    private final double a;
    private final double b;

    public UniformDistribution(double a, double b) {
        this.a = a;
        this.b = b;
    }


    @Override
    public double generateRandomNumber() {
        return RNGUtil.UNIFORM_DISTRIBUTION.nextDouble() * (b - a) + a;
    }
}
