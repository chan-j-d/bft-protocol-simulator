package simulation.network.entity.fault;

import simulation.network.entity.BFTMessage;
import simulation.network.entity.Payload;
import simulation.network.entity.Validator;
import simulation.network.entity.timer.TimerNotifier;
import simulation.protocol.ConsensusProgram;
import simulation.util.Pair;
import simulation.util.rng.ExponentialDistribution;
import simulation.util.rng.RandomNumberGenerator;

import java.util.List;

/**
 * Represents a defunct validator that stopped responding.
 */
public class UnresponsiveValidator<T extends BFTMessage> extends Validator<T> {

    private static final RandomNumberGenerator DUMMY_RNG = new ExponentialDistribution(1);
    private static final int DUMMY_CONSENSUS_LIMIT = -1;

    /**
     * @param name Name of validator.
     * @param timerNotifier TimerNotifier to check time and set timers.
     */
    public UnresponsiveValidator(String name, TimerNotifier<Validator<T>> timerNotifier) {
        super(name, DUMMY_CONSENSUS_LIMIT, timerNotifier, DUMMY_RNG);
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
