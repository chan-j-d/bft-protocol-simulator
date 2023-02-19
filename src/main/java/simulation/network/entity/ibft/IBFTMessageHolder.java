package simulation.network.entity.ibft;

import java.util.List;

public class IBFTMessageHolder {

    public IBFTMessageHolder() {

    }

    public void addMessageToBacklog(IBFTMessage message) {
        // TODO
        return;
    }

    public boolean hasMoreHigherRoundChangeMessagesThan(int consensusInstance, int round) {
        //TODO
        return true;
    }

    public int getNextGreaterRoundChangeMessage(int consensusInstance, int round) {
        //TODO
        return 0;
    }

    public boolean hasQuorumOfMessages(IBFTMessageType type, int consensusInstance, int round) {
        //TODO
        return false;
    }

    public List<IBFTMessage> getQuorumOfMessages(IBFTMessageType type, int consensusInstance, int round) {
        //TODO
        return null;
    }

    public List<IBFTMessage> getMessages(IBFTMessageType type, int consensusInstance, int round) {
        //TODO
        return null;
    }

    public boolean hasQuorumOfMessagesOfSameRound(IBFTMessageType type, int consensusInstance) {
        //TODO
        return false;
    }
}
