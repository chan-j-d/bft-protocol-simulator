package simulation.network.entity.hotstuff;

import java.util.Objects;
import java.util.stream.Stream;

import static simulation.util.StringUtil.MESSAGE_SEPARATOR;

/**
 * Message sent between HotStuff replicas.
 */
public class HSMessage {

    private final HSMessageType type;
    private final int viewNumber;
    private final HSTreeNode node;
    private final QuorumCertificate justify;
    private final int sender;
    private final boolean isVote;

    public HSMessage(int sender, HSMessageType type, int viewNumber, HSTreeNode node, QuorumCertificate qc,
            boolean isVote) {
        this.type = type;
        this.viewNumber = viewNumber;
        this.node = node;
        this.justify = qc;
        this.sender = sender;
        this.isVote = isVote;
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

    public boolean isVote() {
        return isVote;
    }

    @Override
    public String toString() {
        return Stream.of(sender, type, viewNumber, node, justify, isVote ? "vote" : "non-vote")
                .map(Objects::toString)
                .reduce((x, y) -> x + MESSAGE_SEPARATOR + y)
                .get();
    }
}
