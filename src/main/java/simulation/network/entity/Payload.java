package simulation.network.entity;

public class Payload {

    private String message;
    private String destination;

    public Payload(String message, String destination) {
        this.message = message;
        this.destination = destination;
    }

    public String getDestination() {
        return destination;
    }

    public String getMessage() {
        return message;
    }
    @Override
    public String toString() {
        return "Payload: (" + message + ", " + destination + ")";
    }
}
