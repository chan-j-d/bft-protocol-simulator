package simulation.network.entity.hotstuff;

import simulation.network.entity.timer.TimerNotifier;
import simulation.network.entity.Payload;
import simulation.network.entity.Validator;
import simulation.util.rng.RandomNumberGenerator;

import java.util.Arrays;
import java.util.List;

public class HSReplica extends Validator<HSMessage> {

    // TODO consensus count (use node height?)
    // TODO implement time limit (but not needed for testing)

    private int numConsensus;
    private final int consensusLimit;

    private final int n;
    private final int f;
    private final int id;
    private final double baseTimeLimit;

    private int curView;
    private HSMessageType state;

    private HSMessageHolder messageHolder;
    private HSTreeNode curProposal;

    private QuorumCertificate highQc;
    private QuorumCertificate prepareQc;
    private QuorumCertificate preCommitQc;
    private QuorumCertificate commitQc;
    private QuorumCertificate lockedQc;

    public HSReplica(String name, int id, double baseTimeLimit, TimerNotifier<HSMessage> timerNotifier, int n,
            int consensusLimit, RandomNumberGenerator serviceRateGenerator) {
        super(name, id, timerNotifier, serviceRateGenerator, Arrays.asList((Object[]) HSMessageType.values()));
        this.numConsensus = 0;
        this.id = id;
        this.n = n;
        this.f = (this.n - 1) / 3;
        this.consensusLimit = consensusLimit;
        this.baseTimeLimit = baseTimeLimit;
        this.curView = 1;
        this.state = HSMessageType.PREPARE;
        this.messageHolder = new HSMessageHolder();

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

    // Algorithm 2: Basic HotStuff protocol (following HotStuff paper)
    @Override
    public List<Payload<HSMessage>> initializationPayloads() {
        if (id == getLeader(curView)) {
            curProposal = createLeaf(null, new HSCommand(curView));
            broadcastMessageToAll(msg(HSMessageType.PREPARE, curProposal, highQc));
        }
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
        return getProcessedPayloads();
    }

    @Override
    protected List<Payload<HSMessage>> onTimerExpiry() {
        return null;
    }

    private void prepareOperation() {
        int leader = getLeader(curView);
        if (id == leader) {
            if (messageHolder.hasQuorumOfMessages(HSMessageType.NEW_VIEW, curView - 1, n - f)) {
                List<HSMessage> newViewMessages = messageHolder.getVoteMessages(HSMessageType.NEW_VIEW, curView - 1);

                highQc = getMaxViewNumberQc(newViewMessages);
                // creates a generic command as the contents are not important
                curProposal = createLeaf(highQc.getNode(), new HSCommand(curView));
                broadcastMessageToAll(msg(HSMessageType.PREPARE, curProposal, highQc));
            }
        }
        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingMessage(m, HSMessageType.PREPARE, curView)) {
                if (m.getJustify() == null || (m.getNode().extendsFrom(m.getJustify().getNode()) &&
                        safeNode(m.getNode(), m.getJustify()))) {
                    sendMessage(voteMsg(HSMessageType.PREPARE, m.getNode(), null), getNode(leader));
                    state = HSMessageType.PRE_COMMIT;
                    preCommitOperation();
                }
            }
        }
    }

    private QuorumCertificate getMaxViewNumberQc(List<HSMessage> messages) {
        int viewNumber = messages.get(0).getViewNumber();
        HSMessage maxViewNumberMessage = messages.get(0);
        for (HSMessage message : messages) {
            if (viewNumber == -1 || message.getViewNumber() > viewNumber) {
                maxViewNumberMessage = message;
                viewNumber = message.getViewNumber();
            }
        }
        return maxViewNumberMessage.getJustify();
    }

    private void preCommitOperation() {
        int leader = getLeader(curView);
        if (id == leader) {
            if (messageHolder.hasQuorumOfMessages(HSMessageType.PREPARE, curView, n - f)) {
                List<HSMessage> prepareMessages = messageHolder.getVoteMessages(HSMessageType.PREPARE, curView);
                prepareQc = new QuorumCertificate(prepareMessages);
                broadcastMessageToAll(msg(HSMessageType.PRE_COMMIT, null, prepareQc));
            }
        }
        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.PREPARE, curView)) {
                prepareQc = m.getJustify();
                sendMessage(voteMsg(HSMessageType.PRE_COMMIT,
                        m.getJustify().getNode(), null), getNode(leader));
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
            }
        }
        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.PRE_COMMIT, curView)) {
                lockedQc = m.getJustify();
                sendMessage(voteMsg(HSMessageType.COMMIT, m.getJustify().getNode(), null), getNode(leader));
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
                return;
            }
        }

        if (hasLeaderMessage()) {
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
    }

    private void startNextView() {
        sendMessage(voteMsg(HSMessageType.NEW_VIEW, null, prepareQc), getNode(getLeader(curView + 1)));
        messageHolder.advanceView(curView, curView + 1);
        curView++;
        state = HSMessageType.PREPARE;
        prepareOperation();
    }

    @Override
    public boolean isDone() {
        return numConsensus > consensusLimit;
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
