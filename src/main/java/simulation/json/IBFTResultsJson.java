package simulation.json;

public class IBFTResultsJson {

    private int n;
    private int consensusCount;
    private double t_total;
    private double t_newRound;
    private double t_prePrepared;
    private double t_prepared;

    public IBFTResultsJson(int n, int consensusCount, double t_newRound, double t_prePrepared,
            double t_prepared, double t_total) {
        this.n = n;
        this.consensusCount = consensusCount;
        this.t_total = t_total;
        this.t_newRound = t_newRound;
        this.t_prePrepared = t_prePrepared;
        this.t_prepared = t_prepared;
    }

    @Override
    public String toString() {
        return String.format("No. Nodes: %d\nNo. Consensus: %d\nt_newRound: %.3f\nt_prePrepared: %.3f\n" +
                "t_prepared: %.3f\nt_total: %.3f", n, consensusCount, t_newRound, t_prePrepared, t_prepared, t_total);
    }
}
