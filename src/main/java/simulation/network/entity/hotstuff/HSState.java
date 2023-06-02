package simulation.network.entity.hotstuff;

public enum HSState {

    PREPARE("PREPARE"), PRECOMMIT("PRECOMMIT"), COMMIT("COMMIT"), DECIDE("DECIDE");

    private String name;

    HSState(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
