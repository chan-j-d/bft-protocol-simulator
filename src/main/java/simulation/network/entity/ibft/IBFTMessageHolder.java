package simulation.network.entity.ibft;

import simulation.util.Pair;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static simulation.network.entity.ibft.IBFTMessage.NULL_VALUE;
import static simulation.network.entity.ibft.IBFTMessageType.MESSAGE_TYPES;

public class IBFTMessageHolder {

    private final Map<IBFTMessageType, Map<Integer, Map<Integer, Map<Integer, List<IBFTMessage>>>>> messageStorage;
    private Map<Integer, Integer> roundChangeMessageCounts;
    private Map<Integer, Pair<Integer, List<IBFTMessage>>> toCommitRoundValueMap;
    private int threshold;
    private int currentConsensusInstance;

    public IBFTMessageHolder(int threshold, int lambda) {
        messageStorage = new HashMap<>();
        roundChangeMessageCounts = new HashMap<>();
        toCommitRoundValueMap = new HashMap<>();
        this.threshold = threshold;
        this.currentConsensusInstance = lambda;
    }

    public void addMessage(IBFTMessage message) {
        IBFTMessageType type = message.getMessageType();
        int consensusInstance = message.getLambda();
        if (consensusInstance < currentConsensusInstance) {
            return;
        }
        int round = message.getRound();
        int identifyingValue = message.getValue() == NULL_VALUE
                ? message.getPreparedRound()
                : message.getValue();
        messageStorage.putIfAbsent(type, new HashMap<>());
        var typeMap = messageStorage.get(type);
        typeMap.putIfAbsent(consensusInstance, new HashMap<>());
        var consensusMap = typeMap.get(consensusInstance);
        consensusMap.putIfAbsent(round, new HashMap<>());
        var roundMap = consensusMap.get(round);
        roundMap.putIfAbsent(identifyingValue, new ArrayList<>());
        List<IBFTMessage> messageGroup = roundMap.get(identifyingValue);
        messageGroup.add(message);
        processMessageMetadata(message, messageGroup);
    }

    private void processMessageMetadata(IBFTMessage message, List<IBFTMessage> messageGroup) {
        IBFTMessageType type = message.getMessageType();
        if (type == IBFTMessageType.ROUND_CHANGE) {
            int consensusInstance = message.getLambda();
            roundChangeMessageCounts.compute(consensusInstance, (k, v) -> (v == null) ? 1 : v + 1);
        } else if (type == IBFTMessageType.COMMIT) {
            int lambda = message.getLambda();
            if (messageGroup.size() >= threshold && !toCommitRoundValueMap.containsKey(lambda)) {
                int value = message.getValue();
                toCommitRoundValueMap.put(lambda, new Pair<>(value, messageGroup));
            }
        }
    }

    public boolean hasMoreHigherRoundChangeMessagesThan(int consensusInstance, int round) {
        int totalCountForConsensusInstance = roundChangeMessageCounts.computeIfAbsent(consensusInstance, k -> 0);
        int currentRoundChangeCount = filterTypeLambdaRound(IBFTMessageType.ROUND_CHANGE, round, threshold)
                .stream()
                .flatMap(valueMap -> valueMap.values().stream())
                .mapToInt(List::size)
                .sum();
        return totalCountForConsensusInstance - currentRoundChangeCount >= threshold;
    }

    private Optional<Map<Integer, Map<Integer, List<IBFTMessage>>>> filterTypeLambda(IBFTMessageType type, int lambda) {
        return Optional.of(messageStorage)
                .map(store -> store.get(type))
                .map(lambdaMap -> lambdaMap.get(lambda));
    }
    private Optional<Map<Integer, List<IBFTMessage>>> filterTypeLambdaRound(IBFTMessageType type,
            int lambda, int round) {
        return filterTypeLambda(type, lambda).map(roundMap -> roundMap.get(round));
    }

    public int getNextGreaterRoundChangeMessage(int consensusInstance, int round) {
        return messageStorage.get(IBFTMessageType.ROUND_CHANGE).get(consensusInstance)
                .keySet().stream().mapToInt(x -> x).filter(roundKey -> roundKey > round).min().orElse(NULL_VALUE);
    }

    public boolean hasQuorumOfSameValuedMessages(IBFTMessageType type, int consensusInstance, int round) {
        return filterTypeLambdaRound(type, consensusInstance, round)
                .stream()
                .flatMap(map -> map.values().stream())
                .anyMatch(list -> list.size() >= threshold);
    }

    public List<IBFTMessage> getQuorumOfSameValuedMessages(IBFTMessageType type, int consensusInstance,
            int round) {
        return filterTypeLambdaRound(type, consensusInstance, round)
                .stream()
                .flatMap(map -> map.values().stream())
                .filter(list -> list.size() >= threshold)
                .findFirst()
                .orElseThrow(); // this method should be called after hasQuorumOfMessages
    }

    public boolean hasQuorumOfAnyValuedMessages(IBFTMessageType type, int consensusInstance, int round) {
        return filterTypeLambdaRound(type, consensusInstance, round)
                .stream()
                .flatMap(map -> map.values().stream())
                .mapToInt(Collection::size)
                .sum() >= threshold;
    }

    public List<IBFTMessage> getQuorumOfAnyValuedMessages(IBFTMessageType type, int consensusInstance, int round) {
        return filterTypeLambdaRound(type, consensusInstance, round)
                .stream()
                .flatMap(map -> map.values().stream())
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    public List<IBFTMessage> getMessages(IBFTMessageType type, int consensusInstance, int round) {
        return filterTypeLambdaRound(type, consensusInstance, round)
                .stream()
                .flatMap(map -> map.values().stream())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public boolean hasCommitQuorumOfMessages(int consensusInstance) {
        return toCommitRoundValueMap.containsKey(consensusInstance);
    }

    public Pair<Integer, List<IBFTMessage>> getRoundValueToCommit(int consensusInstance) {
        return toCommitRoundValueMap.get(consensusInstance);
    }

    /**
     * Removes stale messages between {@code oldLambda} (inclusive) and {@code newLambda} (exclusive).
     * Necessary for keeping memory usage in check.
     */
    public void advanceConsensusInstance(int oldLambda, int newLambda) {
        for (int i = oldLambda; i < newLambda; i++) {
            for (IBFTMessageType type : MESSAGE_TYPES) {
                messageStorage.getOrDefault(type, new HashMap<>()).remove(i);
            }
            roundChangeMessageCounts.remove(i);
            toCommitRoundValueMap.remove(i);
        }
        currentConsensusInstance = newLambda;
    }

}
