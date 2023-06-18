package simulation.statistics;

import java.util.Map;

public abstract class Statistics {

    public abstract Map<String, Number> getSummaryStatistics();

    @Override
    public String toString() {
        return getSummaryStatistics().entrySet().stream()
                .map(entry -> entry.getKey() + ": " + entry.getValue())
                .reduce((x, y) -> x + "\n" + y)
                .orElse("");
    }
}
