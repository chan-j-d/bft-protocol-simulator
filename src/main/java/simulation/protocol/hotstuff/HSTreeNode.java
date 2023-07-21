package simulation.network.entity.hotstuff;

/**
 * Tree data structure that HotStuff uses to maintain the chain of consensus decisions.
 */
public class HSTreeNode {

    private final HSTreeNode parent;
    private final HSCommand command;
    private final int height;

    /**
     * Constructs a tree node in the consensus chain with {@code parent} node and {@code command}.
     * The {@code height} is defaulted to 1 if root or parent.height + 1 otherwise. It is identical to
     * the number of consensus reached.
     *
     * @param parent Parent node for the new node. {@code null} if it is a root node.
     * @param command {@code HSCommand} to be agreed upon in this node.
     */
    public HSTreeNode(HSTreeNode parent, HSCommand command) {
        this.parent = parent;
        this.command = command;
        this.height = parent == null ? 1 : parent.height + 1;
    }

    private HSTreeNode(HSTreeNode parent, HSCommand command, int height) {
        this.parent = parent;
        this.command = command;
        this.height = height;
    }

    /**
     * Returns true if {@code node} and {@code this} are equal or if {@code this} is a descendant of {@code node}.
     */
    public boolean extendsFrom(HSTreeNode node) {
        if (this.equals(node)) {
            return true;
        }

        if (node.height > this.height) {
            return false;
        }

        return this.parent.extendsFrom(node);
    }

    public int getHeight() {
        return height;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HSTreeNode)) {
            return false;
        }

        HSTreeNode other = (HSTreeNode) o;
        return height == other.height &&
                command.equals(other.command);
    }
}
