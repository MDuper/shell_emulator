package emulator.vfs;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class VirtualFileSystem {
    private final VfsNode root;
    private final List<String> currentPathParts;

    public VirtualFileSystem(VfsNode root) {
        this.root = Objects.requireNonNull(
                root,
                "Корень VFS не может быть null"
        );

        currentPathParts = new ArrayList<>();
    }

    public VfsNode getRoot() {
        return root;
    }

    public String getCurrentPath() {
        if (currentPathParts.isEmpty()) {
            return "/";
        }

        return "/" + String.join("/", currentPathParts);
    }

    public VfsNode resolve(String path) {
        List<String> pathParts = normalizePath(path);
        VfsNode currentNode = root;

        for (String part : pathParts) {
            if (!currentNode.isDirectory()) {
                return null;
            }

            currentNode = currentNode.getChildren().get(part);

            if (currentNode == null) {
                return null;
            }
        }

        return currentNode;
    }

    private List<String> normalizePath(String path) {
        List<String> result = new ArrayList<>();

        if (path == null || path.isBlank()) {
            result.addAll(currentPathParts);
            return result;
        }

        if (!path.startsWith("/")) {
            result.addAll(currentPathParts);
        }

        for (String part : path.split("/")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }

            if (part.equals("..")) {
                if (!result.isEmpty()) {
                    result.remove(result.size() - 1);
                }
                continue;
            }

            result.add(part);
        }

        return result;
    }

    public boolean changeDirectory(String path) {
        VfsNode targetNode = resolve(path);

        if (targetNode == null || !targetNode.isDirectory()) {
            return false;
        }

        List<String> newPathParts = normalizePath(path);

        currentPathParts.clear();
        currentPathParts.addAll(newPathParts);

        return true;
    }

    public long calculateSize(VfsNode node) {
        if (node.isFile()) {
            return node.getContentBytes().length;
        }

        long totalSize = 0;

        for (VfsNode child : node.getChildren().values()) {
            totalSize += calculateSize(child);
        }

        return totalSize;
    }
}
