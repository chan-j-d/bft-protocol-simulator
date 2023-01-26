package simulation.util.timer;

public class TimeoutDoubler extends TimeoutHandler {

    public TimeoutDoubler(double startTime, double initialTimeout) {
        super(startTime, initialTimeout);
    }

    @Override
    public double getNextTimeoutTime(double timeout) {
        return timeout * 2;
    }
}
