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
}
