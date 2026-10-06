package ui;

import org.junit.jupiter.api.Test;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Container;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the "uninstrumented form" failure: the UI must build completely
 * with plain javac/Maven classes (no IntelliJ GUI Designer involved).
 */
class MainPanelConstructionTest {

    static int countComponents(Container c) {
        int n = 1;
        for (Component k : c.getComponents()) {
            n += (k instanceof Container) ? countComponents((Container) k) : 1;
        }
        return n;
    }

    @Test
    void constructor_buildsFullComponentTree() throws Exception {
        final MainPanel[] holder = new MainPanel[1];
        SwingUtilities.invokeAndWait(() -> { holder[0] = new MainPanel(); });
        MainPanel mp = holder[0];

        JPanel root = mp.getPanel();
        assertThat(root).isNotNull();
        // 40 form-bound components plus runtime additions (start screen etc.)
        assertThat(countComponents(root)).isGreaterThanOrEqualTo(40);
    }
}
