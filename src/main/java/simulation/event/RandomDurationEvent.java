package simulation.event;

public abstract class RandomDurationEvent extends Event {


    public RandomDurationEvent(double time) {
        super(time);
    }
    public abstract double generateRandomDuration();
}
