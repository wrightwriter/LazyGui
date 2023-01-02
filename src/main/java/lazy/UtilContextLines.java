package lazy;

import processing.core.PConstants;
import processing.core.PGraphics;

import java.util.List;

import static lazy.ColorStore.normColor;
import static lazy.Globals.gui;

public class UtilContextLines {
    public static final String NEVER = "never";
    public static final String ON_HOVER = "on hover";
    public static final String ALWAYS = "always";
    public static final int SHOW_CONTEXT_LINES_MODE_NEVER = 0;
    public static final int SHOW_CONTEXT_LINES_MODE_ON_HOVER = 1;
    public static final int SHOW_CONTEXT_LINES_ALWAYS = 2;
    public static final List<String> contextLinesOptions = new UtilArrayListBuilder<String>()
            .add(NEVER, ON_HOVER, ALWAYS).build();


    public static void update(PGraphics pg) {
        gui.pushFolder("context lines");
        int showContextLinesMode = contextLinesOptions.indexOf(
                gui.radio("visibility", contextLinesOptions, ON_HOVER));
        boolean shouldPickShortestLine = gui.toggle("shortest line");
        pg.pushStyle();
        int clr = gui.colorPicker("color", normColor(0.5f)).hex;
        pg.stroke(clr);
        pg.fill(clr);
        pg.strokeCap(PConstants.SQUARE);
        pg.strokeWeight(gui.slider("weight", 1.2f));
        float endpointRectSize = gui.slider("end size", 3.5f);
        gui.popFolder();

        List<AbstractNode> allNodes = NodeTree.getAllNodesAsList();
        if (showContextLinesMode == SHOW_CONTEXT_LINES_MODE_NEVER) {
            return;
        }
        for (AbstractNode node : allNodes) {
            if (node.type != NodeType.FOLDER) {
                continue;
            }
            FolderNode folderNode = (FolderNode) node;
            if (folderNode.window == null || folderNode.window.closed) {
                continue;
            }
            boolean shouldShowLineFromTitleTowardsInlineNode = showContextLinesMode == SHOW_CONTEXT_LINES_ALWAYS ||
                    (folderNode.window.isTitleHighlighted() && showContextLinesMode == SHOW_CONTEXT_LINES_MODE_ON_HOVER);
            if (shouldShowLineFromTitleTowardsInlineNode) {
                folderNode.window.drawContextLineFromTitleBarToInlineNode(pg, endpointRectSize, shouldPickShortestLine);
            }
        }
        pg.popStyle();
    }
}
