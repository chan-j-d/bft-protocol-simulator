package simulation.network.entity.ibft;

import simulation.network.entity.timer.TimerNotifier;
import simulation.network.entity.Payload;
import simulation.network.entity.Validator;
import simulation.util.Pair;
import simulation.util.logging.Logger;
import simulation.util.rng.RandomNumberGenerator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static simulation.network.entity.ibft.IBFTMessage.NULL_VALUE;

/**
 * Validator running the IBFT protocol.
 */
public class IBFTNode extends Validator<IBFTMessage> {

    /**
     * Dummy value to be proposed and passed around as part of the protocol.
     */
    private static final int DUMMY_VALUE = 1;
    public static final int FIRST_CONSENSUS_INSTANCE = 1;

    private final Logger logger;

    // Simulation variables
    private int N;
    private int F;

    // Helper attributes
    private final IBFTMessageHolder messageHolder;
    private final double baseTimeLimit;

    private IBFTState state;

    private final int p_i; // identifier
    private int lambda_i; // consensus instance identifier
    private int r_i; // round number
    private int pr_i; // round at which process has prepared
    private int pv_i; // value for which process has prepared
    private List<IBFTMessage> preparedMessageJustification;
    private Map<Integer, List<IBFTMessage>> consensusQuorum;
    private Map<Integer, Integer> otherNodeHeights;
    private int inputValue_i; // value passed as input to instance

    private boolean hasPrePrepared;
    private boolean hasPrepared;
    private boolean hasRoundChangeLeaderPrePrepared;

    /**
     * @param name Name of IBFT validator.
     * @param id Unique integer identifier for IBFT validator.
     * @param baseTimeLimit Base time limit for timeouts.
     * @param timerNotifier TimerNotifier used to get time and set timeouts.
     * @param N Number of nodes in the simulation.
     * @param consensusLimit Consensus limit in simulation.
     * @param serviceRateGenerator Rate at processing messages, assuming an exponentially distributed service time.
     */
    public IBFTNode(String name, int id, double baseTimeLimit, TimerNotifier<IBFTMessage> timerNotifier,
            int N, int consensusLimit, RandomNumberGenerator serviceRateGenerator) {
        super(name, id, consensusLimit, timerNotifier, serviceRateGenerator,
                Arrays.asList((Object[]) IBFTState.values()));
        logger = new Logger(name);
        this.state = IBFTState.NEW_ROUND;
        this.baseTimeLimit = baseTimeLimit;

        this.p_i = id;
        this.N = N;
        this.F = (this.N - 1) / 3;

        this.messageHolder = new IBFTMessageHolder(getQuorumCount(), FIRST_CONSENSUS_INSTANCE);
        this.consensusQuorum = new HashMap<>();
        this.otherNodeHeights = new HashMap<>();
    }

    /**
     * Starts up the protocol and sends out initial messages.
     */
    @Override
    public List<Payload<IBFTMessage>> initializationPayloads() {
        start(FIRST_CONSENSUS_INSTANCE, DUMMY_VALUE);
        return getProcessedPayloads();
    }

    @Override
    public int getConsensusCount() {
        return lambda_i - 1;
    }

    @Override
    public int getNumConsecutiveFailure() {
        return r_i - 1;
    }

    @Override
    public Object getState() {
        return state;
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %d, %d) (timeout at %.3f)",
                super.toString(),
                state,
                lambda_i,
                r_i,
                getTimeoutTime());
    }

    // Utility methods
    private int getQuorumCount() {
        return Math.floorDiv((N + F), 2) + 1;
    }

    private double timerFunction(int round) {
        return Math.pow(2, round - 1) * baseTimeLimit;
    }

    /**
     * Returns the leader for the current {@code consensusInstance} and {@code roundNumber}.
     * A round-robin algorithm is used so the number of nodes {@code N} needs to be specified.
     */
    private static int getLeader(int consensusInstance, int roundNumber, int N) {
        return (consensusInstance + roundNumber) % N;
    }

    // Timer expire handling
    private void startIbftTimer() {
        startTimer(timerFunction(r_i));
    }

    @Override
    protected List<Payload<IBFTMessage>> onTimerExpiry() {
        timeoutOperation();
        return getProcessedPayloads();
    }

    // Message util
    private IBFTMessage createSingleValueMessage(IBFTMessageType type, int value) {
        return IBFTMessage.createValueMessage(p_i, type, lambda_i, r_i, value);
    }

    private IBFTMessage createSingleValueMessage(IBFTMessageType type, int value, List<IBFTMessage> piggybackMessages) {
        return IBFTMessage.createValueMessage(p_i, type, lambda_i, r_i, value, piggybackMessages);
    }

    private IBFTMessage createPreparedValuesMessage(IBFTMessageType type) {
        return IBFTMessage.createPreparedValuesMessage(p_i, type, lambda_i, r_i, pr_i, pv_i);
    }

    private IBFTMessage createPreparedValuesMessage(IBFTMessageType type, List<IBFTMessage> piggybackMessages) {
        return IBFTMessage.createPreparedValuesMessage(p_i, type, lambda_i, r_i, pr_i, pv_i, piggybackMessages);
    }

    // Round start handling

    /**
     * Starts a new consensus instance {@code lambda} with the proposed value {@code value}.
     * The leader broadcasts a PREPREPARED message while all other validators will set up without sending a message.
     * Part of Algorithm 1 in IBFT paper.
     */
    private void start(int lambda, int value) {
        resetRoundBooleans();
        state = IBFTState.NEW_ROUND;

        lambda_i = lambda;
        r_i = 1;
        pr_i = NULL_VALUE;
        pv_i = NULL_VALUE;
        preparedMessageJustification = List.of();
        inputValue_i = value;
        newRoundCleanup();
        if (getLeader(lambda_i, r_i, N) == p_i) {
            broadcastMessageToAll(createSingleValueMessage(IBFTMessageType.PREPREPARED, inputValue_i));
        }
        startIbftTimer();
        runBacklogProcessingOperation();
    }

    /**
     * Runs all the 'upon' operations in the IBFT protocol on the start of a new consensus instance.
     */
    private void runBacklogProcessingOperation() {
        prePrepareOperation();
        prepareOperation();
        commitOperation();
        fPlusOneRoundChangeOperation();
        leaderRoundChangeOperation();
    }

    /**
     * Removes consensus message quorums from outdated consensus instances.
     * Meta-method required for maintaining memory usage during simulations.
     */
    private void newRoundCleanup() {
        int minBlockHeight = otherNodeHeights.values().stream().mapToInt(x -> x).min().orElse(0);
        Set<Integer> toRemoveKeySet = List.copyOf(consensusQuorum.keySet()).stream().filter(x -> x < minBlockHeight)
                .collect(Collectors.toSet());
        for (int oldConsensusInstance : toRemoveKeySet) {
            consensusQuorum.remove(oldConsensusInstance);
        }
    }

    // Message parsing methods
    @Override
    protected List<Payload<IBFTMessage>> processMessage(IBFTMessage message) {
        IBFTMessageType messageType = message.getMessageType();
        int sender = message.getIdentifier();
        int lambda = message.getLambda();
        otherNodeHeights.compute(sender, (k, v) -> (v == null) ? lambda : Math.max(v, lambda));

        if (lambda >= lambda_i) {
            messageHolder.addMessage(message);
            switch (messageType) {
                case PREPREPARED:
                    prePrepareOperation();
                    break;
                case PREPARED:
                    prepareOperation();
                    break;
                case COMMIT:
                    commitOperation();
                    break;
                case SYNC:
                    message.getPiggybackMessages().forEach(messageHolder::addMessage);
                    commitOperation();
                    break;
                case ROUND_CHANGE:
                    int messageRound = message.getRound();
                    if (messageRound > r_i) {
                        fPlusOneRoundChangeOperation();
                    } else if (messageRound == r_i) {
                        leaderRoundChangeOperation();
                    }
                    break;
            }
        } else if (messageType == IBFTMessageType.ROUND_CHANGE) {
            sendMessage(createSingleValueMessage(IBFTMessageType.SYNC, NULL_VALUE,
                    consensusQuorum.get(lambda)), getNode(sender));
        }
        return getProcessedPayloads();
    }

    // Algorithm 3 in IBFT Paper - Round change handling

    /**
     * Handles timer expiry during an IBFT round.
     * This corresponds to the first code block in Algorithm 3.
     */
    private void timeoutOperation() {
        resetRoundBooleans();

        r_i++;
        state = IBFTState.ROUND_CHANGE;
        startIbftTimer();
        if (pr_i == NULL_VALUE && pv_i == NULL_VALUE) {
            broadcastMessageToAll(createPreparedValuesMessage(IBFTMessageType.ROUND_CHANGE));
        } else {
            broadcastMessageToAll(createPreparedValuesMessage(IBFTMessageType.ROUND_CHANGE,
                    preparedMessageJustification));
        }
        prePrepareOperation();
        prepareOperation();
    }

    /**
     * Handles round increment after receiving f + 1 or more round change messages of round greater than current round.
     * This corresponds to the second code block in Algorithm 3.
     */
    private void fPlusOneRoundChangeOperation() {
        if (messageHolder.hasMoreHigherRoundChangeMessagesThan(lambda_i, r_i)) {
            resetRoundBooleans();
            r_i = messageHolder.getNextGreaterRoundChangeMessage(lambda_i, r_i);
            startIbftTimer();
            broadcastMessageToAll(createPreparedValuesMessage(IBFTMessageType.ROUND_CHANGE));
            state = IBFTState.ROUND_CHANGE;
            prePrepareOperation();
            prepareOperation();
        }
    }

    /**
     * Handles round change upon receiving quorum of round change messages where {@code this} is the leader.
     * This corresponds to the third code block in Algorithm 3.
     */
    private void leaderRoundChangeOperation() {
        if (p_i == getLeader(lambda_i, r_i, N)) {
            if (messageHolder.hasQuorumOfAnyValuedMessages(IBFTMessageType.ROUND_CHANGE, lambda_i, r_i)) {
                List<IBFTMessage> roundChangeMessages =
                        messageHolder.getQuorumOfAnyValuedMessages(IBFTMessageType.ROUND_CHANGE,
                                lambda_i, r_i);
                if (justifyRoundChange(roundChangeMessages) && !hasRoundChangeLeaderPrePrepared) {
                    hasRoundChangeLeaderPrePrepared = true;
                    Pair<Integer, Integer> prPvPair = highestPrepared(roundChangeMessages);
                    int pr = prPvPair.first();
                    int pv = prPvPair.second();
                    if (pr != NULL_VALUE && pv != NULL_VALUE) {
                        inputValue_i = pv;
                    }
                    broadcastMessageToAll(createSingleValueMessage(
                            IBFTMessageType.PREPREPARED, inputValue_i, roundChangeMessages));
                }
            }
        }
    }

    // Algorithm 2 in IBFT Paper - Normal case operation

    /**
     * Handles received PRE-PREPARE messages.
     * This corresponds to the first code block in Algorithm 2.
     */
    private void prePrepareOperation() {
        List<IBFTMessage> preprepareMessages = messageHolder.getMessages(IBFTMessageType.PREPREPARED, lambda_i, r_i);
        for (IBFTMessage message : preprepareMessages) {
            int sender = message.getIdentifier();
            if (sender == getLeader(lambda_i, r_i, N) && justifyPrePrepare(message) && !hasPrePrepared) {
                hasPrePrepared = true;

                startIbftTimer();
                state = IBFTState.PREPREPARED;
                inputValue_i = message.getValue();
                broadcastMessageToAll(createSingleValueMessage(IBFTMessageType.PREPARED, inputValue_i));
            }
        }
    }

    /**
     * Handles received PREPARE messages.
     * This corresponds to the second code block in Algorithm 2.
     */
    private void prepareOperation() {
        if (messageHolder.hasQuorumOfSameValuedMessages(IBFTMessageType.PREPARED, lambda_i, r_i)
                && !hasPrepared) {
            hasPrepared = true;

            List<IBFTMessage> prepareMessages =
                    messageHolder.getQuorumOfSameValuedMessages(IBFTMessageType.PREPARED, lambda_i, r_i);
            state = IBFTState.PREPARED;
            pr_i = r_i;
            pv_i = prepareMessages.get(0).getValue();
            preparedMessageJustification = prepareMessages;
            broadcastMessageToAll(createSingleValueMessage(IBFTMessageType.COMMIT, inputValue_i));
        }
    }

    /**
     * Handles received COMMIT messages.
     * This corresponds to the third code block in Algorithm 2.
     */
    private void commitOperation() {
        if (messageHolder.hasCommitQuorumOfMessages(lambda_i)) {
            Pair<Integer, List<IBFTMessage>> valueMessagesPair = messageHolder.getRoundValueToCommit(lambda_i);
            commit(lambda_i, valueMessagesPair.first(), valueMessagesPair.second());
            messageHolder.advanceConsensusInstance(lambda_i, lambda_i + 1);
            lambda_i++;
            start(lambda_i, DUMMY_VALUE);
        }
    }

    /**
     * Commits {@code value} for {@code consensusInstance} with the quorum of {@code messages} for justification.
     */
    private void commit(int consensusInstance, int value, List<IBFTMessage> messages) {
        // Actual value being committed is not important.
        consensusQuorum.put(consensusInstance, messages);
    }

    // Message justification

    /**
     * Returns true if the quorum of round change messages is justified.
     *
     * @param roundChangeMessageQuorum quorum of valid round change messages
     * @return true if the round change quorum is justified.
     */
    private boolean justifyRoundChange(List<IBFTMessage> roundChangeMessageQuorum) {
        Pair<Integer, Integer> quorumAnalysis = highestPrepared(roundChangeMessageQuorum);
        int highestPr = quorumAnalysis.first();
        int highestPv = quorumAnalysis.second();
        return j1Justification(roundChangeMessageQuorum) ||
                j2Justification(roundChangeMessageQuorum, lambda_i, highestPr, highestPv);
    }

    /**
     * Returns true if messages in the list have prepared round and prepared value equal to {@code NULL_VALUE}.
     * J1 justification name comes from the proof under 4.4 of the IBFT paper.
     */
    private boolean j1Justification(List<IBFTMessage> messages) {
        return messages.stream().filter(message ->
                message.getPreparedRound() == NULL_VALUE && message.getPreparedValue() == NULL_VALUE).count() >=
                getQuorumCount();
    }

    /**
     * Returns true if the round change messages have a quorum of valid PREPARE messages to justify the prepared
     * round and value.
     * J2 justification name comes from the proof under 4.4 of the IBFT paper.
     */
    private boolean j2Justification(List<IBFTMessage> messages, int lambda_i, int highestPr, int highestPv) {
        return messages.size() >= getQuorumCount() &&
                messages.stream().anyMatch(message -> message.getPiggybackMessages().stream().filter(pbm ->
                                pbm.getMessageType() == IBFTMessageType.PREPARED &&
                                pbm.getLambda() == lambda_i &&
                                pbm.getRound() == highestPr &&
                                pbm.getValue() == highestPv)
                        .count() >= getQuorumCount());
    }

    /**
     * Returns true if the received pre-prepare message is justified.
     *
     * @param message of type pre-prepare to be analyzed.
     * @return true if the received pre-prepare message is justified.
     */
    private boolean justifyPrePrepare(IBFTMessage message) {
        int messageRound = message.getRound();
        if (messageRound == 1) {
            return true;
        }

        List<IBFTMessage> piggybackMessages = message.getPiggybackMessages();
        List<IBFTMessage> validPiggybackMessages = piggybackMessages.stream()
                .filter(m -> m.getMessageType() == IBFTMessageType.ROUND_CHANGE &&
                        m.getRound() == messageRound &&
                        m.getLambda() == message.getLambda())
                .collect(Collectors.toList());
        return validPiggybackMessages.size() >= getQuorumCount() && justifyRoundChange(validPiggybackMessages);
    }

    /**
     * Returns number of double null messages and highest prepared round with its associated prepared value.
     *
     * @param messageQuorum to be analyzed.
     * @return Pair with first being the number of double null messages, and second being the pair of prepared numbers.
     */
    private Pair<Integer, Integer> highestPrepared(List<? extends IBFTMessage> messageQuorum) {
        int pv = NULL_VALUE;
        int pr = NULL_VALUE;
        for (IBFTMessage message : messageQuorum) {
            int message_pv = message.getPreparedValue();
            int message_pr = message.getPreparedRound();
            if (pr == NULL_VALUE || message_pr > pr) {
                pr = message_pr;
                pv = message_pv;
            }  // else don't update
        }
        return new Pair<>(pr, pv);
    }

    /**
     * Resets boolean tracking each upon operation.
     * The commit and f+1 round change operations can only occur once per round and consensus instance since
     * they change one of those values.
     */
    private void resetRoundBooleans() {
        hasPrePrepared = false;
        hasPrepared = false;
        hasRoundChangeLeaderPrePrepared = false;
    }
}
