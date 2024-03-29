package simulation.network.entity;

import simulation.network.entity.timer.TimerNotifier;
import simulation.protocol.ConsensusProgram;
import simulation.simulator.ValidatorResults;
import simulation.statistics.ConsensusStatistics;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Encapsulates an {@code EndpointNode} that runs a BFT protocol.
 *
 * @param <T> Message class generated by {@code Validator}.
 */
public class Validator<T extends BFTMessage> extends EndpointNode<T>
        implements ValidatorResults, TimerNotifier<ConsensusProgram<T>> {

    private final Map<Integer, Validator<T>> allNodes;
    private final int consensusLimit;
    private final TimerNotifier<Validator<T>> timerNotifier;
    private final RandomNumberGenerator rng;

    private final Map<Integer, String> idNodeNameMap;
    private final Map<Integer, ConsensusProgram<T>> consensusPrograms;
    private final Map<ConsensusProgram<T>, Integer> programToIdMap;

    private double previousRecordedTime;

    /**
     * @param name Name of validator.
     * @param consensusLimit Consensus count limit.
     * @param timerNotifier TimerNotifier to check time and set timers.
     * @param serviceTimeGenerator RNG for service time.
     */
    public Validator(String name, Map<Integer, String> idNodeNameMap, int consensusLimit,
            TimerNotifier<Validator<T>> timerNotifier,
            RandomNumberGenerator serviceTimeGenerator) {
        super(name);
        this.timerNotifier = timerNotifier;
        this.idNodeNameMap = idNodeNameMap;
        this.rng = serviceTimeGenerator;
        this.allNodes = new HashMap<>();
        this.consensusLimit = consensusLimit;
        this.consensusPrograms = new HashMap<>();
        this.programToIdMap = new HashMap<>();
    }

    public void addConsensusProgram(ConsensusProgram<T> consensusProgram) {
        this.consensusPrograms.put(this.consensusPrograms.size() + 1, consensusProgram);
        this.programToIdMap.put(consensusProgram, this.programToIdMap.size() + 1);
    }

    @Override
    public ConsensusStatistics getConsensusStatistics(int programNumber) {
        return consensusPrograms.get(programNumber).getStatistics();
    }

    @Override
    public int getNumConsensusPrograms() {
        return consensusPrograms.size();
    }

    @Override
    public boolean isStillRequiredToRun() {
        return consensusPrograms.values().stream().anyMatch(p -> p.getConsensusCount() < consensusLimit);
    }

    @Override
    public List<Payload<T>> initializationPayloads() {
        return consensusPrograms.keySet().stream()
                .flatMap(x -> convertMessagesToPayloads(consensusPrograms.get(x).initializationPayloads(), x).stream())
                .collect(Collectors.toList());
    }

    /**
     * Processes payload and returns duration and list of resulting payloads.
     * Duration is generated randomly by an exponential random variable.
     * New time is set to be {@code time} + duration generated in order to set the node as occupied up to end time.
     * A consequence of this is that timeouts are only registered after the message that crosses
     * the timeout is processed.
     *
     * @param time Time payload is being processed.
     * @param payload Payload to be processed.
     * @return Returns time taken to process the payload and list of resulting payloads from processing.
     */
    @Override
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        double duration = rng.generateRandomNumber();
        previousRecordedTime = time + duration;
        T message = payload.getMessage();
        int programId = payload.getProgramId();
        ConsensusProgram<T> consensusProgram = consensusPrograms.get(programId);
        List<T> responseMessages = consensusProgram.processAndRegisterMessage(message, previousRecordedTime);
        return new Pair<>(duration, convertMessagesToPayloads(responseMessages, programId));
    }

    private String getIdNodeName(int id) {
        return idNodeNameMap.get(id);
    }

    private List<Payload<T>> convertMessagesToPayloads(List<? extends T> messages, int programId) {
        return messages.stream().map(m -> new Payload<T>(m, getIdNodeName(m.getRecipientId()), programId))
                .collect(Collectors.toList());
    }

    public List<Payload<T>> notifyTime(int id, int timerCount) {
        return convertMessagesToPayloads(consensusPrograms.get(id).notifyTime(timerCount), id);
    }

    @Override
    public void notifyAtTime(ConsensusProgram<T> program, double time, int id, int timerCount) {
        timerNotifier.notifyAtTime(this, time, programToIdMap.get(program), timerCount);
    }

    @Override
    public double getTime() {
        return timerNotifier.getTime();
    }

    @Override
    public String toString() {
        return super.toString() + ": " + consensusPrograms.toString();
    }
}
