package emulator.vfs;

import java.io.IOException;
import java.util.Map;

public final class VfsValidator {
    private VfsValidator() {
    }

    public static void validate(VfsNode root) throws IOException {
        if (root == null) {
            throw new IOException(
                    "JSON не содержит корневой узел"
            );
        }

        if (!root.isDirectory()) {
            throw new IOException(
                    "Корень VFS должен быть каталогом"
            );
        }

        validateNode(root, "/");
    }

    private static void validateNode(
            VfsNode node,
            String path
    ) throws IOException {
        if (node.getType() == null) {
            throw new IOException(
                    "Не указан тип узла: " + path
            );
        }

        if (node.isFile()) {
            validateFile(node, path);
            return;
        }

        if (!node.getContent().isEmpty()) {
            throw new IOException(
                    "Каталог содержит файловые данные: " + path
            );
        }

        for (Map.Entry<String, VfsNode> entry
                : node.getChildren().entrySet()) {
            String name = entry.getKey();
            validateName(name, path);

            VfsNode child = entry.getValue();
            String childPath = buildPath(path, name);

            if (child == null) {
                throw new IOException(
                        "Пустой узел: " + childPath
                );
            }

            validateNode(child, childPath);
        }
    }

    private static String buildPath(
            String parentPath,
            String name
    ) {
        if (parentPath.equals("/")) {
            return parentPath + name;
        }

        return parentPath + "/" + name;
    }

    private static void validateFile(
            VfsNode node,
            String path
    ) throws IOException {
        if (!node.getChildren().isEmpty()) {
            throw new IOException(
                    "Файл содержит дочерние узлы: " + path
            );
        }

        try {
            node.getContentBytes();
        } catch (IllegalArgumentException exception) {
            throw new IOException(
                    "Некорректный Base64 в файле: " + path,
                    exception
            );
        }
    }

    private static void validateName(
            String name,
            String parentPath
    ) throws IOException {
        if (name == null
                || name.isBlank()
                || name.contains("/")
                || name.equals(".")
                || name.equals("..")) {
            throw new IOException(
                    "Некорректное имя в каталоге: "
                            + parentPath
            );
        }
    }
}

