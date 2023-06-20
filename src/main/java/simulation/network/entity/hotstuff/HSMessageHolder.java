package simulation.network.entity.hotstuff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Helper class that handles message storage and retrieval for a HotStuff replica.
 */
public class HSMessageHolder {

    private final Map<HSMessageType, Map<Integer, List<HSMessage>>> voteMessageStorage;
    private final Map<HSMessageType, Map<Integer, HSMessage>> leaderMessageStorage;

    public HSMessageHolder() {
        voteMessageStorage = new HashMap<>();
        leaderMessageStorage = new HashMap<>();
        for (HSMessageType type : HSMessageType.values()) {
            voteMessageStorage.put(type, new HashMap<>());
            leaderMessageStorage.put(type, new HashMap<>());
        }
    }

    /**
     * Adds {@code message} to its backlog.
     */
    public void addMessage(HSMessage message) {
        HSMessageType type = message.getType();
        int view = message.getViewNumber();
        boolean isVote = message.isVote();
        if (isVote) {
            Map<Integer, List<HSMessage>> typeMap = voteMessageStorage.get(type);
            if (!typeMap.containsKey(view)) {
                typeMap.put(view, new ArrayList<>());
            }

            List<HSMessage> messages = typeMap.get(view);
            messages.add(message);
        } else {
            // assumes only one (non-vote) message of each type per view (from the leader)
            leaderMessageStorage.get(type).put(view, message);
        }
    }

    /**
     * Returns true if backlog contains a quorum of messages of {@code type} and {@code view}.
     * Convenience function to check for quorum before retrieving it.
     */
    public boolean hasQuorumOfMessages(HSMessageType type, int view, int quorum) {
        return voteMessageStorage.get(type).getOrDefault(view, List.of()).size() >= quorum;
    }

    /**
     * Returns a list of messages of {@code type} and {@code view} that the leader has received.
     */
    public List<HSMessage> getVoteMessages(HSMessageType type, int view) {
        return Optional.of(voteMessageStorage.get(type).remove(view)).orElse(List.of());
    }

    /**
     * Returns true if as a replica, it has received a message of {@code type} and {@code view} from the leader.
     * Convenience function to check for presence of leader message before retrieving it.
     */
    public boolean containsLeaderMessage(HSMessageType type, int view) {
        return leaderMessageStorage.get(type).containsKey(view);
    }

    /**
     * Returns leader message for {@code type} and {@code view}.
     */
    public HSMessage getLeaderMessage(HSMessageType type, int view) {
        return leaderMessageStorage.get(type).get(view);
    }

    /**
     * Removes redundant messages in older views from the backlog.
     * This is a non-protocol related message used to reduce the memory usage of a large simulation.
     */
    public void advanceView(int oldView, int newView) {
        for (int i = oldView; i < newView; i++) {
            for (HSMessageType type : HSMessageType.values()) {
                if (type == HSMessageType.NEW_VIEW) {
                    voteMessageStorage.get(type).remove(i - 1);
                    leaderMessageStorage.get(type).remove(i - 1);
                } else {
                    voteMessageStorage.get(type).remove(i);
                    leaderMessageStorage.get(type).remove(i);
                }
            }
        }
    }
}
