package simulation.util.rng;

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
