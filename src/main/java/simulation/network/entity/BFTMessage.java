package simulation.network.entity;

public abstract class BFTMessage {

    public abstract String getType();
    public abstract int getRecipientId();
}
