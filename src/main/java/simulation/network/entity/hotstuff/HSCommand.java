package simulation.network.entity.hotstuff;

public class HSCommand {

    private final int value;

    public HSCommand(int value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof HSCommand)) {
            return false;
        }
        HSCommand other = (HSCommand) o;
        return value == other.value;
    }
}
