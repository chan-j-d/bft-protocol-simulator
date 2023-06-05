package simulation.network.entity.hotstuff;

public class HSTreeNode {

    private final HSTreeNode parent;
    private final HSCommand command;
    private final int height;

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

    public static HSTreeNode createRootNode(HSCommand command) {
        return new HSTreeNode(null, command, 0);
    }
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
