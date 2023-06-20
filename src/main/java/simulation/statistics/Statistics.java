package simulation.statistics;

import java.util.Map;

/**
 * Statistics abstract class.
 */
public abstract class Statistics {

    /**
     * Returns a map of various quantities measured and their relevant statistics.
     */
    public abstract Map<String, Number> getSummaryStatistics();

    @Override
    public String toString() {
        return getSummaryStatistics().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((x, y) -> x + "\n" + y)
                .orElse("");
    }
}
