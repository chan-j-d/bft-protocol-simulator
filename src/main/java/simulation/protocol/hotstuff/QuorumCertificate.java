package simulation.network.entity.hotstuff;

import java.util.List;

/**
 * Quorum of HotStuff messages used to justify a decision made by the leader of the current view.
 */
public class QuorumCertificate {

    private final HSMessageType type;
    private final int viewNumber;
    private final HSTreeNode node;
    private final HSSignature signature;

    public QuorumCertificate(List<HSMessage> messages) {
        HSMessage m = messages.get(0);
        this.type = m.getMessageType();
        this.viewNumber = m.getViewNumber();
        this.node = m.getNode();
        this.signature = tCombine(type, viewNumber, node, messages);
    }

    /**
     * Combines the {@code type}, {@code viewNumber}, {@code node} and {@code messages} into a unique signature.
     */
    public HSSignature tCombine(HSMessageType type, int viewNumber, HSTreeNode node, List<HSMessage> messages) {
        // Not necessary at the moment as this is for private/public key verification which is assumed to be correct.
        return new HSSignature();
    }

    public HSMessageType getType() {
        return type;
    }

    public int getViewNumber() {
        return viewNumber;
    }

    public HSTreeNode getNode() {
        return node;
    }
}
