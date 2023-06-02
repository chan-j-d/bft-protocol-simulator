package simulation.network.entity.hotstuff;

public class HSMessage {

    private final HSMessageType type;
    private final int viewNumber;
    private final HSTreeNode node;
    private final QuorumCertificate justify;
    private final int sender;

    public HSMessage(int sender, HSMessageType type, int viewNumber, HSTreeNode node, QuorumCertificate qc) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.node = node;
        this.justify = qc;
        this.sender = sender;
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

    public QuorumCertificate getJustify() {
        return justify;
    }

    public int getSender() {
        return sender;
    }
}
