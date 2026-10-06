package emulator;

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

public final class EmulatorWindow extends JFrame {

    private static final int WINDOW_WIDTH = 800;
    private static final int WINDOW_HEIGHT = 500;
    private static final int EXIT_ARGUMENT_COUNT = 0;
    private static final Pattern ENVIRONMENT_VARIABLE_PATTERN =
            Pattern.compile("\\$([A-Za-z_][A-Za-z0-9_]*)");

    private final JTextArea outputArea;
    private final JTextField inputField;
    private final EmulatorConfig config;

    public EmulatorWindow(EmulatorConfig config) {
        super(createTitle());

        this.config = config;

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

    private boolean executeStartupComand(String commandLine) {
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
                        executeStartupComand(command);

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
            case "ls", "cd" -> {
                executeStub(command, arguments);
                yield true;
            }
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

    private void executeStub(
            String command,
            String[] arguments
    ) {
        outputArea.append("Команда: " + command + "\n");
        outputArea.append(
                "Аргументы: "
                    + String.join(" ", arguments)
                    + "\n"
        );
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
    }
}