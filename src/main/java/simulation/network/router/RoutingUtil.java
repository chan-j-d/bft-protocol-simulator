package simulation.network.router;

import java.util.List;

/**
 * Contains utility methods for routing.
 */
public class RoutingUtil {

    /**
     * Updates the routing tables of in the list of {@code switches}.
     */
    public static <T> void updateRoutingTables(List<Switch<T>> switches) {
        boolean isAnyNodeUpdated;
        do {
            isAnyNodeUpdated = switches.stream().anyMatch(Switch::update);
        } while (isAnyNodeUpdated);
    }
}
