package simulation.network.entity.hotstuff;

import simulation.network.entity.NodeTimerNotifier;
import simulation.network.entity.Payload;
import simulation.network.entity.TimedNode;

import java.util.List;

public class HSReplica extends TimedNode<HSMessage> {

    private int viewNumber;
    private QuorumCertificate lockedQc;

    public HSReplica(String name, NodeTimerNotifier<HSMessage> timerNotifier) {
        super(name, timerNotifier);
    }

    // Utility methods
    private HSMessage voteMessage(HSMessageType type, HSTreeNode node, QuorumCertificate qc) {
        return new HSMessage(type, viewNumber, node, qc);
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

    @Override
    public List<Payload<HSMessage>> initializationPayloads() {
        return null;
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
