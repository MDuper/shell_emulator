package emulator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.net.InetAddress;
import java.net.UnknownHostException;
import javax.swing.SwingUtilities;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import emulator.vfs.VfsNode;
import emulator.vfs.VirtualFileSystem;

public final class EmulatorWindow extends JFrame {

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 500;
    private static final int EXIT_ARGUMENT_COUNT = 0;
    private static final int CD_ARGUMENT_COUNT = 1;
    private static final int LS_MAX_ARGUMENT_COUNT = 1;
    private static final int DU_MAX_ARGUMENT_COUNT = 1;
    private static final int CP_ARGUMENT_COUNT = 2;
    private static final int CP_RECURSIVE_ARGUMENT_COUNT = 3;
    private static final String RECURSIVE_OPTION = "-r";
    private static final int RM_ARGUMENT_COUNT = 1;
    private static final int RM_RECURSIVE_ARGUMENT_COUNT = 2;

    private static final Pattern ENVIRONMENT_VARIABLE_PATTERN =
            Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");
    private static final int DATE_ARGUMENT_COUNT = 0;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final int HELP_ARGUMENT_COUNT = 0;
    private static final String HELP_TEXT = """
        Доступные команды:
          ls [PATH]                     показать содержимое каталога
          cd PATH                       перейти в каталог
          date                          показать дату и время
          du [PATH]                     показать размер файла или каталога
          cp SOURCE DESTINATION         скопировать файл
          cp -r SOURCE DESTINATION      скопировать каталог
          rm PATH                       удалить файл
          rm -r PATH                    удалить каталог
          help                          показать список команд
          exit                          завершить работу эмулятора
        """;

    private final JTextArea outputArea;
    private final JTextField inputField;
    private final EmulatorConfig config;
    private final VirtualFileSystem virtualFileSystem;

    public EmulatorWindow(
            EmulatorConfig config,
            VirtualFileSystem virtualFileSystem
    ) {
        super(createTitle());

        this.config = config;
        this.virtualFileSystem = virtualFileSystem;

        outputArea = new JTextArea();
        outputArea.setEditable(false);
        outputArea.setText("Эмулятор готов к работе.\n");

        displayConfiguration();

        inputField = new JTextField();
        inputField.addActionListener(event -> handleInput());

        JScrollPane scrollPane = new JScrollPane(outputArea);

        setLayout(new BorderLayout());
        add(scrollPane, BorderLayout.CENTER);
        add(inputField, BorderLayout.SOUTH);

        setSize(WINDOW_WIDTH, WINDOW_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        SwingUtilities.invokeLater(this::executeStartupScript);
    }

    private void handleInput() {
        String input = inputField.getText().trim();

        if (input.isEmpty()) {
            return;
        }

        String expandedInput =
                expandEnvironmentVariables(input);

        outputArea.append("> " + input + "\n");
        executeCommand(expandedInput);
        inputField.setText("");
    }

    private boolean executeStartupCommand(String commandLine) {
        String input = commandLine.trim();

        if (input.isEmpty()) {
            return true;
        }

        outputArea.append("> " + input + "\n");

        String expandedInput =
                expandEnvironmentVariables(input);

        return executeCommand(expandedInput);
    }

    private void executeStartupScript() {
        try {
            List<String> commands =
                    Files.readAllLines(
                            config.getStartupScriptPath()
                    );
            for (String command : commands) {
                boolean successful =
                        executeStartupCommand(command);

                if (!successful) {
                    outputArea.append(
                            "Выполнение стартового скрипта приостановлено.\n"
                    );
                    return;
                }
            }
        } catch (IOException exception) {
            outputArea.append(
                    "Ошибка чтения стартового скрипта: "
                            + exception.getMessage()
                            + "\n"
            );
        }
    }

    private static String expandEnvironmentVariables(
            String input
    ) {
        Matcher matcher =
                ENVIRONMENT_VARIABLE_PATTERN.matcher(input);

        StringBuilder result = new StringBuilder();

        while (matcher.find()) {
            String variableName = matcher.group(1);
            String variableValue = System.getenv(variableName);

            if (variableValue == null) {
                variableValue = matcher.group();
            }

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(variableValue)
            );
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private boolean executeCommand(String input) {
        String[] parts = input.split("\\s+");
        String command = parts[0];

        String[] arguments = Arrays.copyOfRange(
                parts,
                1,
                parts.length
        );

        return switch (command) {
            case "ls" -> executeLs(arguments);
            case "cd" -> executeCd(arguments);
            case "date" -> executeDate(arguments);
            case "du" -> executeDu(arguments);
            case "cp" -> executeCp(arguments);
            case "rm" -> executeRm(arguments);
            case "help" -> executeHelp(arguments);
            case "exit" -> executeExit(arguments);
            default -> {
                outputArea.append(
                        "Ошибка: неизвестная команда "
                                + command
                                + "\n"
                );
                yield false;
            }
        };
    }

    private boolean executeExit(String[] arguments) {
        if (arguments.length != EXIT_ARGUMENT_COUNT) {
            outputArea.append(
                    "Ошибка: exit не принимает аргументы\n"
            );
            return false;
        }

        dispose();
        return true;
    }

    private boolean executeCd(String[] arguments) {
        if (arguments.length != CD_ARGUMENT_COUNT) {
            outputArea.append(
                    "Ошибка: cd ожидает один аргумент\n"
            );
            return false;
        }

        String path = arguments[0];
        VfsNode targetNode = virtualFileSystem.resolve(path);

        if (targetNode == null) {
            outputArea.append(
                    "Ошибка: путь не найден: "
                            + path
                            + "\n"
            );
            return false;
        }

        if (!targetNode.isDirectory()) {
            outputArea.append(
                    "Ошибка: не является каталогом: "
                            + path
                            + "\n"
            );
            return false;
        }

        virtualFileSystem.changeDirectory(path);

        outputArea.append(
                "Текущий каталог: "
                        + virtualFileSystem.getCurrentPath()
                        + "\n"
        );

        return true;
    }

    private boolean executeLs(String[] arguments) {
        if (arguments.length > LS_MAX_ARGUMENT_COUNT) {
            outputArea.append(
                    "Ошибка: ls принимает не более одного аргумента\n"
            );
            return false;
        }

        String path = "";

        if (arguments.length == LS_MAX_ARGUMENT_COUNT) {
            path = arguments[0];
        }

        VfsNode targetNode =  virtualFileSystem.resolve(path);

        if (targetNode == null) {
            outputArea.append(
                    "Ошибка: путь не найден: "
                            + path
                            + "\n"
            );
            return false;
        }

        if (targetNode.isFile()) {
            outputArea.append(path + "\n");
            return true;
        }

        for (String name : targetNode.getChildren().keySet()) {
            outputArea.append(name + "\n");
        }

        return true;
    }

    private boolean executeDate(String[] arguments) {
        if (arguments.length != DATE_ARGUMENT_COUNT) {
            outputArea.append(
                    "Ошибка: date не принимает аргументы\n"
            );
            return false;
        }

        LocalDateTime currentDateTime = LocalDateTime.now();

        String formatDateTime =
                currentDateTime.format(DATE_FORMATTER);

        outputArea.append(formatDateTime + "\n");

        return true;
    }

    private boolean executeDu(String[] arguments) {
        if (arguments.length > DU_MAX_ARGUMENT_COUNT) {
            outputArea.append(
                    "Ошибка: du принимает не более одного аргумента\n"
            );
            return false;
        }

        String path = "";

        if (arguments.length == DU_MAX_ARGUMENT_COUNT) {
            path = arguments[0];
        }

        VfsNode targetNode = virtualFileSystem.resolve(path);

        if (targetNode == null) {
            outputArea.append(
                    "Ошибка: путь не найден: "
                            + path
                            + "\n"
            );
            return false;
        }

        long size = virtualFileSystem.calculateSize(targetNode);

        String displayedPath = path;

        if (displayedPath.isBlank()) {
            displayedPath = virtualFileSystem.getCurrentPath();
        }

        outputArea.append(
                size
                        + " байт\t"
                        + displayedPath
                        + "\n"
        );

        return true;
    }

    private boolean executeCp(String[] arguments) {
        boolean regularMode =
                arguments.length == CP_ARGUMENT_COUNT
                        && !arguments[0].equals(RECURSIVE_OPTION);

        boolean recursiveMode =
                arguments.length == CP_RECURSIVE_ARGUMENT_COUNT
                        && arguments[0].equals(RECURSIVE_OPTION);

        if (!regularMode && !recursiveMode) {
            outputArea.append(
                    "Ошибка: используйте cp SOURCE DESTINATION "
                            + "или cp -r SOURCE DESTINATION\n"
            );
            return false;
        }

        int sourceIndex = recursiveMode ? 1 : 0;
        int destinationIndex = sourceIndex + 1;

        String sourcePath = arguments[sourceIndex];
        String destinationPath = arguments[destinationIndex];

        VfsNode sourceNode =
                virtualFileSystem.resolve(sourcePath);

        if (sourceNode == null) {
            outputArea.append(
                    "Ошибка: исходный путь не найден: "
                            + sourcePath
                            + "\n"
            );
            return false;
        }

        if (sourceNode.isDirectory() && !recursiveMode) {
            outputArea.append(
                    "Ошибка: для копирования каталога "
                            + "требуется флаг -r\n"
            );
            return false;
        }

        if (!virtualFileSystem.copy(
                sourcePath,
                destinationPath,
                recursiveMode
        )) {
            outputArea.append(
                    "Ошибка: не удалось скопировать в "
                            + destinationPath
                            + "\n"
            );
            return false;
        }

        outputArea.append(
                "Скопировано: "
                        + sourcePath
                        + " -> "
                        + destinationPath
                        + "\n"
        );

        return true;
    }

    private boolean executeRm(String[] arguments) {
        boolean regularMode =
                arguments.length == RM_ARGUMENT_COUNT
                    && !arguments[0].equals(RECURSIVE_OPTION);

        boolean recursiveMode =
                arguments.length == RM_RECURSIVE_ARGUMENT_COUNT
                        && arguments[0].equals(RECURSIVE_OPTION);

        if (!regularMode && !recursiveMode) {
            outputArea.append(
                    "Ошибка: используйте rm PATH "
                            + "или rm -r PATH\n"
            );
            return false;
        }

        int pathIndex = recursiveMode ? 1 : 0;
        String path = arguments[pathIndex];

        VfsNode targetNode = virtualFileSystem.resolve(path);

        if (targetNode == null) {
            outputArea.append(
                    "Ошибка: путь не найден: "
                            + path
                            + "\n"
            );
            return false;
        }

        if (targetNode.isDirectory() && !recursiveMode) {
            outputArea.append(
                    "Ошибка: для удаления каталога "
                            + "требуется флаг -r\n"
            );
            return false;
        }

        if (!virtualFileSystem.remove(path, recursiveMode)) {
            outputArea.append(
                    "Ошибка: не удалось удалить "
                            + path
                            + "\n"
            );
            return false;
        }

        outputArea.append(
                "Удалено: "
                        + path
                        + "\n"
        );

        return true;
    }

    private boolean executeHelp(String[] arguments) {
        if (arguments.length != HELP_ARGUMENT_COUNT) {
            outputArea.append(
                    "Ошибка: help не принимает аргументы\n"
            );
            return false;
        }

        outputArea.append(HELP_TEXT);

        return true;
    }

    private static String createTitle() {
        String username = System.getProperty(
                "user.name",
                "unknown-user"
        );

        return "Эмулятор - ["
                + username
                + "@"
                + getHostname()
                + "]";
    }

    private static String getHostname() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException exception) {
            return "unknown-host";
        }
    }

    private void displayConfiguration() {
        outputArea.append("\nПараметры запуска:\n");

        outputArea.append(
                "VFS: "
                + config.getVfsPath()
                +"\n"
        );

        outputArea.append(
                "Стартовый скрипт: "
                        + config.getStartupScriptPath()
                        +"\n"
        );

        outputArea.append(
                "Текущий каталог VFS: "
                        + virtualFileSystem.getCurrentPath()
                        + "\n"
        );

        outputArea.append(
                "Объектов в корне VFS: "
                        + virtualFileSystem
                        .getRoot()
                        .getChildren()
                        .size()
                        + "\n"
        );
    }
}