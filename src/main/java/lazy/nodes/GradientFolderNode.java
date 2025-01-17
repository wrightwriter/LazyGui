package lazy.nodes;

import com.google.gson.JsonElement;
import lazy.PickerColor;
import lazy.stores.ShaderStore;
import lazy.utils.ArrayListBuilder;
import lazy.stores.JsonSaveStore;
import processing.core.PGraphics;
import processing.opengl.PShader;

import java.util.ArrayList;

import static lazy.stores.NormColorStore.color;
import static lazy.stores.GlobalReferences.app;
import static lazy.stores.LayoutStore.cell;
import static processing.core.PApplet.*;

public class GradientFolderNode extends FolderNode {
    PGraphics out;
    final RadioFolderNode directionTypePicker;
    final RadioFolderNode blendTypePicker;
    final ArrayList<String> blendTypeOptions = new ArrayListBuilder<String>().add("mix").add("rgb").add("hsv").build();
    final ArrayList<String> directionOptions = new ArrayListBuilder<String>().add("x").add("y").add("center").build();

    final String gradientShader = "gradient.glsl";
    private final int colorCount;
    private int lastUpdatedFrame = -1;

    public GradientFolderNode(String path, FolderNode parent, float alpha) {
        super(path, parent);
        directionTypePicker = new RadioFolderNode(path + "/direction", this, directionOptions.toArray(new String[0]), directionOptions.get(1));
        blendTypePicker = new RadioFolderNode(path + "/blend type", this, blendTypeOptions.toArray(new String[0]), blendTypeOptions.get(0));
        children.add(new GradientPreviewNode(path + "/preview", this));
        children.add(directionTypePicker);
        children.add(blendTypePicker);
        colorCount = 5;
        idealWindowWidthInCells = 9;
        for (int i = 0; i < colorCount; i++) {
            float iNorm = norm(i, 0, colorCount - 1);
            // default A alpha is 1 for some reason even though I set 0 here
            children.add(createGradientColorPicker(path + "/" + getColorNameByIndex(i), iNorm, alpha, iNorm, i % 2 == 0));
        }
        JsonSaveStore.overwriteWithLoadedStateIfAny(this);
    }

    @Override
    public void updateValuesRegardlessOfParentWindowOpenness() {
        updateOutGraphics();
    }

    @Override
    protected void drawNodeForeground(PGraphics pg, String name) {
        drawLeftText(pg, name);
        drawGradientPreviewIcon(pg);
    }

    void drawGradientPreviewIcon(PGraphics pg){
        pg.translate(size.x - cell * 0.5f, cell * 0.5f);
        pg.imageMode(CENTER);
        float previewRectSize = cell * 0.6f;
        pg.image(out, 0, 0, previewRectSize, previewRectSize);
        strokeForegroundBasedOnMouseOver(pg);
        pg.rectMode(CENTER);
        pg.noFill();
        pg.rect(0, 0, previewRectSize, previewRectSize);
    }

    String getColorNameByIndex(int index) {
        String a = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        return String.valueOf(a.charAt(index));
    }

    private void updateOutGraphics() {
        if(lastUpdatedFrame == app.frameCount){
            return; // weird bugs when updated more than once per frame
        }
        lastUpdatedFrame = app.frameCount;
        if (out == null || out.width != app.width || out.height != app.height) {
            out = app.createGraphics(app.width, app.height, P2D);
        }

        children.sort((o1, o2) -> {
            assert o1 != null;
            assert o2 != null;
            if (o1.className.equals(GradientColorNode.class.getSimpleName()) && o2.className.equals(GradientColorNode.class.getSimpleName())) {
                return Float.compare(((GradientColorNode) o1).getGradientPos(), ((GradientColorNode) o2).getGradientPos());
            }
            return 0;
        });
        PShader shader = ShaderStore.getShader(gradientShader);
        int activeColorCount = getColorCount();
        shader.set("colorCount", activeColorCount);
        shader.set("colorValues", getColorValues(activeColorCount), 4);
        shader.set("colorPositions", getColorPositions(activeColorCount), 1);
        shader.set("directionType", directionOptions.indexOf(directionTypePicker.valueString));
        shader.set("blendType", blendTypeOptions.indexOf(blendTypePicker.valueString));
        out.beginDraw();
        out.clear();
        out.filter(shader);
        out.endDraw();
    }

    private int getColorCount() {
        int activeColorCount = colorCount;
        ArrayList<GradientColorNode> colorPickers = getAllGradientColorPickerChildrenInPositionOrder();
        for (GradientColorNode colorPicker : colorPickers) {
            if (colorPicker.isSkipped()) {
                activeColorCount--;
            }
        }
        return activeColorCount;
    }

    private float[] getColorValues(int activeCount) {
        ArrayList<GradientColorNode> colorPickers = getAllGradientColorPickerChildrenInPositionOrder();
        float[] result = new float[activeCount * 4];
        int i = 0;
        for (GradientColorNode colorPicker : colorPickers) {
            if (colorPicker.isSkipped()) {
                continue;
            }
            PickerColor color = colorPicker.getColor();
            result[i] = color.hue;
            result[i + 1] = color.saturation;
            result[i + 2] = color.brightness;
            result[i + 3] = color.alpha;
            i += 4;
        }
        return result;
    }

    private float[] getColorPositions(int activeCount) {
        ArrayList<GradientColorNode> colorPickers = getAllGradientColorPickerChildrenInPositionOrder();
        float[] result = new float[activeCount];
        int i = 0;
        for (GradientColorNode colorPicker : colorPickers) {
            if (colorPicker.isSkipped()) {
                continue;
            }
            result[i++] = colorPicker.getGradientPos();
        }
        return result;
    }

    public PGraphics getOutputGraphics() {
        updateOutGraphics();
        return out;
    }

    ArrayList<GradientColorNode> getAllGradientColorPickerChildrenInPositionOrder() {
        ArrayList<GradientColorNode> result = new ArrayList<>();
        for (AbstractNode node : children) {
            if (node.className.contains(GradientColorNode.class.getSimpleName())) {
                result.add((GradientColorNode) node);
            }
        }
        return result;
    }

    public void overwriteState(JsonElement loadedNode) {
        super.overwriteState(loadedNode);
    }

    GradientColorNode createGradientColorPicker(String path, float brightnessNorm, float alphaNorm,
                                                float pos, boolean active) {
        int hex = color(0, 0, brightnessNorm, alphaNorm);
        return new GradientColorNode(path, this, hex, pos, active);
    }

    private static class GradientPreviewNode extends AbstractNode {

        final GradientFolderNode parent;

        GradientPreviewNode(String path, GradientFolderNode parent) {
            super(NodeType.VALUE, path, parent);
            this.parent = parent;
            masterInlineNodeHeightInCells = 6;
        }

        @Override
        protected void drawNodeBackground(PGraphics pg) {
            pg.image(parent.getOutputGraphics(), 1, 1, size.x-1, size.y-1);
        }

        @Override
        protected void drawNodeForeground(PGraphics pg, String name) {

        }

    }

}
