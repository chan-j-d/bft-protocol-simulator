package simulation.statistics;

import java.util.HashMap;
import java.util.Map;

public class QueueStatistics extends Statistics {

    private static final String KEY_NUM_MESSAGES_IN_QUEUE = "Average number of messages in queue";
    private static final String KEY_MESSAGE_ARRIVAL_RATE = "Average effective arrival rate";
    private static final String KEY_WAITING_TIME = "Average waiting time per message";

    private long totalMessageCount;
    private double totalMessageQueueTime;

    private double totalTime;
    private double totalQueueingTime;

    public QueueStatistics() {
        totalMessageCount = 0;
        totalMessageQueueTime = 0;
        totalTime = 0;
        totalQueueingTime = 0;
    }

    @Override
    public Map<String, Number> getSummaryStatistics() {
        return Map.of(KEY_NUM_MESSAGES_IN_QUEUE, totalQueueingTime / totalTime,
                KEY_MESSAGE_ARRIVAL_RATE, totalMessageCount / totalTime,
                KEY_WAITING_TIME, totalMessageQueueTime / totalTime);
    }

    public void addMessageArrived(double timeElapsed) {
        totalQueueingTime += totalMessageCount * timeElapsed;
        totalMessageCount += 1;
        totalTime += timeElapsed;
    }

    public void addMessageQueueTime(double messageQueueTime) {
        totalMessageQueueTime += messageQueueTime;
    }
}
