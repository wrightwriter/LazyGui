import processing.core.PApplet;
import processing.core.PGraphics;
import processing.opengl.PShader;
import toolbox.Gui;
import toolbox.ShaderReloader;

public class MainTest extends PApplet {
    Gui gui;
    PGraphics pg;
    float shaderTime;

    public static void main(String[] args) {
        PApplet.main(java.lang.invoke.MethodHandles.lookup().lookupClass());
    }

    public void settings() {
        size(1000, 1000, P2D);
//        fullScreen(P2D);
    }

    public void setup() {
        gui = new Gui(this);
        pg = createGraphics(width, height, P2D);
    }

    @SuppressWarnings("DuplicatedCode")
    public void draw() {
        pg.beginDraw();
        pg.noStroke();
        pg.image(gui.gradient("bg"), 0, 0);
        pg.image(gui.imagePicker("image", ""), 0, 0);
        if (gui.toggle("shader/show")) {
            shaderTime += radians(gui.slider("shader/time"));
            String[] shaderPath = new String[]{"testFrag.glsl", "testVert.glsl"};
            PShader shader = ShaderReloader.getShader(shaderPath[0], shaderPath[1]);
            shader.set("time", shaderTime);
            shader.set("alpha", gui.slider("shader/alpha", 0.5f, 0, 1));
            ShaderReloader.filter(shaderPath[0], shaderPath[1], pg);
        }
        pg.fill(gui.colorPicker("text/fill").hex);
        pg.textAlign(CENTER);
        pg.textSize(gui.slider("text/size", 36));
        pg.text(gui.selectString("text/content", "hello", "world", "apples", "oranges", "123456789", "1234567890","12345678901"), width / 2f, height / 2f);
        pg.endDraw();
        clear();
        image(pg, 0, 0);
        gui.draw();
    }
}
