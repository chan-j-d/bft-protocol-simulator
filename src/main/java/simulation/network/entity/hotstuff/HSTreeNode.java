package simulation.network.entity.hotstuff;

public class HSTreeNode {

    private final HSTreeNode parent;
    private final HSCommand command;

    public HSTreeNode(HSTreeNode parent, HSCommand command) {
        this.parent = parent;
        this.command = command;
    }

    public boolean extendsFrom(HSTreeNode node) {
        // TODO implement this
        return false;
    }
}
