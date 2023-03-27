package simulation.util;

public class MathUtil {

    public static int ceilDiv(int a, int b) {
        return (int) Math.ceil((double) a / b);
    }

    public static double log(double a, double base) {
        return Math.log(a) / Math.log(base);
    }
}
