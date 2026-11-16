package ui;

import java.awt.Component;
import java.awt.Container;
import java.util.Optional;

/** Named Swing component lookup for presentation tests; missing controls fail explicitly. */
final class UiComponents {
    private UiComponents() { }

    static <T extends Component> T find(Container root, String name, Class<T> type) {
        Component component = locate(root, name)
                .orElseThrow(() -> new AssertionError("Missing component: " + name));
        if (!type.isInstance(component)) {
            throw new AssertionError("Wrong component type for " + name);
        }
        return type.cast(component);
    }

    private static Optional<Component> locate(Container root, String name) {
        for (Component child : root.getComponents()) {
            if (name.equals(child.getName())) {
                return Optional.of(child);
            }
            if (child instanceof Container) {
                Optional<Component> nested = locate((Container) child, name);
                if (nested.isPresent()) {
                    return nested;
                }
            }
        }
        return Optional.empty();
    }
}
