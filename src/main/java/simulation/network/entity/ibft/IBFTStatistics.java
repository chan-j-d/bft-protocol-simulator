package simulation.network.entity.ibft;

import simulation.statistics.Statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.LinkedHashMap;

public class IBFTStatistics extends Statistics {

    private int nodeCount;
    private int consensusCount;
    private Map<IBFTState, Double> stateTimeMap;
    public IBFTStatistics() {
        nodeCount = 1;
        consensusCount = 0;
        stateTimeMap = new HashMap<>();
        for (IBFTState state : IBFTState.STATES) {
            stateTimeMap.put(state, 0.0);
        }
    }

    private IBFTStatistics(int nodeCount, int consensusCount, Map<IBFTState, Double> stateTimeMap) {
        this.nodeCount = nodeCount;
        this.consensusCount = consensusCount;
        this.stateTimeMap = stateTimeMap;
    }

    public void incrementConsensusCount() {
        this.consensusCount++;
    }

    public void addTime(IBFTState state, double time) {
        stateTimeMap.compute(state, (k, v) -> (v != null) ? v + time : time);
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
        Map<IBFTState, Double> newMap = new HashMap<>();
        for (IBFTState state : IBFTState.STATES) {
            newMap.put(state, getTimeForState(state) + otherStatistics.getTimeForState(state));
        }
        return new IBFTStatistics(totalNodeCount, newConsensusCount, newMap);
    }

    public int getConsensusCount() {
        return consensusCount;
    }

    @Override
    public Map<String, Number> getSummaryStatistics() {
        Map<String, Number> results = new LinkedHashMap<>();
        results.put("Total node count: ", getNodeCount());
        results.put("Total consensus count: ", getConsensusCount());
        double total = 0;
        for (IBFTState state : IBFTState.STATES) {
            double value = stateTimeMap.get(state) / getNodeCount() / getConsensusCount();
            total += value;
            results.put(String.format("Average time at state %s per node per instance: ", state), value);
        }
        results.put("Total time per consensus instance per node: ", total);
        return results;
    }
}
