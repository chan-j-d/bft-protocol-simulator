package simulation.util.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;

public class Logger {

    public static final String DEFAULT_DIRECTORY = "logs";
    public static Logger MAIN_LOGGER;

    private final String fileName;
    private java.util.logging.Logger logger;

    public static void setup() {
        MAIN_LOGGER = new Logger("MAIN");
    }

    public Logger(String name) {
        this.fileName = name;
        logger = java.util.logging.Logger.getLogger(name);
        logger.setUseParentHandlers(false);
        try {
            addFileHandler(logger, DEFAULT_DIRECTORY, fileName);
        } catch (IOException ioe) {
            MAIN_LOGGER.log(Level.WARNING, "Unable to set up logger for " + name + "\n" +
                    Arrays.toString(ioe.getStackTrace()));
        }
    }

    public static void addFileHandler(java.util.logging.Logger logger, String directory, String filename)
            throws IOException {
        File file = new File(directory);
        if (file.exists() && !file.isDirectory()) {
            MAIN_LOGGER.log(Level.WARNING, "Default logs directory name is being used.");
            return;
        } else if (!file.exists()) {
            Files.createDirectory(Paths.get(directory));
        }

        FileHandler fileHandler = new FileHandler(Paths.get(directory, filename).toString());
        fileHandler.setFormatter(new LogFormatter());
        logger.addHandler(fileHandler);
    }

    public void log(String message) {
        logger.log(Level.INFO, message);
    }

    public void log(Level level, String message) {
        logger.log(level, message);
    }
}
