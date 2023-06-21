package simulation;

import com.google.gson.Gson;
import simulation.io.IoInterface;
import simulation.io.NoIo;
import simulation.json.QueueResultsJson;
import simulation.json.RunConfigJson;
import simulation.json.ValidatorResultsJson;
import simulation.simulator.RunResults;
import simulation.simulator.Simulator;
import simulation.statistics.ConsensusStatistics;
import simulation.statistics.QueueStatistics;
import simulation.util.logging.Logger;
import simulation.util.rng.RNGUtil;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.logging.LogManager;

import static simulation.simulator.RunConfigUtil.createSimulator;

/**
 * Main driver class for simulations.
 */
public class BFTSimulation {

    /**
     * Default JSON directory.
     */
    private static final Path JSON_DIRECTORY = Paths.get("json");
    /**
     * JSON results path.
     */
    private static final String RESULTS_JSON_FILEPATH =
            JSON_DIRECTORY.resolve("validator_results.json").toString();
    /**
     * Switch statistics JSON path.
     */
    private static final String SWITCH_GROUP_STATISTICS =
            JSON_DIRECTORY.resolve("switch_group_%d.json").toString();
    private static final Gson GSON = new Gson();

    /**
     * Reads a runConfigJson file from {@code args} and runs a simulation based on it.
     */
    public static void main(String[] args) {
        setup();

        RunConfigJson runConfigJson = readFromJson(args[0], RunConfigJson.class);

        int numTrials = runConfigJson.getNumRuns();
        int seedMultiplier = runConfigJson.getSeedMultiplier();
        int startingSeed = runConfigJson.getStartingSeed();

//        IoInterface io = new FileIo("output.txt");
        IoInterface io = new NoIo();
        RunResults runResults = null;
        int numGroups = -1;
        for (int i = 0; i < numTrials; i++) {
            long seed = startingSeed + seedMultiplier * i;
            RunResults currentRunResults = runSimulation(seed, io, runConfigJson);

            io.output("\nSummary:");
            io.output(currentRunResults.toString());

            runResults = runResults == null ? currentRunResults : runResults.mergeRunResults(currentRunResults);
        }
        io.output(runResults.toString());
        io.close();

        ConsensusStatistics consensusStatistics = runResults.getValidatorStatistics();
        QueueStatistics validatorQueueStats = runResults.getValidatorQueueStatistics();
        List<QueueStatistics> switchStatistics = runResults.getSwitchStatistics();
        numGroups = switchStatistics.size();

        ValidatorResultsJson resultsJson = new ValidatorResultsJson(consensusStatistics, validatorQueueStats);
        writeObjectToJson(resultsJson, RESULTS_JSON_FILEPATH);

        for (int i = 0; i < numGroups; i++) {
            QueueStatistics queueStatistics = switchStatistics.get(i);
            QueueResultsJson queueResultsJson = new QueueResultsJson(queueStatistics.getAverageNumMessagesInQueue(),
                    queueStatistics.getMessageArrivalRate(), queueStatistics.getAverageMessageWaitingTime());
            writeObjectToJson(queueResultsJson, String.format(SWITCH_GROUP_STATISTICS, i));
        }

        System.out.println(consensusStatistics);
        System.out.println("\nAverage queue stats");
        System.out.println(validatorQueueStats);

        cleanup();
    }

    /**
     * Runs a singular simulation run with {@code seed} and outputs results via {@code io}.
     */
    private static RunResults runSimulation(long seed, IoInterface io, RunConfigJson configJson) {
        RNGUtil.setSeed(seed);
        Simulator simulator = createSimulator(configJson);
        while (!simulator.isSimulationOver()) {
            simulator.simulate().ifPresent(io::output);
        }
        return simulator.getRunResults();
    }

    /**
     * Reads {@code filename} into an object of class {@code clazz}.
     */
    public static <T> T readFromJson(String filename, Class<T> clazz) {
        try (FileReader fr = new FileReader(filename)) {
            return GSON.fromJson(fr, clazz);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to locate/parse %s file to read as Json.",
                    filename));
        }
    }

    /**
     * Writes {@code object} to {@code filename}.
     */
    public static void writeObjectToJson(Object object, String filename) {
        try (FileWriter fw = new FileWriter(filename)) {
            GSON.toJson(object, fw);
        } catch (IOException e) {
            throw new RuntimeException(String.format("Unable to write %s to %s json file.", object, filename));
        }
    }

    /**
     * Setup operation.
     * Run before running any simulations.
     */
    private static void setup() {
        deleteFilesInDirectory(Logger.DEFAULT_DIRECTORY);
        deleteFilesInDirectory(JSON_DIRECTORY.toString());
        Logger.setup();
    }

    /**
     * Deletes files in directory specified by {@code path}.
     */
    private static void deleteFilesInDirectory(String path) {
        try {
            File directory = new File(path);
            if (directory.exists() && directory.isDirectory()) {
                for (File file : directory.listFiles()) {
                    file.delete();
                }
            }
            Files.deleteIfExists(Paths.get(Logger.DEFAULT_DIRECTORY));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Cleanup operation after running all simulations.
     */
    private static void cleanup() {
        LogManager.getLogManager().reset();
    }
}