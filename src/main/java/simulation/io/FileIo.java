package simulation.io;

import java.io.FileWriter;
import java.io.IOException;

public class FileIo implements IoInterface {

    private FileWriter fileWriter;

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
