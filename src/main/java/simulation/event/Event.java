package simulation.event;

import simulation.util.Printable;

import java.util.List;

public abstract class Event implements Comparable<Event>, Printable {
    private double time;

    public Event(double time) {
        this.time = time;
    }

    public double getTime() {
        return time;
    }

    @Override
    public int compareTo(Event e) {
        if (this.time > e.time) {
            return 1;
        } else if (this.time == e.time) {
            return 0;
        } else {
            return -1;
        }
    }

    @Override
    public String toString() {
        return String.format("%.3f", time);
    }

    public abstract List<Event> simulate();

    @Override
    public boolean toDisplay() {
        return true;
    }
}
