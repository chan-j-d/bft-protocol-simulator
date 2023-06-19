package simulation.network.entity.hotstuff;

import simulation.network.entity.timer.TimerNotifier;
import simulation.network.entity.Payload;
import simulation.network.entity.Validator;
import simulation.util.logging.Logger;
import simulation.util.rng.RandomNumberGenerator;

import java.util.Arrays;
import java.util.List;

/**
 * Replica running the HotStuff protocol.
 */
public class HSReplica extends Validator<HSMessage> {

    private final Logger logger;

    private int numConsensus;

    private final int n;
    private final int f;
    private final int id;
    private final double baseTimeLimit;
    private int numConsecutiveFailures;

    private int curView;
    private HSMessageType state;

    private HSMessageHolder messageHolder;
    private HSTreeNode curProposal;

    private QuorumCertificate highQc;
    private QuorumCertificate prepareQc;
    private QuorumCertificate preCommitQc;
    private QuorumCertificate commitQc;
    private QuorumCertificate lockedQc;

    private boolean hasReceivedLeaderMessageInDecidePhase;

    /**
     * @param name Name of HotStuff replica.
     * @param id Unique integer identifier for HotStuff replica.
     * @param baseTimeLimit Base time limit for timeouts.
     * @param timerNotifier TimerNotifier used to get time and set timeouts.
     * @param n Number of nodes in the simulation.
     * @param consensusLimit Consensus limit in simulation.
     * @param serviceRateGenerator Rate at processing messages, assuming an exponentially distributed service time.
     */
    public HSReplica(String name, int id, double baseTimeLimit, TimerNotifier<HSMessage> timerNotifier, int n,
            int consensusLimit, RandomNumberGenerator serviceRateGenerator) {
        super(name, id, consensusLimit, timerNotifier, serviceRateGenerator,
                Arrays.asList((Object[]) HSMessageType.values()));
        this.logger = new Logger(name);
        this.numConsensus = 0;
        this.id = id;
        this.n = n;
        this.f = (this.n - 1) / 3;
        this.baseTimeLimit = baseTimeLimit;
        this.curView = 1;
        this.state = HSMessageType.PREPARE;
        this.messageHolder = new HSMessageHolder();
        this.numConsecutiveFailures = 0;
        this.hasReceivedLeaderMessageInDecidePhase = false;

        this.lockedQc = null;
        this.prepareQc = null;
    }

    // Algorithm 1: Utility methods

    /**
     * Creates a leader {@code HSMessage} of {@code type}, containing {@code node} and {@code qc} as justification.
     */
    private HSMessage msg(HSMessageType type, HSTreeNode node, QuorumCertificate qc) {
        return new HSMessage(id, type, curView, node, qc, false);
    }

    /**
     * Creates a vote {@code HSMessage} of {@code type}, containing{@code node} and {@code qc} as justification.
     */
    private HSMessage voteMsg(HSMessageType type, HSTreeNode node, QuorumCertificate qc) {
        // Here, we ignore the signing component of the vote message as we are not concerned with verification.
        return new HSMessage(id, type, curView, node, qc, true);
    }

    /**
     * Creates a new {@code HSTreeNode} with {@code parent} and proposed {@code command}.
     */
    private HSTreeNode createLeaf(HSTreeNode parent, HSCommand command) {
        return new HSTreeNode(parent, command);
    }

    /**
     * Returns true if the given message {@code m} is of {@code type} and {@code viewNumber}.
     */
    private boolean matchingMessage(HSMessage m, HSMessageType type, int viewNumber) {
        return m.getType().equals(type) && viewNumber == m.getViewNumber();
    }

    /**
     * Returns true if the given {@code qc} is of {@code type} and {@code viewNumber}.
     * For ease of checking for view = 1, we set {@code qc} = null as true as well.
     */
    private boolean matchingQc(QuorumCertificate qc, HSMessageType type, int viewNumber) {
        return qc == null || (qc.getType().equals(type) && viewNumber == qc.getViewNumber());
    }

    /**
     * Returns true if the node is safe.
     * Safe is fulfilled by one of two criteria, that the {@code node} extends from the currently {@code lockedQc}
     * or that its view number is greater than that in the {@code lockedQc}.
     */
    private boolean safeNode(HSTreeNode node, QuorumCertificate qc) {
        return lockedQc == null || // handling curView = 1
                node.extendsFrom(lockedQc.getNode()) || qc.getViewNumber() > lockedQc.getViewNumber();
    }

    // Other utilities
    private int getLeader(int viewNumber) {
        return viewNumber % n;
    }

    private boolean hasLeaderMessage() {
        return messageHolder.containsLeaderMessage(state, curView);
    }

    private HSMessage getLeaderMessage() {
        return messageHolder.getLeaderMessage(state, curView);
    }

    private double timerFunction(int numConsecutiveFailures) {
        return Math.pow(2, numConsecutiveFailures) * baseTimeLimit;
    }

    private void startHsTimer() {
        startTimer(timerFunction(numConsecutiveFailures));
    }

    // Algorithm 2: Basic HotStuff protocol (following HotStuff paper)

    /**
     * Creates the initial messages sent out at the start of the protocol.
     * Leader: Sends out PREPARE messages as if it had received a quorum of NEW_VIEW messages for view 1.
     * Replica: Start timer.
     */
    @Override
    public List<Payload<HSMessage>> initializationPayloads() {
        if (id == getLeader(curView)) {
            curProposal = createLeaf(null, new HSCommand(curView));
            broadcastMessageToAll(msg(HSMessageType.PREPARE, curProposal, highQc));
        }
        startHsTimer();
        return getProcessedPayloads();
    }

    @Override
    protected List<Payload<HSMessage>> processMessage(HSMessage message) {
        int messageView = message.getViewNumber();
        HSMessageType type = message.getType();
        if (messageView < curView - 1 || (type != HSMessageType.NEW_VIEW && messageView == curView - 1)) {
            // If the view of the message is lower than expected, ignore the message.
            return List.of();
        }

//        logger.log(String.format("Time: %s, Name: %s, (PROCESSING) State: %s, Leader: %s, CurView: %s, Consensus: %s, Consecutive Failures: %s, Message: %s",
//                getTime(), getName(), state, getLeader(curView), curView, numConsensus, numConsecutiveFailures, message));
        messageHolder.addMessage(message);
        switch (state) {
            case PREPARE:
                prepareOperation();
                break;
            case PRE_COMMIT:
                preCommitOperation();
                break;
            case COMMIT:
                commitOperation();
                break;
            case DECIDE:
                decideOperation();
                break;
        }
//        return getProcessedPayloads();
        List<Payload<HSMessage>> payloads = getProcessedPayloads();
//        logger.log("Processing payloads: " + payloads.toString());
        return payloads;
    }

    /**
     * Runs basic HotStuff protocol when in the Prepare phase.
     * This block translates the code of the basic HotStuff protocol in Algorithm 2 as per the HotStuff paper.
     */
    private void prepareOperation() {
        int leader = getLeader(curView);
        if (id == leader) {
            if (messageHolder.hasQuorumOfMessages(HSMessageType.NEW_VIEW, curView - 1, n - f)) {
                List<HSMessage> newViewMessages = messageHolder.getVoteMessages(HSMessageType.NEW_VIEW, curView - 1);

                highQc = getMaxViewNumberQc(newViewMessages);
                // creates a generic command as the contents are not important
                curProposal = createLeaf(highQc != null ? highQc.getNode() : null, new HSCommand(curView));
                broadcastMessageToAll(msg(HSMessageType.PREPARE, curProposal, highQc));
                startHsTimer();
            }
        }
        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingMessage(m, HSMessageType.PREPARE, curView)) {
                if (m.getJustify() == null || (m.getNode().extendsFrom(m.getJustify().getNode()) &&
                        safeNode(m.getNode(), m.getJustify()))) {
                    sendMessage(voteMsg(HSMessageType.PREPARE, m.getNode(), null), getNode(leader));
                    startHsTimer();
                    state = HSMessageType.PRE_COMMIT;
                    preCommitOperation();
                }
            }
        }
    }

    /**
     * Given a quorum of messages, returns the max view number among the justification of each message.
     * Returns {@code null} if none of the messages have a justification (relevant only for first consensus instance).
     */
    private QuorumCertificate getMaxViewNumberQc(List<HSMessage> messages) {
        int viewNumber = -1;
        QuorumCertificate highQc = null;
        for (HSMessage message : messages) {
            QuorumCertificate messageJustify = message.getJustify();
            if ((messageJustify != null) && (viewNumber == -1 || messageJustify.getViewNumber() > viewNumber)) {
                highQc = messageJustify;
                viewNumber = messageJustify.getViewNumber();
            }
        }
        return highQc;
    }

    /**
     * Runs basic HotStuff protocol when in the Pre-Commit phase.
     * This block translates the code of the basic HotStuff protocol in Algorithm 2 as per the HotStuff paper.
     */
    private void preCommitOperation() {
        int leader = getLeader(curView);
        if (id == leader) {
            if (messageHolder.hasQuorumOfMessages(HSMessageType.PREPARE, curView, n - f)) {
                List<HSMessage> prepareMessages = messageHolder.getVoteMessages(HSMessageType.PREPARE, curView);
                prepareQc = new QuorumCertificate(prepareMessages);
                broadcastMessageToAll(msg(HSMessageType.PRE_COMMIT, null, prepareQc));
                startHsTimer();
            }
        }
        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.PREPARE, curView)) {
                prepareQc = m.getJustify();
                sendMessage(voteMsg(HSMessageType.PRE_COMMIT,
                        m.getJustify().getNode(), null), getNode(leader));
                startHsTimer();
                state = HSMessageType.COMMIT;
                commitOperation();
            }
        }
    }

    /**
     * Runs basic HotStuff protocol when in the Commit phase.
     * This block translates the code of the basic HotStuff protocol in Algorithm 2 as per the HotStuff paper.
     */
    private void commitOperation() {
        int leader = getLeader(curView);
        if (id == leader) {
            if (messageHolder.hasQuorumOfMessages(HSMessageType.PRE_COMMIT, curView, n - f)) {
                List<HSMessage> preCommitMessages = messageHolder.getVoteMessages(HSMessageType.PRE_COMMIT, curView);
                preCommitQc = new QuorumCertificate(preCommitMessages);
                broadcastMessageToAll(msg(HSMessageType.COMMIT, null, preCommitQc));
                startHsTimer();
            }
        }
        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.PRE_COMMIT, curView)) {
                lockedQc = m.getJustify();
                sendMessage(voteMsg(HSMessageType.COMMIT, m.getJustify().getNode(), null), getNode(leader));
                startHsTimer();
                state = HSMessageType.DECIDE;
                decideOperation();
            }
        }
    }

    /**
     * Runs basic HotStuff protocol when in the Decide phase.
     * This block translates the code of the basic HotStuff protocol in Algorithm 2 as per the HotStuff paper.
     */
    private void decideOperation() {
        int leader = getLeader(curView);
        if (id == leader) {
            if (messageHolder.hasQuorumOfMessages(HSMessageType.COMMIT, curView, n - f)) {
                List<HSMessage> commitMessages = messageHolder.getVoteMessages(HSMessageType.COMMIT, curView);
                commitQc = new QuorumCertificate(commitMessages);
                broadcastMessageToAll(msg(HSMessageType.DECIDE, null, commitQc));
                startHsTimer();
                return;
            }
        }

        if (hasLeaderMessage()) {
            if (!hasReceivedLeaderMessageInDecidePhase) {
                hasReceivedLeaderMessageInDecidePhase = true;
                startHsTimer();
            }
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.COMMIT, curView)) {
                numConsensus = m.getJustify().getNode().getHeight();
                commit(m.getJustify().getNode());
                startNextView();
            }
        }
    }

    /**
     * Commits the given {@code node} to the consensus chain.
     */
    private void commit(HSTreeNode node) {
        // consensus achieved
        numConsecutiveFailures = 0;
    }

    /**
     * On timeout, starts the next view.
     */
    @Override
    protected List<Payload<HSMessage>> onTimerExpiry() {
        numConsecutiveFailures++;
//        logger.log(String.format("Time: %s, Name: %s, (EXPIRY) State: %s, Leader: %s, CurView: %s, Consensus: %s, Consecutive Failures: %s",
//                getTime(), getName(), state, getLeader(curView), curView, numConsensus, numConsecutiveFailures));
        startNextView();
        List<Payload<HSMessage>> payloads = getProcessedPayloads();
//        logger.log("Expiry payloads: " + payloads.toString());
        return payloads;
//        return getProcessedPayloads();
    }

    /**
     * Starts the next view by sending out a NEW_VIEW message to the leader of the next view.
     */
    private void startNextView() {
        sendMessage(voteMsg(HSMessageType.NEW_VIEW, null, prepareQc), getNode(getLeader(curView + 1)));
        startHsTimer();
        messageHolder.advanceView(curView, curView + 1);
        curView++;
        state = HSMessageType.PREPARE;
        prepareOperation();
    }

    @Override
    public String toString() {
        return String.format("%s (%s, %d)",
                getName(),
                state,
                curView);
    }

    @Override
    public int getConsensusCount() {
        return numConsensus;
    }

    @Override
    public Object getState() {
        return state;
    }
}
