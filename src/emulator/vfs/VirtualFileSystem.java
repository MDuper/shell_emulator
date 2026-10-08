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
        return resolveParts(normalizePath(path));
    }

    private VfsNode resolveParts(List<String> pathParts) {
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

    private VfsNode resolveParent(String path) {
        List<String> pathParts = normalizePath(path);

        if (pathParts.isEmpty()) {
            return null;
        }

        pathParts.remove(pathParts.size() - 1);

        return resolveParts(pathParts);
    }

    private String getName(String path) {
        List<String> pathParts = normalizePath(path);

        if (pathParts.isEmpty()) {
            return "";
        }

        return pathParts.get(pathParts.size() - 1);
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

    public boolean copy(
            String sourcePath,
            String destinationPath,
            boolean recursive
    ) {
        VfsNode sourceNode = resolve(sourcePath);

        if (sourceNode == null) {
            return false;
        }

        if (sourceNode.isDirectory() && !recursive) {
            return false;
        }

        VfsNode destinationNode = resolve(destinationPath);
        VfsNode destinationParent;
        String destinationName;

        if (destinationNode != null
                && destinationNode.isDirectory()) {
            destinationParent = destinationNode;
            destinationName = getName(sourcePath);
        } else {
            destinationParent = resolveParent(destinationPath);
            destinationName = getName(destinationPath);
        }

        if (destinationParent == null
                || !destinationParent.isDirectory()
                || destinationName.isBlank()) {
            return false;
        }

        destinationParent.getChildren().put(
                destinationName,
                sourceNode.deepCopy()
        );

        return true;
    }

    public boolean remove(
            String path,
            boolean recursive
    ) {
        List<String> pathParts = normalizePath(path);

        if (pathParts.isEmpty()
                || containsCurrentDirectory(pathParts)) {
            return false;
        }

        VfsNode targetNode = resolveParts(pathParts);

        if (targetNode == null) {
            return false;
        }

        if (targetNode.isDirectory() && !recursive) {
            return false;
        }

        String targetName =
                pathParts.remove(pathParts.size() - 1);

        VfsNode parentNode = resolveParts(pathParts);

        if (parentNode == null || !parentNode.isDirectory()) {
            return false;
        }

        return parentNode.getChildren().remove(targetName)
                != null;
    }

    private boolean containsCurrentDirectory(
            List<String> pathParts
    ) {
        if (pathParts.size() > currentPathParts.size()) {
            return false;
        }

        return currentPathParts
                .subList(0, pathParts.size())
                .equals(pathParts);
    }
}
