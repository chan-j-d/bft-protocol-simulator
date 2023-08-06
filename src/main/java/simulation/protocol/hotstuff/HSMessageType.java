package simulation.protocol.hotstuff;

/**
 * Enum for different message types the HotStuff protocol requires.
 */
public enum HSMessageType {

    NEW_VIEW("NEW_VIEW"), PREPARE("PREPARE"), PRE_COMMIT("PRE_COMMIT"), COMMIT("COMMIT"), DECIDE("DECIDE");

    private String name;

    HSMessageType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
