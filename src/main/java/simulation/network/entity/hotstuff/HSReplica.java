package simulation.network.entity.hotstuff;

import simulation.network.entity.NodeTimerNotifier;
import simulation.network.entity.Payload;
import simulation.network.entity.Validator;
import simulation.util.rng.RandomNumberGenerator;

import java.util.ArrayList;
import java.util.List;

public class HSReplica extends Validator<HSMessage> {

    private final int n;
    private final int f;
    private final int id;

    private int curView;
    private HSState state;

    private int messageCount;
    private List<HSMessage> messages;
    private HSTreeNode curProposal;

    private QuorumCertificate highQc;
    private QuorumCertificate prepareQc;
    private QuorumCertificate preCommitQc;
    private QuorumCertificate commitQc;
    private QuorumCertificate lockedQc;

    public HSReplica(int id, int n, int f, String name, NodeTimerNotifier<HSMessage> timerNotifier,
            RandomNumberGenerator serviceRateGenerator) {
        super(name, id, timerNotifier, serviceRateGenerator);
        this.id = id;
        this.n = n;
        this.f = f;
        this.curView = 0;
        this.state = HSState.PREPARE;
        this.messageCount = 0;
        messages = new ArrayList<>();
    }

    // Algorithm 1: Utility methods
    private HSMessage voteMessage(HSMessageType type, HSTreeNode node, QuorumCertificate qc) {
        return new HSMessage(id, type, curView, node, qc);
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
        return node.extendsFrom(lockedQc.getNode()) || qc.getViewNumber() > lockedQc.getViewNumber();
    }

    // Other utilities
    private int getLeader(int viewNumber) {
        return viewNumber % n;
    }

    private HSMessage createNewViewMessage(int currentView, QuorumCertificate qc) {
        return new HSMessage(id, HSMessageType.NEW_VIEW, currentView, null, qc);
    }

    // Algorithm 2: Basic HotStuff protocol (following HotStuff paper)

    @Override
    public List<Payload<HSMessage>> initializationPayloads() {
        sendMessage(createNewViewMessage(curView, null), getNode(getLeader(curView + 1)));
        curView++;
        return getProcessedPayloads();
    }



    @Override
    protected List<Payload<HSMessage>> processMessage(HSMessage message) {
        switch (state) {
            case PREPARE:
                prepareOperation(message);
                break;
            case PRECOMMIT:
                preCommitOperation(message);
                break;
            case COMMIT:
                commitOperation(message);
                break;
            case DECIDE:
                decideOperation(message);
                break;
        }
        return getProcessedPayloads();
    }

    private void prepareOperation(HSMessage m) {
        int leader = getLeader(curView);
        if (id == leader) {
            if (matchingMessage(m, HSMessageType.NEW_VIEW, curView - 1)) {
                messages.add(m);
            }
            if (messages.size() < n - f) {
                return;
            }

            highQc = getMaxViewNumberQc(messages);
            curProposal = createLeaf(highQc.getNode(), new HSCommand());
            broadcastMessageToAll(createLeaderMessage(HSMessageType.PREPARE, curProposal, highQc));
            messages = new ArrayList<>();
        }
        if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.PREPARE, curView))
            if (m.getNode().extendsFrom(m.getJustify().getNode()) &&
                    safeNode(m.getNode(), m.getJustify())) {
                sendMessage(createVoteMessage(HSMessageType.PREPARE, m.getNode()), getNode(leader));
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

    private HSMessage createVoteMessage(HSMessageType type, HSTreeNode node) {
        return new HSMessage(id, type, curView, node, null);
    }

    private void preCommitOperation(HSMessage m) {
        int leader = getLeader(curView);
        if (id == leader) {
            if (matchingMessage(m, HSMessageType.PREPARE, curView)) {
                messages.add(m);
            }
            if (messages.size() < n - f) {
                return;
            }

            prepareQc = new QuorumCertificate(messages);
            broadcastMessageToAll(createLeaderMessage(HSMessageType.PRE_COMMIT, null, prepareQc));
            messages = new ArrayList<>();
        }
        if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.PREPARE, curView)) {
                prepareQc = m.getJustify();
                sendMessage(createVoteMessage(HSMessageType.PRE_COMMIT,
                        m.getJustify().getNode()), getNode(leader));
        }
    }

    private void commitOperation(HSMessage m) {
        int leader = getLeader(curView);
        if (id == leader) {
            if (matchingMessage(m, HSMessageType.PRE_COMMIT, curView)) {
                messages.add(m);
            }
            if (messages.size() < n - f) {
                return;
            }

            preCommitQc = new QuorumCertificate(messages);
            broadcastMessageToAll(createLeaderMessage(HSMessageType.COMMIT, null, preCommitQc));
            messages = new ArrayList<>();
        }
        if (m.getSender() == leader && matchingMessage(m, HSMessageType.PRE_COMMIT, curView)) {
            lockedQc = m.getJustify();
            sendMessage(createVoteMessage(HSMessageType.COMMIT, m.getJustify().getNode()), getNode(leader));
        }
    }

    private void decideOperation(HSMessage m) {
        int leader = getLeader(curView);
        if (id == leader) {
            if (matchingMessage(m, HSMessageType.COMMIT, curView)) {
                messages.add(m);
            }
            if (messages.size() < n - f) {
                return;
            }

            commitQc = new QuorumCertificate(messages);
            broadcastMessageToAll(createLeaderMessage(HSMessageType.DECIDE, null, commitQc));
            messages = new ArrayList<>();
        }

        if (m.getSender() == leader && matchingQc(m.getJustify(), HSMessageType.COMMIT, curView)) {
            // consensus achieved (?)
        }
    }

    private HSMessage createLeaderMessage(HSMessageType type, HSTreeNode proposal, QuorumCertificate qc) {
        return new HSMessage(id, type, curView, proposal, qc);
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
