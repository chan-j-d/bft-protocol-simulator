package simulation.statistics;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueueStatistics extends Statistics {

    public static final String KEY_NUM_MESSAGES_IN_QUEUE = "Average number of messages in queue";
    public static final String KEY_MESSAGE_ARRIVAL_RATE = "Average effective arrival rate";
    public static final String KEY_WAITING_TIME = "Average waiting time per message";
    public static final String KEY_PRODUCT_ARRIVAL_WAITING_TIME = "Product of waiting time and arrival rate";

    private long totalMessageCount;
    private int currentMessageCount;
    private double totalMessageQueueTime;

    private double totalTime;
    private double totalQueueingTime;

    public QueueStatistics() {
        totalMessageCount = 0;
        currentMessageCount = 0;
        totalMessageQueueTime = 0;
        totalTime = 0;
        totalQueueingTime = 0;
    }

    private QueueStatistics(long totalMessageCount, int currentMessageCount, double totalMessageQueueTime,
            double totalTime, double totalQueueingTime) {
        this.totalMessageCount = totalMessageCount;
        this.currentMessageCount = currentMessageCount;
        this.totalMessageQueueTime = totalMessageQueueTime;
        this.totalTime = totalTime;
        this.totalQueueingTime = totalQueueingTime;
    }
    @Override
    public Map<String, Number> getSummaryStatistics() {
        Map<String, Number> map = new LinkedHashMap<>();
        map.put(KEY_NUM_MESSAGES_IN_QUEUE, totalQueueingTime / totalTime);
        map.put(KEY_MESSAGE_ARRIVAL_RATE, totalMessageCount / totalTime);
        map.put(KEY_WAITING_TIME, totalMessageQueueTime / totalMessageCount);
        map.put(KEY_PRODUCT_ARRIVAL_WAITING_TIME, (totalMessageQueueTime / totalTime));
        return map;
    }

    public void addMessageArrived(double timeElapsed) {
        totalQueueingTime += currentMessageCount * timeElapsed;
        totalMessageCount += 1;
        currentMessageCount += 1;
        totalTime += timeElapsed;
    }

    public void updateTimeElapsed(double timeElapsed) {
        totalQueueingTime += currentMessageCount * timeElapsed;
        totalTime += timeElapsed;
    }
    public void addMessageQueueTime(double timeElapsed, double messageQueueTime) {
        totalQueueingTime += currentMessageCount * timeElapsed;
        totalMessageQueueTime += messageQueueTime;
        currentMessageCount -= 1;
    }

    public String getValues() {
        return String.format("Total message count: %d\nCurrent message count: %d\nTotal message queue time: %.3f\nTotal time: %.3f\nTotal queueing time: %.3f",
                totalMessageCount, currentMessageCount, totalMessageQueueTime, totalTime, totalQueueingTime);
    }

    public QueueStatistics addStatistics(QueueStatistics other) {
        return new QueueStatistics(
                this.totalMessageCount + other.totalMessageCount,
                this.currentMessageCount + other.currentMessageCount,
                this.totalMessageQueueTime + other.totalMessageQueueTime,
                this.totalTime + other.totalTime,
                this.totalQueueingTime + other.totalQueueingTime);
    }
}
