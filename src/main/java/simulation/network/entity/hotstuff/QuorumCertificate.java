package simulation.network.entity.hotstuff;

import java.util.List;

public class QuorumCertificate {

    private final HSMessageType type;
    private final int viewNumber;
    private final HSTreeNode node;
    private final HSSignature signature;

    public QuorumCertificate(List<HSMessage> messages) {
        HSMessage m = messages.get(0);
        this.type = m.getType();
        this.viewNumber = m.getViewNumber();
        this.node = m.getNode();
        this.signature = tCombine(type, viewNumber, node, messages);
    }

    public HSSignature tCombine(HSMessageType type, int viewNumber, HSTreeNode node, List<HSMessage> messages) {
        // TODO Update signature implementation
        // not really necessary at the moment as this is for private/public key verification
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
