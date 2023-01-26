package simulation.util;

public interface Queueable<T> {

    boolean isEmpty();
    void addToQueue(T t);
    T popFromQueue();
}
