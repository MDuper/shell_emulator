package emulator.vfs;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

public final class VfsNode {
    private VfsNodeType type;
    private VfsContentEncoding encoding;
    private String content;
    private Map<String, VfsNode> children;

    public VfsNode() {
        encoding = VfsContentEncoding.TEXT;
        content = "";
        children = new LinkedHashMap<>();
    }

    public VfsNodeType getType() {
        return type;
    }

    public void setType(VfsNodeType type) {
        this.type = type;
    }

    public VfsContentEncoding getEncoding() {
        return encoding;
    }

    public void setEncoding(VfsContentEncoding encoding) {
        if (encoding == null) {
            this.encoding = VfsContentEncoding.TEXT;
            return;
        }

        this.encoding = encoding;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        if (content == null) {
            this.content = "";
            return;
        }

        this.content = content;
    }

    public Map<String, VfsNode> getChildren() {
        return children;
    }

    public void setChildren(Map<String, VfsNode> children) {
        if (children == null) {
            this.children = new LinkedHashMap<>();
            return;
        }

        this.children = new LinkedHashMap<>(children);
    }

    public byte[] getContentBytes() {
        if (encoding == VfsContentEncoding.BASE64) {
            return Base64.getDecoder().decode(content);
        }

        return content.getBytes(StandardCharsets.UTF_8);
    }

    public boolean isFile() {
        return type == VfsNodeType.FILE;
    }

    public boolean isDirectory() {
        return type == VfsNodeType.DIRECTORY;
    }
}