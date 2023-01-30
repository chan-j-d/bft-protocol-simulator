package simulation.network.entity.ibft;

import simulation.network.entity.NetworkNode;
import simulation.network.entity.Payload;
import simulation.util.timer.TimeoutDoubler;
import simulation.util.timer.TimeoutHandler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

public class IBFTNode extends NetworkNode {

    /*
     * TODO Add round change mechanism
     *  Add registering of messages in the wrong phase i.e. receiving committed while in prepared
     */
    private static final int PREPREPARE_TYPE = 0;
    private static final int PREPARE_TYPE = 1;
    private static final int COMMITTED_TYPE = 2;
    private static final int ROUND_CHANGE_TYPE = 3;
    private static final Map<IBFTState, Integer> STATE_TO_TYPE_MAP = Map.of(
            IBFTState.NEW_ROUND, PREPREPARE_TYPE,
            IBFTState.PRE_PREPARED, PREPARE_TYPE,
            IBFTState.PREPARED, COMMITTED_TYPE,
            IBFTState.ROUND_CHANGE, ROUND_CHANGE_TYPE);
    private static final String SEPARATOR = ":";

    private final int identifier;
    private int F;
    private List<IBFTNode> otherNodes;

    // TODO Change to map to have more efficient pulling of exact message types to process
    private Map<Integer, Map<Integer, List<String>>> backlog;

    private final TimeoutHandler timer;
    private boolean isNotifiedOfTimer;

    private IBFTState state;
    private int prepareCount;
    private int committedCount;
    private int currentRound; // same as leader for this
    private int sequence;
    private int height;
    private Map<Integer, Integer> roundChangeCountMap;
    private int nextRound;
    private double currentTime;

    public IBFTNode(String name, int identifier, double timeLimit) {
        super(name);
        this.identifier = identifier;
        this.otherNodes = new ArrayList<>();
        this.timer = new TimeoutDoubler(0, timeLimit);
        this.isNotifiedOfTimer = false;
        this.state = IBFTState.NEW_ROUND;
        this.backlog = new HashMap<>();
        this.roundChangeCountMap = new HashMap<>();

        this.prepareCount = 0;
        this.committedCount = 0;
        this.currentRound = 0;
        this.sequence = 0;
        this.height = -1;
        this.nextRound = this.currentRound + 1;
        this.currentTime = 0;
    }

    public void setOtherNodes(List<IBFTNode> otherNodes) {
        this.otherNodes = new ArrayList<>(otherNodes);
        this.F = otherNodes.size() / 3;
    }

    @Override
    public List<Payload> processPayload(double time, Payload payload) {
        String message = payload.getMessage();
        currentTime = time;
        if (time > 200) {
            return List.of();
        }
        return processMessage(message);
    }

    @Override
    public List<Payload> initializationPayloads() {
        if (identifier == getLeaderFromRoundNumber(currentRound)) {
            List<NetworkNode> allNodes = new ArrayList<>(otherNodes);
            allNodes.add(this);
            return broadcastMessage(createPrePrepareMessage(identifier), allNodes);
        } else {
            return analyzeMessagesInNewRoundState(getBacklogMessagesOf(PREPREPARE_TYPE));
        }
    }

    @Override
    public double getNextNotificationTime() {
        if (isNotifiedOfTimer) {
            return -1;
        } else {
            isNotifiedOfTimer = true;
            return timer.getTimeoutTime();
        }
    }

    @Override
    public List<Payload> notifyTime(double time) {
        if (state != IBFTState.ROUND_CHANGE && timer.hasTimedOut(time) && height == -1) {
            currentTime = time;
            // note the height == 0 condition is to restrict it from going for more rounds
            nextRound = getRoundChangeRoundFromCurrentState();
            state = IBFTState.ROUND_CHANGE;

            List<Payload> payloads = broadcastMessage(createRoundChangeMessage(), otherNodes);
            payloads.addAll(processPotentialRoundChange(getRoundChangeRoundFromCurrentState()));
            return payloads;
        } else {
            return List.of();
        }
    }

    // backlog handling
    private void addToBacklog(String message) {
        int type = getMessageType(message);
        int round = getRoundOfMessage(message);
        backlog.putIfAbsent(round, new HashMap<>());
        backlog.get(round).putIfAbsent(type, new ArrayList<>());
        backlog.get(round).get(type).add(message);
    }

    private List<String> getBacklogMessagesOf(int type) {
        return Optional.ofNullable(backlog.get(currentRound))
                .map(map -> map.remove(type))
                .orElse(List.of());
    }

    // util
    private int getMessageTypeForState(IBFTState state) {
        return STATE_TO_TYPE_MAP.get(state);
    }

    // Default format is {identifier}:{round}:{message type}:{message details}
    private String createSeparatedMessage(List<Object> objects) {
        List<Object> defaultStrings = new ArrayList<>(List.of(identifier, currentRound));
        defaultStrings.addAll(objects);
        return defaultStrings.stream().map(Object::toString).collect(Collectors.joining(":"));
    }

    private int getMessageType(String message) {
        return Integer.parseInt(message.split(SEPARATOR)[2]);
    }

    private int getSender(String message) {
        return Integer.parseInt(message.split(SEPARATOR)[0]);
    }

    private int getRoundOfMessage(String message) {
        return Integer.parseInt(message.split(SEPARATOR)[1]);
    }

    private String getMessageDetails(String message) {
        return message.split(SEPARATOR, 4)[3];
    }

    private int getNextRoundNumber(int roundNumber) {
        return roundNumber + 1;
    }

    private int getLeaderFromRoundNumber(int roundNumber) {
        return roundNumber % (otherNodes.size() + 1);
    }

    // message analysis methods
    private List<Payload> processMessage(String originalMessage) {
        int status = getMessageType(originalMessage);
        if (status == ROUND_CHANGE_TYPE) {
            processRoundChangeMessage(originalMessage);
        }
        if (status != getMessageTypeForState(state)) {
            addToBacklog(originalMessage);
            return List.of();
        }

        return analyzeMessages(List.of(originalMessage));
    }

    private List<Payload> analyzeMessages(List<String> originalMessages) {
        List<Payload> finalizedPayloads = new ArrayList<>();
        List<String> messages = new ArrayList<>(originalMessages);
        while (!messages.isEmpty()) {
            if (state == IBFTState.NEW_ROUND) {
                finalizedPayloads.addAll(analyzeMessagesInNewRoundState(messages));
            } else if (state == IBFTState.PRE_PREPARED) {
                finalizedPayloads.addAll(analyzeMessagesInPrePreparedState(messages));
            } else if (state == IBFTState.PREPARED) {
                finalizedPayloads.addAll(analyzeMessagesInPreparedState(messages));
            } else if (state == IBFTState.ROUND_CHANGE) {
                finalizedPayloads.addAll(analyzeMessagesInRoundChangeState(messages));
            } else {
                throw new RuntimeException("Unknown state"); //TODO update exception type if necessary
            }
            messages = getBacklogMessagesOf(getMessageTypeForState(state));
        }
        return finalizedPayloads;
    }

    private String createPrePrepareMessage(int sequence) {
        return createSeparatedMessage(List.of(PREPREPARE_TYPE, sequence));
    }

    private List<Payload> analyzeMessagesInNewRoundState(List<String> messages) {
        for (String message : messages) {
            int sender = getSender(message);
            sequence = Integer.parseInt(getMessageDetails(message));
            if (sender == getLeaderFromRoundNumber(currentRound) && sequence > height) {
                state = IBFTState.PRE_PREPARED;
                return broadcastMessage(createPrepareMessage(sequence), otherNodes);
                // assumption that only one such message exists in the queue at most
            }
        }
        return List.of();
    }

    private String createPrepareMessage(int sequence) {
        return createSeparatedMessage(List.of(PREPARE_TYPE, sequence));
    }

    private List<Payload> analyzeMessagesInPrePreparedState(List<String> messages) {
        for (String message : messages) {
            String[] details = getMessageDetails(message).split(SEPARATOR);
            int sequence = Integer.parseInt(details[0]);
            int round = getRoundOfMessage(message);
            if (round == currentRound && sequence == this.sequence) {
                prepareCount++;

            } else if (round > currentRound) {
                addToBacklog(message);
            }
        }
        if (prepareCount >= 2 * F) {
            state = IBFTState.PREPARED;
            return broadcastMessage(createCommittedMessage(sequence), otherNodes);
        }
        return List.of();
    }

    private String createCommittedMessage(int sequence) {
        return createSeparatedMessage(List.of(COMMITTED_TYPE, sequence));
    }

    private void updateVariablesToNewRound() {
        isNotifiedOfTimer = false;
        timer.setStartTime(currentTime);
        state = IBFTState.NEW_ROUND;
        committedCount = 0;
        prepareCount = 0;
    }
    private List<Payload> analyzeMessagesInPreparedState(List<String> messages) {
        for (String message : messages) {
            String[] details = getMessageDetails(message).split(SEPARATOR);
            int sequence = Integer.parseInt(details[0]);
            int round = getRoundOfMessage(message);
            if (round == currentRound && sequence == this.sequence) {
                committedCount++;
            } else if (round > currentRound) {
                addToBacklog(message);
            }
        }
        if (committedCount >= 2 * F) {
            height = sequence;
            currentRound = getNextRoundNumber(currentRound);
            updateVariablesToNewRound();
            System.out.println(currentTime + ": Consensus achieved at " + this);
            // consensus achieved
        }
        return List.of();
    }

    // round change handling
    private void addRoundChangeCount(int round) {
        roundChangeCountMap.put(round, roundChangeCountMap.getOrDefault(round, 0) + 1);
    }

    private int getRoundChangeCount(int round) {
        return roundChangeCountMap.getOrDefault(round, 0);
    }

    private int getRoundChangeRoundFromMessage(String message) {
        return Integer.parseInt(getMessageDetails(message));
    }

    private void processRoundChangeMessage(String message) {
        addRoundChangeCount(getRoundChangeRoundFromMessage(message));
    }

    private int getRoundChangeRoundFromCurrentState() {
        int currentMax = -1;
        for (int round : roundChangeCountMap.keySet()) {
            if (round > currentRound && roundChangeCountMap.get(round) > F + 1 && round > currentMax) {
                currentMax = round;
            }
        }
        return currentMax == -1 ? currentRound + 1 : currentMax;
    }
    private String createRoundChangeMessage() {
        return createSeparatedMessage(List.of(ROUND_CHANGE_TYPE, nextRound));
    }

    private List<Payload> analyzeMessagesInRoundChangeState(List<String> messages) {
        String message = messages.get(0); // Should only have 1
        int suggestedRound = getRoundChangeRoundFromMessage(message);
        int roundCount = getRoundChangeCount(suggestedRound);
        if (suggestedRound > nextRound && roundCount >= F + 1) {
            nextRound = suggestedRound;
            return broadcastMessage(createRoundChangeMessage(), otherNodes);
        }
        return processPotentialRoundChange(nextRound);
    }

    private List<Payload> processPotentialRoundChange(int nextRound) {
        int roundCount = getRoundChangeCount(nextRound);
        if (roundCount >= 2 * F) {
            System.out.println(currentTime + ": " + this + " changed round from " + currentRound + " to " + nextRound);
            currentRound = nextRound;
            updateVariablesToNewRound();
            List<Payload> finalPayloads = new ArrayList<>(initializationPayloads());
            finalPayloads.addAll(analyzeMessages(getBacklogMessagesOf(PREPREPARE_TYPE)));
            return finalPayloads;
        } else {
            return List.of();
        }
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %d, %d, %d, %d, RC(%d, %d))",
                super.toString(),
                state,
                sequence,
                currentRound,
                prepareCount,
                committedCount,
                nextRound,
                getRoundChangeCount(nextRound));
    }
}
