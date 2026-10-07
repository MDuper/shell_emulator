package emulator;

import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.SwingUtilities;
import java.io.IOException;
import emulator.vfs.VfsLoader;
import emulator.vfs.VfsNode;
import emulator.vfs.VirtualFileSystem;

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

        VfsNode root = loadVfs(config.getVfsPath());

        if (root == null) {
            return;
        }

        VirtualFileSystem virtualFileSystem =
                new VirtualFileSystem(root);

        SwingUtilities.invokeLater(() -> {
            EmulatorWindow window =
                    new EmulatorWindow(
                            config,
                            virtualFileSystem
                    );
            window.setVisible(true);
        });
    }

    private static VfsNode loadVfs(Path path) {
        try {
            VfsLoader loader = new VfsLoader();
            VfsNode root = loader.load(path);

            System.out.println(
                    "VFS загружена. Объектов в корне: "
                            + root.getChildren().size()
            );

            return root;
        } catch (IOException exception) {
            System.err.println(
                    "Ошибка загрузки VFS: "
                            + exception.getMessage()
            );
            return null;
        }
    }
}