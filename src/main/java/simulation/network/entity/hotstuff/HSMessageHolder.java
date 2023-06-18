package simulation.network.entity.hotstuff;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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

    public boolean hasQuorumOfMessages(HSMessageType type, int view, int quorum) {
        return voteMessageStorage.get(type).getOrDefault(view, List.of()).size() >= quorum;
    }
    public List<HSMessage> getVoteMessages(HSMessageType type, int view) {
        return Optional.of(voteMessageStorage.get(type).remove(view)).orElse(List.of());
    }

    public boolean containsLeaderMessage(HSMessageType type, int view) {
        return leaderMessageStorage.get(type).containsKey(view);
    }

    public HSMessage getLeaderMessage(HSMessageType type, int view) {
        return leaderMessageStorage.get(type).get(view);
    }

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
