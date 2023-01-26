package simulation.util.timer;

public abstract class TimeoutHandler {

    private double startTime;
    private double timeout;
    private boolean hasTimedOut;

    public TimeoutHandler(double startTime, double initialTimeout) {
        this.startTime = startTime;
        this.timeout = initialTimeout;
        this.hasTimedOut = false;
    }

    public void setStartTime(double time) {
        this.startTime = time;
        if (hasTimedOut) {
            timeout = getNextTimeoutTime(timeout);
        }
    }
    public boolean hasTimedOut(double currentTime) {
         hasTimedOut = currentTime >= getTimeoutTime();
         return hasTimedOut;
    }

    public double getTimeoutTime() {
        return startTime + timeout;
    }

    public abstract double getNextTimeoutTime(double timeout);
}
