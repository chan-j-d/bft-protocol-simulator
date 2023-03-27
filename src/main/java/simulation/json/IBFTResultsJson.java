package simulation.json;

public class IBFTResultsJson {

    private double t_newRound;
    private double t_prePrepared;
    private double t_prepared;
    private double t_roundChange;
    private double t_total;

    public IBFTResultsJson(double t_newRound, double t_prePrepared,
            double t_prepared, double t_roundChange, double t_total) {
        this.t_total = t_total;
        this.t_newRound = t_newRound;
        this.t_prePrepared = t_prePrepared;
        this.t_roundChange = t_roundChange;
        this.t_prepared = t_prepared;
    }

    @Override
    public String toString() {
        return String.format("t_newRound: %.3f\nt_prePrepared: %.3f\n" +
                "t_prepared: %.3f\nt_total: %.3f", t_newRound, t_prePrepared, t_prepared, t_total);
    }
}
