package simulation.io;

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
