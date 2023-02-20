package simulation.network.entity;

public class Payload<T> {

    private T message;
    private String destination;

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
