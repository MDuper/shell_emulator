package emulator;

import java.nio.file.Path;

public final class EmulatorConfig {
    private final Path vfsPath;
    private final Path startupScriptPath;

    public EmulatorConfig(
            Path vfsPath,
            Path startupScriptPath
    ) {
        this.vfsPath = vfsPath;
        this.startupScriptPath = startupScriptPath;
    }

    public Path getVfsPath() {
        return vfsPath;
    }

    public Path getStartupScriptPath() {
        return startupScriptPath;
    }


}
