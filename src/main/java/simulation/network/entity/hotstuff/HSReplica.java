package simulation.network.entity.hotstuff;

import simulation.network.entity.NodeTimerNotifier;
import simulation.network.entity.Payload;
import simulation.network.entity.Validator;
import simulation.util.rng.RandomNumberGenerator;

import java.util.List;

public class HSReplica extends Validator<HSMessage> {

    // TODO add asynchronous message holding

    private int consensusInstance;

    private final int n;
    private final int f;
    private final int id;

    private int curView;
    private HSMessageType state;

    private HSMessageHolder messageHolder;
    private HSTreeNode curProposal;

    private QuorumCertificate highQc;
    private QuorumCertificate prepareQc;
    private QuorumCertificate preCommitQc;
    private QuorumCertificate commitQc;
    private QuorumCertificate lockedQc;

    public HSReplica(int id, int n, int f, String name, NodeTimerNotifier<HSMessage> timerNotifier,
            RandomNumberGenerator serviceRateGenerator) {
        super(name, id, timerNotifier, serviceRateGenerator);
        this.consensusInstance = 0;
        this.id = id;
        this.n = n;
        this.f = f;
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
        return qc.getType().equals(type) && viewNumber == qc.getViewNumber();
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
        if (messageView < curView) {
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

    private void prepareOperation() {
        int leader = getLeader(curView);
        if (id == leader) {
            List<HSMessage> newViewMessages = messageHolder.getVoteMessages(HSMessageType.NEW_VIEW, curView - 1);
            if (newViewMessages.size() < n - f) {
                return;
            }

            highQc = getMaxViewNumberQc(newViewMessages);
            // creates a generic command as the contents are not important
            curProposal = createLeaf(highQc.getNode(), new HSCommand(curView));
            broadcastMessageToAll(msg(HSMessageType.PREPARE, curProposal, highQc));
        }
        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.PREPARE, curView)) {
                if (m.getNode().extendsFrom(m.getJustify().getNode()) &&
                        safeNode(m.getNode(), m.getJustify())) {
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
            List<HSMessage> prepareMessages = messageHolder.getVoteMessages(HSMessageType.PREPARE, curView);
            if (prepareMessages.size() < n - f) {
                return;
            }

            prepareQc = new QuorumCertificate(prepareMessages);
            broadcastMessageToAll(msg(HSMessageType.PRE_COMMIT, null, prepareQc));
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
            List<HSMessage> preCommitMessages = messageHolder.getVoteMessages(HSMessageType.PRE_COMMIT, curView);
            if (preCommitMessages.size() < n - f) {
                return;
            }

            preCommitQc = new QuorumCertificate(preCommitMessages);
            broadcastMessageToAll(msg(HSMessageType.COMMIT, null, preCommitQc));
        }
        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingMessage(m, HSMessageType.PRE_COMMIT, curView)) {
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
            List<HSMessage> commitMessages = messageHolder.getVoteMessages(HSMessageType.COMMIT, curView);
            if (commitMessages.size() < n - f) {
                return;
            }

            commitQc = new QuorumCertificate(commitMessages);
            broadcastMessageToAll(msg(HSMessageType.DECIDE, null, commitQc));
        }

        if (hasLeaderMessage()) {
            HSMessage m = getLeaderMessage();
            if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.COMMIT, curView)) {
                // consensus achieved (?)
                startNextView();
            }
        }
    }

    private void startNextView() {
        sendMessage(voteMsg(HSMessageType.NEW_VIEW, null, prepareQc), getNode(getLeader(curView + 1)));
        messageHolder.advanceView(curView, curView + 1);
        curView++;
        state = HSMessageType.PREPARE;
        prepareOperation();
    }

    @Override
    protected void registerTimeElapsed(double time) {
        // TODO Add statistics tracking for timing
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public List<Payload<HSMessage>> notifyTime(double time, HSMessage message) {
        return null;
    }
}
