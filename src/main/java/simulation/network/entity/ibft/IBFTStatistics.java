package simulation.network.entity.ibft;

import simulation.statistics.Statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

public class IBFTStatistics extends Statistics {

    private static final String KEY_NODE_COUNT = "Total node count";
    private static final String KEY_CONSENSUS_COUNT = "Total consensus count";
    private static final String KEY_STATE_AVERAGE_TIME = "Average time at state %s per node per instance";
    private static final String KEY_AVERAGE_TIME_PER_CONSENSUS = "Average time per consensus instance per node";
    private int nodeCount;
    private int consensusCount;
    private double totalTime;

    private Map<IBFTState, Double> stateTimeMap;
    public IBFTStatistics() {
        nodeCount = 1;
        consensusCount = 0;
        totalTime = 0;
        stateTimeMap = new HashMap<>();
        for (IBFTState state : IBFTState.STATES) {
            stateTimeMap.put(state, 0.0);
        }
    }

    private IBFTStatistics(int nodeCount, int consensusCount, double totalTime, Map<IBFTState, Double> stateTimeMap) {
        this.nodeCount = nodeCount;
        this.consensusCount = consensusCount;
        this.totalTime = totalTime;
        this.stateTimeMap = stateTimeMap;
    }

    public void incrementConsensusCount() {
        this.consensusCount++;
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
        double totalTime = getTotalTime() + otherStatistics.getTotalTime();
        Map<IBFTState, Double> newMap = new HashMap<>();
        for (IBFTState state : IBFTState.STATES) {
            newMap.put(state, getTimeForState(state) + otherStatistics.getTimeForState(state));
        }
        return new IBFTStatistics(totalNodeCount, newConsensusCount, totalTime, newMap);
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
        results.put(KEY_NODE_COUNT, getNodeCount());
        results.put(KEY_CONSENSUS_COUNT, getConsensusCount());
        for (IBFTState state : IBFTState.STATES) {
            double value = stateTimeMap.get(state) / getNodeCount() / getConsensusCount();
            results.put(String.format(KEY_STATE_AVERAGE_TIME, state), value);
        }
        results.put(KEY_AVERAGE_TIME_PER_CONSENSUS, getTotalTime() / getNodeCount() / getConsensusCount());
        return results;
    }
}
