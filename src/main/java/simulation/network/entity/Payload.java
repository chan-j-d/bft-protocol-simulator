package simulation.network.entity;

/**
 * Encapsulates a payload traveling through a computer network.
 *
 * @param <T> Message class contained in payload.
 */
public class Payload<T> {

    private final T message;
    private final String destination;
    private final int programId;

    public Payload(T message, String destination, int programId) {
        this.message = message;
        this.destination = destination;
        this.programId = programId;
    }

    public String getDestination() {
        return destination;
    }

    public T getMessage() {
        return message;
    }

    public int getProgramId() {
        return programId;
    }

    @Override
    public String toString() {
        return "Payload: (" + message + ", " + destination + ")";
    }
}
