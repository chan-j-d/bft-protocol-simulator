package simulation.io;

/**
 * {@code IoInterface} implementation that does not output messages.
 *
 * Mainly used for saving memory for large simulations.
 */
public class NoIo implements IoInterface {

    @Override
    public void output(String message) {
        // do nothing
    }

    @Override
    public void close() {
        // do nothing
    }
}
