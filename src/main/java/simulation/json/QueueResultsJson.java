package simulation.json;

public class QueueResultsJson {

    private final double L;
    private final double lambda;
    private final double W;

    public QueueResultsJson(double L, double lambda, double W) {
        this.L = L;
        this.lambda = lambda;
        this.W = W;
    }

    @Override
    public String toString() {
        return String.format("Average no. of customers in queue: %.3f\n" +
                "Message arrival rate: %.3f\n" +
                "Average waiting time: %.3f", L, lambda, W);
    }
}
