package emulator.vfs;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;

public final class VfsLoader {
    private final ObjectMapper objectMapper;

    public VfsLoader() {
        objectMapper = new ObjectMapper();
    }

    public VfsNode load(Path path) throws IOException {
        VfsNode root = objectMapper.readValue(
                path.toFile(),
                VfsNode.class
        );

        VfsValidator.validate(root);

        return root;
    }
}