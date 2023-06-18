package simulation.network.entity.hotstuff;

import simulation.network.entity.timer.TimerNotifier;
import simulation.network.entity.Payload;
import simulation.network.entity.Validator;
import simulation.util.logging.Logger;
import simulation.util.rng.RandomNumberGenerator;

import java.util.Arrays;
import java.util.List;

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
    private HSMessage msg(HSMessageType type, HSTreeNode node, QuorumCertificate qc) {
        return new HSMessage(id, type, curView, node, qc, false);
    }
    private HSMessage voteMsg(HSMessageType type, HSTreeNode node, QuorumCertificate qc) {
        // Here, we ignore the signing component of the vote message as we are not concerned with verification.
        return new HSMessage(id, type, curView, node, qc, true);
    }

    private HSTreeNode createLeaf(HSTreeNode parent, HSCommand command) {
        return new HSTreeNode(parent, command);
    }

    private boolean matchingMessage(HSMessage m, HSMessageType type, int viewNumber) {
        return m.getType().equals(type) && viewNumber == m.getViewNumber();
    }

    private boolean matchingQc(QuorumCertificate qc, HSMessageType type, int viewNumber) {
        return qc == null || (qc.getType().equals(type) && viewNumber == qc.getViewNumber());
    }

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
            // ignore message
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
                commit(numConsensus);
                startNextView();
            }
        }
    }

    private void commit(int numConsensus) {
        // consensus achieved
        numConsecutiveFailures = 0;
    }

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
