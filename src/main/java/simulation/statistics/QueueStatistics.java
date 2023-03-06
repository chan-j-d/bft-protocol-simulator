package simulation.statistics;

import java.util.LinkedHashMap;
import java.util.Map;

public class QueueStatistics extends Statistics {

    public static final String KEY_NUM_MESSAGES_IN_QUEUE = "Average number of messages in queue";
    public static final String KEY_MESSAGE_ARRIVAL_RATE = "Average effective arrival rate";
    public static final String KEY_WAITING_TIME = "Average waiting time per message";
    public static final String KEY_PRODUCT_ARRIVAL_WAITING_TIME = "Product of waiting time and arrival rate";
    public static final String KEY_TOTAL_TIME = "Total time";

    private long totalMessageCount;
    private int currentMessageCount;
    private double totalMessageQueueTime;

    private double totalTime;
    private double totalQueueingTime;
    private int numNodes;

    public QueueStatistics() {
        totalMessageCount = 0;
        currentMessageCount = 0;
        totalMessageQueueTime = 0;
        totalTime = 0;
        totalQueueingTime = 0;
        numNodes = 1;
    }

    private QueueStatistics(long totalMessageCount, int currentMessageCount, double totalMessageQueueTime,
            double totalTime, double totalQueueingTime, int numNodes) {
        this.totalMessageCount = totalMessageCount;
        this.currentMessageCount = currentMessageCount;
        this.totalMessageQueueTime = totalMessageQueueTime;
        this.totalTime = totalTime;
        this.totalQueueingTime = totalQueueingTime;
        this.numNodes = numNodes;
    }
    @Override
    public Map<String, Number> getSummaryStatistics() {
        double averageNumMessagesInQueue = totalQueueingTime / totalTime;
        double messageArrivalRate = totalMessageCount / totalTime;
        double averageTimeInQueue = totalMessageQueueTime / totalMessageCount;
        Map<String, Number> map = new LinkedHashMap<>();
        map.put(KEY_NUM_MESSAGES_IN_QUEUE, averageNumMessagesInQueue);
        map.put(KEY_MESSAGE_ARRIVAL_RATE, messageArrivalRate);
        map.put(KEY_WAITING_TIME, averageTimeInQueue);
        map.put(KEY_PRODUCT_ARRIVAL_WAITING_TIME, messageArrivalRate * averageTimeInQueue);
        map.put(KEY_TOTAL_TIME, totalTime / numNodes);
        return map;
    }

    public void addMessageArrived(double timeElapsed) {
        totalQueueingTime += currentMessageCount * timeElapsed;
        totalMessageCount += 1;
        currentMessageCount += 1;
        totalTime += timeElapsed;
    }

    public void addMessageQueueTime(double timeElapsed, double messageQueueTime) {
        totalQueueingTime += currentMessageCount * timeElapsed;
        totalMessageQueueTime += messageQueueTime;
        totalTime += timeElapsed;
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
                this.totalQueueingTime + other.totalQueueingTime,
                this.numNodes + other.numNodes);
    }
}
