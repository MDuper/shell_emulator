package emulator;

import javax.swing.SwingUtilities;


public final class Main {
    private Main() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            EmulatorWindow window = new EmulatorWindow();
            window.setVisible(true);
        });
    }
}