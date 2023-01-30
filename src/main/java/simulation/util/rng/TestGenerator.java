package simulation.util.rng;

public class TestGenerator implements RandomNumberGenerator {

    private double duration;

    public TestGenerator(double duration) {
        this.duration = duration;
    }
    @Override
    public double generateRandomNumber() {
        return duration;
    }
}
