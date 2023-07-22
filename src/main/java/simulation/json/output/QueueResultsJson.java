package simulation.json.output;

/**
 * Encapsulates the queue statistics of an object that employs a queue.
 */
public class QueueResultsJson {

    private final double L;
    private final double lambda;
    private final double W;

    /**
     * @param L Average number of messages in the queue.
     * @param lambda Message arrival rate.
     * @param W Average waiting time for message.
     */
    public QueueResultsJson(double L, double lambda, double W) {
        this.L = L;
        this.lambda = lambda;
        this.W = W;
    }

    @Override
    public String toString() {
        return String.format("Average no. of messages in queue: %.3f\n" +
                "Message arrival rate: %.3f\n" +
                "Average waiting time: %.3f", L, lambda, W);
    }
}
