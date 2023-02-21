package simulation.network.entity.ibft;

import simulation.statistics.Statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

public class IBFTStatistics extends Statistics {

    private int nodeCount;
    private int consensusCount;
    private int numMessagesArrived;
    private double totalTime;

    private Map<IBFTState, Double> stateTimeMap;
    public IBFTStatistics() {
        nodeCount = 1;
        consensusCount = 0;
        numMessagesArrived = 0;
        totalTime = 0;
        stateTimeMap = new HashMap<>();
        for (IBFTState state : IBFTState.STATES) {
            stateTimeMap.put(state, 0.0);
        }
    }

    private IBFTStatistics(int nodeCount, int consensusCount, int numMessagesArrived, double totalTime,
            Map<IBFTState, Double> stateTimeMap) {
        this.nodeCount = nodeCount;
        this.consensusCount = consensusCount;
        this.numMessagesArrived = numMessagesArrived;
        this.totalTime = totalTime;
        this.stateTimeMap = stateTimeMap;
    }

    public void incrementConsensusCount() {
        this.consensusCount++;
    }

    public void incrementMesssageArrivedCount() {
        this.numMessagesArrived++;
    }

    public void addTime(IBFTState state, double time) {
        stateTimeMap.compute(state, (k, v) -> (v != null) ? v + time : time);
        totalTime += time;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public double getTimeForState(IBFTState state) {
        return stateTimeMap.get(state);
    }

    public IBFTStatistics addStatistics(IBFTStatistics otherStatistics) {
        int totalNodeCount = getNodeCount() + otherStatistics.getNodeCount();
        int newConsensusCount = Math.max(consensusCount, otherStatistics.getConsensusCount());
        int totalMessageCount = getNumMessagesArrived() + otherStatistics.getNumMessagesArrived();
        double totalTime = getTotalTime() + otherStatistics.getTotalTime();
        Map<IBFTState, Double> newMap = new HashMap<>();
        for (IBFTState state : IBFTState.STATES) {
            newMap.put(state, getTimeForState(state) + otherStatistics.getTimeForState(state));
        }
        return new IBFTStatistics(totalNodeCount, newConsensusCount, totalMessageCount, totalTime, newMap);
    }

    private int getNumMessagesArrived() {
        return numMessagesArrived;
    }

    private double getTotalTime() {
        return totalTime;
    }

    private int getConsensusCount() {
        return consensusCount;
    }

    @Override
    public Map<String, Number> getSummaryStatistics() {
        Map<String, Number> results = new LinkedHashMap<>();
        results.put("Total node count", getNodeCount());
        results.put("Total consensus count", getConsensusCount());
        for (IBFTState state : IBFTState.STATES) {
            double value = stateTimeMap.get(state) / getNodeCount() / getConsensusCount();
            results.put(String.format("Average time at state %s per node per instance", state), value);
        }
        results.put("Message arrival rate", getNumMessagesArrived() / getTotalTime());
        results.put("Total time per consensus instance per node",
                getTotalTime() / getNodeCount() / getConsensusCount());
        return results;
    }
}
