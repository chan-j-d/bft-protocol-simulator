package simulation.network.entity.hotstuff;

public class HSMessage {

    private final HSMessageType type;
    private final int viewNumber;
    private final HSTreeNode node;
    private final QuorumCertificate qc;

    public HSMessage(HSMessageType type, int viewNumber, HSTreeNode node, QuorumCertificate qc) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.node = node;
        this.qc = qc;
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

    public QuorumCertificate getQc() {
        return qc;
    }
}
