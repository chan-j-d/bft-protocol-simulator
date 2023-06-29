package simulation.statistics;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Records consensus related statistics in a simulation.
 */
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
    private final Map<Integer, Map<String, Double>> roundStateTimeMap;
    private final Map<String, Integer> messageCountMap;

    /**
     * @param states Various states the validator can take during a simulation.
     */
    public ConsensusStatistics(Collection<Object> states) {
        nodeCount = 1;
        consensusCount = 0;
        totalTime = 0;
        stateTimeMap = new LinkedHashMap<>();
        for (Object state : states) {
            stateTimeMap.put(state.toString(), 0.0);
        }
        roundStateTimeMap = new LinkedHashMap<>();
        messageCountMap = new LinkedHashMap<>();
    }

    private ConsensusStatistics(int nodeCount, int consensusCount, double totalTime,
            Map<String, Double> stateTimeMap, Map<Integer, Map<String, Double>> roundStateTimeMap,
            Map<String, Integer> messageCountMap) {
        this.nodeCount = nodeCount;
        this.consensusCount = consensusCount;
        this.totalTime = totalTime;
        this.stateTimeMap = stateTimeMap;
        this.roundStateTimeMap = roundStateTimeMap;
        this.messageCountMap = messageCountMap;
    }

    public void setConsensusCount(int consensusCount) {
        this.consensusCount = consensusCount;
    }

    public Collection<String> getStates() {
        return stateTimeMap.keySet();
    }

    public void addMessageCount(String state) {
        messageCountMap.compute(state, (k, v) -> v == null ? 1 : v + 1);
    }

    public void addMessageCount(Object state) {
        addMessageCount(state.toString());
    }

    public double getNormalizedMessageCountForState(String type) {
        return normalizeValue(messageCountMap.get(type).doubleValue());
    }

    public double getNormalizedMessageCountForState(Object type) {
        return getNormalizedMessageCountForState(type.toString());
    }

    /**
     * Records additional {@code time} spent in {@code state}.
     */
    public void addTime(Object state, double time) {
        addTime(state.toString(), time);
    }

    /**
     * Records additional {@code time} spent in {@code state}.
     */
    public void addTime(String state, double time) {
        stateTimeMap.compute(state, (k, v) -> (v != null) ? v + time : time);
        totalTime += time;
    }

    /**
     * Records additional {@code time} spent in {@code state} at round {@code round}.
     * The round is equal to number of consecutive failures + 1.
     */
    public void addRoundTime(int round, Object state, double time) {
        addRoundTime(round, state.toString(), time);
    }

    /**
     * Records additional {@code time} spent in {@code state} at round {@code round}.
     * The round is equal to number of consecutive failures + 1.
     */
    public void addRoundTime(int round, String state, double time) {
        if (!roundStateTimeMap.containsKey(round)) {
            Map<String, Double> roundStateMap = new LinkedHashMap<>();
            for (String tempState : getStates()) {
                roundStateMap.put(tempState, 0.0);
            }
            roundStateTimeMap.put(round, roundStateMap);
        }
        roundStateTimeMap.get(round).put(state, roundStateTimeMap.get(round).get(state) + time);
    }

    public int getNodeCount() {
        return nodeCount;
    }

    /**
     * Combines the two {@code ConsensusStatistics} into a singular run.
     */
    public ConsensusStatistics combineStatistics(ConsensusStatistics otherStatistics) {
        int totalNodeCount = getNodeCount() + otherStatistics.getNodeCount();
        int newConsensusCount = Math.max(consensusCount, otherStatistics.getConsensusCount());
        double totalTime = getTotalTime() + otherStatistics.getTotalTime();
        Map<String, Double> totalStateTimeMap = mergeTwoMaps(stateTimeMap, otherStatistics.stateTimeMap);
        Map<Integer, Map<String, Double>> totalRoundStateTimeMap = new LinkedHashMap<>();
        int highestRound = Math.max(
                roundStateTimeMap.keySet().stream().max(Integer::compare).get(),
                otherStatistics.roundStateTimeMap.keySet().stream().max(Integer::compare).get());
        for (int round = 1; round <= highestRound; round++) {
            totalRoundStateTimeMap.put(round, mergeTwoMaps(roundStateTimeMap.get(round),
                    otherStatistics.roundStateTimeMap.get(round)));
        }
        Map<String, Integer> newMessageCountMap = new LinkedHashMap<>();
        for (String state : messageCountMap.keySet()) {
            newMessageCountMap.put(state, messageCountMap.getOrDefault(state, 0) +
                    otherStatistics.messageCountMap.getOrDefault(state, 0));
        }
        return new ConsensusStatistics(totalNodeCount, newConsensusCount, totalTime,
                totalStateTimeMap, totalRoundStateTimeMap, newMessageCountMap);
    }

    /**
     * Combines {@code map1} and {@code map2}.
     * Assumes that at least one of the maps are not null.
     */
    private Map<String, Double> mergeTwoMaps(Map<String, Double> map1, Map<String, Double> map2) {
        if (map1 == null) {
            return map2;
        } else if (map2 == null) {
            return map1;
        }
        Map<String, Double> newMap = new LinkedHashMap<>();
        Set<String> combinedKeySet = new LinkedHashSet<>(map1.keySet());
        combinedKeySet.addAll(map2.keySet());
        for (String state : combinedKeySet) {
            double map1StateTime = map1.containsKey(state) ? map1.get(state) : 0;
            double map2StateTime = map2.containsKey(state) ? map2.get(state) : 0;
            newMap.put(state, map1StateTime + map2StateTime);
        }
        return newMap;
    }

    private double getTotalTime() {
        return totalTime;
    }

    private int getConsensusCount() {
        return consensusCount;
    }

    public double getAverageConsensusTime() {
        return getTotalTime() / getNodeCount() / getConsensusCount();
    }

    private double getNormalizedTimeForState(String state) {
        return normalizeValue(stateTimeMap.get(state));
    }

    private double normalizeValue(double value) {
        return value / getNodeCount() / getConsensusCount();
    }

    @Override
    public Map<String, Number> getSummaryStatistics() {
        Map<String, Number> results = new LinkedHashMap<>();
        results.put(KEY_NODE_COUNT, getNodeCount());
        results.put(KEY_CONSENSUS_COUNT, getConsensusCount());
        for (String state : getStates()) {
            results.put(String.format(KEY_STATE_AVERAGE_TIME, state), getNormalizedTimeForState(state));
        }
        results.put(KEY_AVERAGE_TIME_PER_CONSENSUS, getAverageConsensusTime());
        results.put(KEY_TOTAL_TIME, getTotalTime() / getNodeCount());
        return results;
    }

    public Map<String, Double> getNormalizedStateTimeMap() {
        Map<String, Double> normalizedMap = new LinkedHashMap<>(stateTimeMap);
        for (String state : stateTimeMap.keySet()) {
            normalizedMap.put(state, getNormalizedTimeForState(state));
        }
        return normalizedMap;
    }

    public Map<Integer, Map<String, Double>> getNormalizedRoundStateTimeMap() {
        Map<Integer, Map<String, Double>> normalizedMap = new LinkedHashMap<>();
        for (int round : roundStateTimeMap.keySet()) {
            Map<String, Double> newMap = new LinkedHashMap<>(roundStateTimeMap.get(round));
            for (String state : getStates()) {
                newMap.put(state, normalizeValue(roundStateTimeMap.get(round).get(state)));
            }
            normalizedMap.put(round, newMap);
        }
        return normalizedMap;
    }

    public Map<String, Double> getNormalizedMessageCountMap() {
        Map<String, Double> newMap = new LinkedHashMap<>();
        for (String state : messageCountMap.keySet()) {
            newMap.put(state, getNormalizedMessageCountForState(state));
        }
        return newMap;
    }
}
