import processing.core.PApplet;
import processing.event.KeyEvent;

/**
 * Created by Aleksander on 12:49, 02/07/2018.
 */
public class Main extends PApplet {


    private boolean up = false, render = true;

    public static void main(String[] args) {
        PApplet.main("Main");
    }

    @Override
    public void settings() {
        size(1000, 800, P2D);
    }

    Simulation s;
    @Override
    public void setup() {
        s = new Simulation(0.05f, 0.05f, 0.05f, 40, 40);
        s.setWidth(this.width - 200);
        s.setHeight(this.height);
        up = false;

    }

    @Override
    public void draw() {
        if (up) {
            int frames = 10;
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < frames; i++) {
                s.update();
            }
            System.out.printf("Took %d ms to render %d frames\n", System.currentTimeMillis() - startTime, frames);
            render = true;
        }
        if (render) {
            background(255);
            s.render(this);

            translate(800, 0);

            fill(0);

            beginShape();
            {
                fill(255, 0, 0);
                vertex(30, 30);
                vertex(100, 30);
                fill(0, 0, 255);
                vertex(100, 770);
                vertex(30, 770);
            }
            endShape(CLOSE);

            text(s.getMax(), 100, 30);
            text(s.getMin(), 100, 770);

            text(s.getTime(), 0, 20);

            render = false;

        }

    }

    @Override
    public void keyPressed(KeyEvent event) {
        switch (event.getKeyCode()) {
            case 10:
                up = !up;
                render = true;
                break;
            case java.awt.event.KeyEvent.VK_SPACE:
                s.update();
                render = true;
                break;
            case java.awt.event.KeyEvent.VK_A:
                s.toggleMode();
                render = true;
                break;
        }
    }
}
