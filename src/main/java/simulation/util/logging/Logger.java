package simulation.util.logging;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.FileHandler;
import java.util.logging.Level;

public class Logger {

    private static final String DEFAULT_DIRECTORY = "logs";
    private static final java.util.logging.Logger BACKUP_LOGGER = java.util.logging.Logger.getLogger("MAIN");

    private final String fileName;
    private java.util.logging.Logger logger;

    public Logger(String name) {
        this.fileName = name;
        logger = java.util.logging.Logger.getLogger(name);
        try {
            addFileHandler(logger, DEFAULT_DIRECTORY, fileName);
        } catch (IOException ioe) {
            BACKUP_LOGGER.log(Level.WARNING, "Unable to set up logger for " + name + "\n" +
                    Arrays.toString(ioe.getStackTrace()));
        }
    }

    public static void addFileHandler(java.util.logging.Logger logger, String directory, String filename)
            throws IOException {
        File file = new File(directory);
        if (file.exists() && !file.isDirectory()) {
            BACKUP_LOGGER.log(Level.WARNING, "Default logs directory name is being used.");
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
}
