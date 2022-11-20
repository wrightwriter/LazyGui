package lazy;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.annotations.Expose;


import processing.core.PGraphics;
import processing.core.PVector;
import processing.opengl.PShader;

import java.util.ArrayList;
import java.util.HashMap;

import static processing.core.PApplet.*;

class SliderNode extends AbstractNode {

    ArrayList<Character> numpadChars = new Utils.ArrayListBuilder<Character>()
            .add('0','1','2','3','4','5','6','7','8','9')
            .build();
    private int numpadInputAppendLastFrame = -1;
    protected float numpadBufferValue = 0;
    protected boolean showPercentIndicatorWhenConstrained = true;
    private int numberInputIndexAfterFloatingPoint = -1;

    SliderNode(String path, FolderNode parentFolder, float defaultValue, float min, float max, float defaultPrecision, boolean constrained) {
        super(NodeType.VALUE, path, parentFolder);
        valueFloatDefault = defaultValue;
        if (!Float.isNaN(defaultValue)) {
            valueFloat = defaultValue;
        }
        valueFloatMin = min;
        valueFloatMax = max;
        valueFloatPrecision = defaultPrecision;
        valueFloatPrecisionDefault = defaultPrecision;
        valueFloatConstrained = constrained &&
                max != Float.MAX_VALUE && max != Integer.MAX_VALUE &&
                min != -Float.MAX_VALUE && min != -Integer.MAX_VALUE;
        initSliderPrecisionArrays();
        State.overwriteWithLoadedStateIfAny(this);
    }

    @Expose
    float valueFloat;
    @Expose
    int currentPrecisionIndex;
    @Expose
    float valueFloatPrecision;
    float valueFloatDefault;
    float valueFloatMin;
    float valueFloatMax;
    boolean valueFloatConstrained;
    float valueFloatPrecisionDefault;
    float backgroundScrollX = 0;


    PVector mouseDelta = new PVector();
    protected ArrayList<Float> precisionRange = new ArrayList<>();
    HashMap<Float, Integer> precisionRangeDigitsAfterDot = new HashMap<>();


    String shaderPath = "sliderBackground.glsl";

    private void initSliderPrecisionArrays() {
        initPrecision();
        loadArbitraryPrecisionIntoArrays();
    }

    void initSliderBackgroundShader() {
        InternalShaderStore.getShader(shaderPath);
    }

    private void initPrecision() {
        precisionRange.add(0.00001f);
        precisionRange.add(0.0001f);
        precisionRange.add(0.001f);
        precisionRange.add(0.01f);
        precisionRange.add(0.1f);
        precisionRange.add(1.0f);
        precisionRange.add(10.0f);
        precisionRange.add(100.0f);
        setPrecisionIndexAndValue(4);
        precisionRangeDigitsAfterDot.put(0.00001f, 5);
        precisionRangeDigitsAfterDot.put(0.0001f, 4);
        precisionRangeDigitsAfterDot.put(0.001f, 3);
        precisionRangeDigitsAfterDot.put(0.01f, 2);
        precisionRangeDigitsAfterDot.put(0.1f, 1);
        precisionRangeDigitsAfterDot.put(1f, 0);
        precisionRangeDigitsAfterDot.put(10f, 0);
        precisionRangeDigitsAfterDot.put(100f, 0);
    }

    private void loadArbitraryPrecisionIntoArrays() {
        float p = valueFloatPrecision;
        for (int i = 0; i < precisionRange.size() - 1; i++) {
            float thisValue = precisionRange.get(i);
            float nextValue = precisionRange.get(i + 1);
            if (thisValue == p) {
                setPrecisionIndexAndValue(i);
                return;
            } else if (thisValue < p && nextValue > p) {
                setPrecisionIndexAndValue(i + 1);
                precisionRange.add(i + 1, p);
                precisionRangeDigitsAfterDot.put(p, String.valueOf(p).length() - 2);
            }
        }
    }

    @Override
    protected void updateDrawInlineNodeAbstract(PGraphics pg) {
        updateDrawSliderNodeValue(pg);
    }

    void updateDrawSliderNodeValue(PGraphics pg) {
        if(numpadInputJustFinished()){
            setValueFloat(numpadBufferValue);
        }
        if (isDragged || isMouseOverNode) {
            updateValueMouseInteraction();
            boolean constrainedThisFrame = tryConstrainValue();
            drawBackgroundScroller(pg, constrainedThisFrame);
            mouseDelta.x = 0;
            mouseDelta.y = 0;
        }
        fillForegroundBasedOnMouseOver(pg);
        drawRightText(pg, getValueToDisplay() + (isNumpadInputActive() ? "_" : ""));
    }

    private void drawBackgroundScroller(PGraphics pg, boolean constrainedThisFrame) {
        if (!constrainedThisFrame) {
            backgroundScrollX -= mouseDelta.x;
        }
        float widthMult = 1f;
        if (valueFloatConstrained && showPercentIndicatorWhenConstrained) {
            widthMult = constrain(norm(valueFloat, valueFloatMin, valueFloatMax), 0, 1);
            backgroundScrollX = 0;
        }
        updateBackgroundShader(pg);
        pg.fill(ThemeStore.getColor(ThemeColorType.NORMAL_BACKGROUND));
        pg.noStroke();
        pg.rect(1, 0, (size.x - 1) * widthMult, size.y);
        pg.resetShader();
    }

    protected void updateBackgroundShader(PGraphics pg) {
        PShader shader = InternalShaderStore.getShader(shaderPath);
        shader.set("scrollX", backgroundScrollX);
        shader.set("quadPos", pos.x, pos.y);
        shader.set("quadSize", size.x, size.y);
        shader.set("precisionNormalized", norm(currentPrecisionIndex, 0, precisionRange.size()));
        InternalShaderStore.shader(shaderPath, pg);
    }

    String getValueToDisplay() {
        int fractionPadding = precisionRangeDigitsAfterDot.get(valueFloatPrecision);
        float value = isNumpadInputActive() ? numpadBufferValue : valueFloat;
        if (fractionPadding == 0) {
            String wholeNumber = String.valueOf(floor(value));
            if(numberInputIndexAfterFloatingPoint == 0){
                wholeNumber += ".";
            }
            return wholeNumber;
        }
        if (Float.isNaN(value)) {
            return "NaN";
        }
        return nf(value, 0, fractionPadding).replaceAll(",", ".");
    }

    @Override
    void mouseWheelMovedOverNode(float x, float y, int dir) {
        super.mouseWheelMovedOverNode(x, y, dir);
        if (dir > 0) {
            increasePrecision();
        } else if (dir < 0) {
            decreasePrecision();
        }
    }

    private void setWholeNumberPrecision(){
        for (int i = 0; i < precisionRange.size(); i++) {
            if(precisionRange.get(i) >= 1f){
                setPrecisionIndexAndValue(i);
                break;
            }
        }
    }

    private void decreasePrecision() {
        setPrecisionIndexAndValue(min(currentPrecisionIndex + 1, precisionRange.size() - 1));
    }

    private void increasePrecision() {
        setPrecisionIndexAndValue(max(currentPrecisionIndex - 1, 0));
    }

    protected void setPrecisionIndexAndValue(int newPrecisionIndex) {
        currentPrecisionIndex = newPrecisionIndex;
        valueFloatPrecision = precisionRange.get(currentPrecisionIndex);
        validatePrecision();
    }

    private void updateValueMouseInteraction() {
        if(mouseDelta.x != 0){
            float delta = mouseDelta.x * precisionRange.get(currentPrecisionIndex);
            setValueFloat(valueFloat - delta);
        }
    }

    protected boolean tryConstrainValue() {
        boolean constrained = false;
        if (valueFloatConstrained) {
            if (valueFloat > valueFloatMax || valueFloat < valueFloatMin) {
                constrained = true;
            }
            valueFloat = constrain(valueFloat, valueFloatMin, valueFloatMax);
        }
        return constrained;
    }

    @Override
    void keyPressedOverNode(LazyKeyEvent e, float x, float y) {
        super.keyPressedOverNode(e, x, y);
        if (e.getKeyChar() == 'r') {
            if (Float.isNaN(valueFloatDefault)) {
                setValueFloat(valueFloatDefault);
            }
            valueFloatPrecision = valueFloatPrecisionDefault;
            currentPrecisionIndex = precisionRange.indexOf(valueFloatPrecision);
        }
        tryReadNumpadInput(e);
        if (e.getKeyCode() == KeyCodes.CTRL_C) {
            String value = getValueToDisplay();
            if(value.endsWith(".")){
                value += "0";
            }
            Utils.setClipboardString(value);
        }
        if (e.getKeyCode() == KeyCodes.CTRL_V) {
            String clipboardString = Utils.getClipboardString();
            try {
                float clipboardValue = Float.parseFloat(clipboardString);
                if (!Float.isNaN(clipboardValue)) {
                    setValueFloat(clipboardValue);
                } else {
                    println("Could not parse float from this clipboard string: " + clipboardString);
                }
            } catch (NumberFormatException nfe) {
                println("Could not parse float from this clipboard string: " + clipboardString);
            }
        }
    }

    private void tryReadNumpadInput(LazyKeyEvent e) {
        if(numpadChars.contains(e.getKeyChar())){
            tryAppendNumberInputToValue(Integer.valueOf(String.valueOf(e.getKeyChar())));
        }
        switch (e.getKeyChar()) {
            case '.':
            case ',':
                numberInputIndexAfterFloatingPoint = 0;
                numpadInputAppendLastFrame = State.app.frameCount;
                break;
            case '+':
                if(valueFloat <= 0){
                    valueFloat = abs(valueFloat);
                }
                break;
            case '-':
                if(valueFloat >= 0){
                    valueFloat = -valueFloat;
                }
                break;
            case '*':
                decreasePrecision();
                break;
            case '/':
                increasePrecision();
                break;
            default: {
                return;
            }
        }
        State.onUndoableActionEnded();
        onValueFloatChanged();
    }

    private void tryAppendNumberInputToValue(Integer input) {
        boolean replaceMode = numpadInputAppendLastFrame == -1 ||
                State.app.frameCount - numpadInputAppendLastFrame > State.keyboardInputAppendCooldown;
        numpadInputAppendLastFrame = State.app.frameCount;
        if (replaceMode) {
            numberInputIndexAfterFloatingPoint = -1;
            numpadBufferValue = input;
            // set the precision for it to be ready for increasing precision when appending fractions
            setWholeNumberPrecision();
            return;
        }
        int valueFloored = floor(numpadBufferValue);
        if (numberInputIndexAfterFloatingPoint == -1) {
            // append whole number (floating point can represent only 7 whole digits)
            // see https://en.wikipedia.org/wiki/Single-precision_floating-point_format
            if(String.valueOf(valueFloored).length() <= 7){
                numpadBufferValue = valueFloored * 10 + input;
            }
        } else {
            // append fraction only
            float fractionToAdd = input * (pow(0.1f, ++numberInputIndexAfterFloatingPoint));
            numpadBufferValue = numpadBufferValue + fractionToAdd;
            // adjust precision to show the new number

            if(numberInputIndexAfterFloatingPoint > nf(fractionToAdd,0,0).length() - 3){
                increasePrecision();
            }
        }

    }

    protected boolean isNumpadInputActive() {
        return numpadInputAppendLastFrame != -1 &&
                State.app.frameCount <= numpadInputAppendLastFrame + State.keyboardInputAppendCooldown;
    }

    protected boolean numpadInputJustFinished(){
        return numpadInputAppendLastFrame != -1 &&
                State.app.frameCount == numpadInputAppendLastFrame + State.keyboardInputAppendCooldown;
    }

    protected void setValueFloat(float floatToSet) {
        valueFloat = floatToSet;
        onValueFloatChanged();
    }

    protected void onValueFloatChanged() {
        tryConstrainValue();
    }

    @Override
    void mouseDragNodeContinue(LazyMouseEvent e) {
        super.mouseDragNodeContinue(e);
        if (isDragged) {
            mouseDelta.x = e.getPrevX() - e.getX();
            mouseDelta.y = e.getPrevY() - e.getY();
            e.setConsumed(true);
        }
    }

    @Override
    void overwriteState(JsonElement loadedNode) {
        JsonObject json = loadedNode.getAsJsonObject();
        if (json.has("currentPrecisionIndex")) {
            currentPrecisionIndex = json.get("currentPrecisionIndex").getAsInt();
        }
        if (json.has("valueFloatPrecision")) {
            valueFloatPrecision = json.get("valueFloatPrecision").getAsFloat();
            valueFloatPrecisionDefault = json.get("valueFloatPrecision").getAsFloat();
        }
        if (json.has("valueFloat")) {
            setValueFloat(json.get("valueFloat").getAsFloat());
        }
    }

    @Override
    String getConsolePrintableValue() {
        return getValueToDisplay();
    }

}
