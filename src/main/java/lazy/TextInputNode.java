package lazy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;
import processing.core.PConstants;
import processing.core.PGraphics;

public class TextInputNode extends AbstractNode {

    @Expose
    String content;

    TextInputNode(String path, FolderNode folder, String content) {
        super(NodeType.VALUE, path, folder);
        this.content = content;
        State.overwriteWithLoadedStateIfAny(this);
    }

    @Override
    protected void updateDrawInlineNodeAbstract(PGraphics pg) {
        fillForegroundBasedOnMouseOver(pg);
        drawRightText(pg, getDisplayValue(pg));
    }

    @Override
    void keyPressedOverNode(LazyKeyEvent e, float x, float y) {
        // based on tip #13 in here:
        // https://amnonp5.wordpress.com/2012/01/28/25-life-saving-tips-for-processing/
        if (isMouseOverNode) {
//            println(e.toString());
            if (e.getKeyCode() == PConstants.BACKSPACE) {
                if (content.length() > 0) {
                    content = content.substring(0, content.length() - 1);
                }
            } else if (e.getKeyCode() == KeyCodes.DELETE || e.getKeyChar() == PConstants.DELETE) {
                content = "";
            } else if (e.getKeyCode() == KeyCodes.CTRL_C && e.getKeyChar() != 'c') {
                Utils.setClipboardString(this.content);
            } else if (e.getKeyCode() == KeyCodes.CTRL_V && e.getKeyChar() != 'v') {
                content = Utils.getClipboardString();
            } else if (e.getKeyCode() != PConstants.SHIFT && e.getKeyCode() != PConstants.CONTROL && e.getKeyCode() != PConstants.ALT) {
                content = content + e.getKeyChar();
            }
        }
//        println(content.replaceAll("\\n", "(newline)"));
    }

    @Override
    void overwriteState(JsonElement loadedNode) {
        JsonObject json = loadedNode.getAsJsonObject();
        if (json.has("content")) {
            content = json.get("content").getAsString();
        }
    }

    public String getStringValue() {
        return content;
    }

    private String getDisplayValue(PGraphics pg) {
        if(content.endsWith("\n")){
            return "";
        }
        float availableWidth = parent.window.windowSizeX - pg.textWidth(name + " ") - State.textMarginX * 2;
        if(!content.contains("\n")){
            return Utils.getSubstringFromEndToFit(pg, content, availableWidth);
        }
        String[] lines = content.split("[\\r\\n]+");
        String lastLine = lines[lines.length-1];
        return Utils.getSubstringFromEndToFit(pg, lastLine, availableWidth);
    }

    @Override
    String getConsolePrintableValue() {
        return content;
    }
}
