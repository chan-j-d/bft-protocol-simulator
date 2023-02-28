package simulation.network.router;

import java.util.List;

public class RoutingUtil {

    public static <T> void updateRoutingTables(List<Switch<T>> switches) {
        boolean isAnyNodeUpdated;
        do {
            isAnyNodeUpdated = switches.stream().anyMatch(Switch::update);
        } while (isAnyNodeUpdated);
    }
}
