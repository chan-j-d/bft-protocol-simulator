package simulation.io;

public class ConsoleIo implements IoInterface {

    @Override
    public void output(String message) {
        System.out.println(message);
    }

    @Override
    public void close() {
        // do nothing
    }
}
