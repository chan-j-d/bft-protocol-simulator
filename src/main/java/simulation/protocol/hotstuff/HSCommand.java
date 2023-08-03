package simulation.protocol.hotstuff;

/**
 * Encapsulates a command proposed by a HotStuff validator.
 *
 * For ease of simulation, an {@code HSCommand} will only contain an agreed upon value
 * for the current consensus instance.
 */
public class HSCommand {

    private final int value;

    /**
     * @param value Proposed {@code value} for an HotStuff consensus instance.
     */
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

    @Override
    public String toString() {
        return "Value: " + value;
    }
}
