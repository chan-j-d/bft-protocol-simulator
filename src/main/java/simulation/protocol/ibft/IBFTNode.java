package simulation.protocol.ibft;

import simulation.network.entity.timer.TimerNotifier;
import simulation.protocol.ConsensusProgram;
import simulation.protocol.ConsensusProgramImpl;
import simulation.util.Pair;
import simulation.util.logging.Logger;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static simulation.protocol.ibft.IBFTMessage.NULL_VALUE;

/**
 * Validator running the IBFT protocol.
 */
public class IBFTNode extends ConsensusProgramImpl<IBFTMessage> {

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
    private final Map<Integer, List<IBFTMessage>> consensusQuorum;
    private final Map<Integer, Integer> otherNodeHeights;
    private int inputValue_i; // value passed as input to instance
    private int leader;

    private boolean hasPrePrepared;
    private boolean hasPrepared;
    private boolean hasRoundChangeLeaderPrePrepared;

    /**
     * @param name Name of IBFT validator.
     * @param id Unique integer identifier for IBFT validator.
     * @param baseTimeLimit Base time limit for timeouts.
     * @param N Number of nodes in the simulation.
     * @param idNodeNameMap Map of node ids to their names in the network.
     * @param timerNotifier Time notifier to be used for setting timers.
     */
    public IBFTNode(String name, int id, double baseTimeLimit, int N,
            Map<Integer, String> idNodeNameMap,
            TimerNotifier<ConsensusProgram<IBFTMessage>> timerNotifier) {
        super(N, timerNotifier);
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
    public List<IBFTMessage> initializationPayloads() {
        start(FIRST_CONSENSUS_INSTANCE, DUMMY_VALUE);
        return getMessages();
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
    public String getState() {
        return state.toString();
    }

    @Override
    public Collection<String> getStates() {
        return Arrays.stream(IBFTState.values()).map(IBFTState::toString).collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return String.format("IBFT (%s, %d, %d) (timeout at %.3f)",
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
     * Returns the leader for the current {@code lambda_i} and {@code r_i}.
     * A random permutation is chosen before running a round-robin algorithm.
     * A round-robin algorithm is used so the number of nodes {@code N} needs to be specified.
     */
    private int getLeader() {
        List<Integer> intList = IntStream.range(0, N).boxed().collect(Collectors.toList());
        Collections.shuffle(intList, new Random(lambda_i));
        return intList.get(r_i % N);
    }

    // Timer expire handling
    private void startIbftTimer() {
        startTimer(timerFunction(r_i));
    }

    @Override
    protected List<IBFTMessage> onTimerExpiry() {
        timeoutOperation();
        return getMessages();
    }

    // Message util
    private IBFTMessage createSingleValueMessage(int recipient, IBFTMessageType type, int value) {
        return IBFTMessage.createValueMessage(p_i, recipient, type, lambda_i, r_i, value);
    }

    private IBFTMessage createSingleValueMessage(int recipient, IBFTMessageType type, int value,
            List<IBFTMessage> piggybackMessages) {
        return IBFTMessage.createValueMessage(p_i, recipient, type, lambda_i, r_i, value, piggybackMessages);
    }

    private IBFTMessage createPreparedValuesMessage(int recipient, IBFTMessageType type) {
        return IBFTMessage.createPreparedValuesMessage(p_i, recipient, type, lambda_i, r_i, pr_i, pv_i);
    }

    private IBFTMessage createPreparedValuesMessage(int recipient, IBFTMessageType type,
            List<IBFTMessage> piggybackMessages) {
        return IBFTMessage.createPreparedValuesMessage(p_i, recipient,
                type, lambda_i, r_i, pr_i, pv_i, piggybackMessages);
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
        updateRound(1);
        pr_i = NULL_VALUE;
        pv_i = NULL_VALUE;
        preparedMessageJustification = List.of();
        inputValue_i = value;
        newRoundCleanup();
        if (leader == p_i) {
            broadcastMessage(id -> createSingleValueMessage(id, IBFTMessageType.PREPREPARED, inputValue_i));
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
    public List<IBFTMessage> processMessage(IBFTMessage message) {
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
            sendMessage(createSingleValueMessage(sender, IBFTMessageType.SYNC, NULL_VALUE,
                    consensusQuorum.get(lambda)));
        }
        return getMessages();
    }

    // Algorithm 3 in IBFT Paper - Round change handling

    /**
     * Handles timer expiry during an IBFT round.
     * This corresponds to the first code block in Algorithm 3.
     */
    private void timeoutOperation() {
        resetRoundBooleans();

        updateRound(r_i + 1);
        state = IBFTState.ROUND_CHANGE;
        startIbftTimer();
        if (pr_i == NULL_VALUE && pv_i == NULL_VALUE) {
            broadcastMessage(id -> createPreparedValuesMessage(id, IBFTMessageType.ROUND_CHANGE));
        } else {
            broadcastMessage(id -> createPreparedValuesMessage(id, IBFTMessageType.ROUND_CHANGE,
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
            updateRound(messageHolder.getNextGreaterRoundChangeMessage(lambda_i, r_i));
            startIbftTimer();
            broadcastMessage(id -> createPreparedValuesMessage(id, IBFTMessageType.ROUND_CHANGE));
            state = IBFTState.ROUND_CHANGE;
            prePrepareOperation();
            prepareOperation();
        }
    }

    private void updateRound(int newRound) {
        r_i = newRound;
        leader = getLeader();
    }

    /**
     * Handles round change upon receiving quorum of round change messages where {@code this} is the leader.
     * This corresponds to the third code block in Algorithm 3.
     */
    private void leaderRoundChangeOperation() {
        if (p_i == leader) {
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
                    broadcastMessage(id -> createSingleValueMessage(id,
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
            if (sender == leader && justifyPrePrepare(message) && !hasPrePrepared) {
                hasPrePrepared = true;

                startIbftTimer();
                state = IBFTState.PREPREPARED;
                inputValue_i = message.getValue();
                broadcastMessage(id -> createSingleValueMessage(id, IBFTMessageType.PREPARED, inputValue_i));
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
            broadcastMessage(id -> createSingleValueMessage(id, IBFTMessageType.COMMIT, inputValue_i));
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
