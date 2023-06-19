package simulation.network.entity;

/**
 * Encapsulates a payload traveling through a computer network.
 *
 * @param <T> Message class contained in payload.
 */
public class Payload<T> {

    private final T message;
    private final String destination;

    public Payload(T message, String destination) {
        this.message = message;
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public T getMessage() {
        return message;
    }
    @Override
    public String toString() {
        return "Payload: (" + message + ", " + destination + ")";
    }
}
