package emulator;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.SwingUtilities;

public final class Main {
    private static final int EXPECTED_ARGUMENT_COUNT = 2;

    private Main() {
    }

    public static void main(String[] args) {
        if (args.length != EXPECTED_ARGUMENT_COUNT) {
            System.err.println(
                    "Использование: ./run.sh "
                            + "<путь-к-vfs> "
                            + "<путь-к-скрипту>"
            );
            return;
        }

        EmulatorConfig config = new EmulatorConfig(
                Path.of(args[0]),
                Path.of(args[1])
        );

        if (!Files.isRegularFile(config.getVfsPath())) {
            System.err.println(
                    "Ошибка: файл VFS не найден: "
                            + config.getVfsPath()
            );
            return;
        }

        if (!Files.isRegularFile(config.getStartupScriptPath())) {
            System.err.println(
                    "Ошибка: стартовый скрипт не найден: "
                            + config.getStartupScriptPath()
            );
            return;
        }

        SwingUtilities.invokeLater(() -> {
            EmulatorWindow window =
                    new EmulatorWindow(config);
            window.setVisible(true);
        });
    }
}