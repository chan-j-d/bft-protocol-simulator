package simulation.io;

/**
 * Contract for the type of input/output handling used by the simulator program.
 */
public interface IoInterface {

    /**
     * Displays {@code message} to the user.
     */
    void output(String message);

    /**
     * Closes the I/O stream.
     */
    void close();
}
