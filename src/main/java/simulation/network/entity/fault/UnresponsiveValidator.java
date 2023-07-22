package simulation.network.entity.fault;

import simulation.network.entity.BFTMessage;
import simulation.network.entity.Payload;
import simulation.network.entity.Validator;
import simulation.network.entity.timer.TimerNotifier;
import simulation.protocol.ConsensusProgram;
import simulation.util.Pair;
import simulation.util.rng.RandomNumberGenerator;

import java.util.List;

/**
 * Represents a defunct validator that stopped responding.
 */
public class UnresponsiveValidator<T extends BFTMessage> extends Validator<T> {

    /**
     * @param name Name of validator.
     * @param consensusLimit Consensus count limit.
     * @param timerNotifier TimerNotifier to check time and set timers.
     * @param serviceTimeGenerator RNG for service time.
     */
    public UnresponsiveValidator(String name, int consensusLimit, TimerNotifier<Validator<T>> timerNotifier,
            RandomNumberGenerator serviceTimeGenerator) {
        super(name, consensusLimit, timerNotifier, serviceTimeGenerator);
    }

    @Override
    public List<Payload<T>> initializationPayloads() {
        return List.of();
    }

    @Override
    public boolean isStillRequiredToRun() {
        return false;
    }

    @Override
    public Pair<Double, List<Payload<T>>> processPayload(double time, Payload<T> payload) {
        return new Pair<>(0.0, List.of());
    }

    @Override
    public void notifyAtTime(ConsensusProgram<T> program, double time, int timerCount) {
        return;
    }
}
