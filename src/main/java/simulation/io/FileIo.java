package simulation.io;

import java.io.FileWriter;
import java.io.IOException;

/**
 * {@code IoInterface} that outputs results to a filename specified by {@code filename}.
 */
public class FileIo implements IoInterface {

    private FileWriter fileWriter;

    /**
     * @param filename Name of file to save results.
     */
    public FileIo(String filename) {
        try {
            fileWriter = new FileWriter(filename, false);
        } catch (IOException e) {
            System.err.println("Unable to create write file: " + e);
        }
    }

    @Override
    public void output(String message) {
        try {
            fileWriter.write(message + "\n");
        } catch (IOException e) {
            System.err.println("Unable to write to file: " + e);
        }
    }

    @Override
    public void close() {
        try {
            fileWriter.close();
        } catch (IOException e) {
            System.err.println("Unable to close file: " + e);
        }
    }
}
