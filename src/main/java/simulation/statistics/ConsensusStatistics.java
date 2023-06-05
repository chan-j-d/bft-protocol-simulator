package simulation.statistics;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class ConsensusStatistics extends Statistics {

    private static final String KEY_NODE_COUNT = "Total node count";
    private static final String KEY_CONSENSUS_COUNT = "Total consensus count";
    private static final String KEY_STATE_AVERAGE_TIME = "Average time at state %s per node per instance";
    private static final String KEY_AVERAGE_TIME_PER_CONSENSUS = "Average time per consensus instance per node";
    private static final String KEY_TOTAL_TIME = "Total time";
    private int consensusCount;
    private double totalTime;

    private final int nodeCount;
    private final Map<String, Double> stateTimeMap;

    public ConsensusStatistics(Collection<Object> states) {
        nodeCount = 1;
        consensusCount = 0;
        totalTime = 0;
        stateTimeMap = new LinkedHashMap<>();
        for (Object state : states) {
            stateTimeMap.put(state.toString(), 0.0);
        }
    }

    private ConsensusStatistics(int nodeCount, int consensusCount, double totalTime,
            Map<String, Double> stateTimeMap) {
        this.nodeCount = nodeCount;
        this.consensusCount = consensusCount;
        this.totalTime = totalTime;
        this.stateTimeMap = stateTimeMap;
    }

    public void incrementConsensusCount() {
        this.consensusCount++;
    }

    public void setConsensusCount(int consensusCount) {
        this.consensusCount = consensusCount;
    }

    public Collection<String> getStates() {
        return stateTimeMap.keySet();
    }

    public void addTime(Object state, double time) {
        addTime(state.toString(), time);
    }

    public void addTime(String state, double time) {
        stateTimeMap.compute(state, (k, v) -> (v != null) ? v + time : time);
        totalTime += time;
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public ConsensusStatistics combineStatistics(ConsensusStatistics otherStatistics) {
        int totalNodeCount = getNodeCount() + otherStatistics.getNodeCount();
        int newConsensusCount = Math.max(consensusCount, otherStatistics.getConsensusCount());
        double totalTime = getTotalTime() + otherStatistics.getTotalTime();
        Map<String, Double> newMap = new LinkedHashMap<>();
        for (String state : stateTimeMap.keySet()) {
            newMap.put(state, getTimeForState(state) + otherStatistics.getTimeForState(state));
        }
        return new ConsensusStatistics(totalNodeCount, newConsensusCount, totalTime, newMap);
    }

    private double getTimeForState(String state) {
        return stateTimeMap.get(state);
    }

    private double getTotalTime() {
        return totalTime;
    }

    private int getConsensusCount() {
        return consensusCount;
    }

    public double getTimeInState(String state) {
        return getNormalizedTimeForState(state);
    }

    public double getAverageConsensusTime() {
        return getTotalTime() / getNodeCount() / getConsensusCount();
    }

    private double getNormalizedTimeForState(String state) {
        return stateTimeMap.get(state) / getNodeCount() / getConsensusCount();
    }

    @Override
    public Map<String, Number> getSummaryStatistics() {
        Map<String, Number> results = new LinkedHashMap<>();
        results.put(KEY_NODE_COUNT, getNodeCount());
        results.put(KEY_CONSENSUS_COUNT, getConsensusCount());
        for (String state : stateTimeMap.keySet()) {
            results.put(String.format(KEY_STATE_AVERAGE_TIME, state), getNormalizedTimeForState(state));
        }
        results.put(KEY_AVERAGE_TIME_PER_CONSENSUS, getAverageConsensusTime());
        results.put(KEY_TOTAL_TIME, getTotalTime() / getNodeCount());
        return results;
    }
}
