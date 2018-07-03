import com.github.rjeschke.txtmark.Run;
import processing.core.PConstants;
import processing.core.PVector;
import sun.plugin2.util.SystemUtil;

import java.util.ArrayList;

/**
 * Created by Aleksander on 12:50, 02/07/2018.
 */
public class Simulation {

    private float[][][] u, v;
    private float[][] p;
    private float[][] speeds;
    private boolean[][] bounds;
    private int arrIndex = 0;
    private int numElementsX, numElementsY;
    private  float deltaX, deltaY, deltaT;
    private  float deltaX2, deltaY2;

    private float rho;
    private float nu;

    private int frame = 0;

    private float maxSpeed, maxP, minP;

    private int width, height;

    enum DisplayMode {
        SPEED,
        PRESSURE
    }

    private DisplayMode displayMode = DisplayMode.SPEED;

    public Simulation(float deltaX, float deltaY, float deltaT, final int numElementsX, final int numElementsY) {
        frame = 0;
        rho = 1;
        nu = 8.9e-4f;   //Pa * s;

        this.deltaX = deltaX;
        this.deltaX2 = deltaX * deltaX;

        this.deltaY = deltaY;
        this.deltaY2 = deltaY * deltaY;

        this.deltaT = deltaT;

        this.numElementsX = numElementsX;
        this.numElementsY = numElementsY;

        System.out.println("numElementsX = " + numElementsX);
        System.out.println("numElementsY = " + numElementsY);


        u = new float[2][numElementsX][numElementsY];
        v = new float[2][numElementsX][numElementsY];
        p = new float[numElementsX][numElementsY];
        speeds = new float[numElementsX][numElementsY];

        bounds = new boolean[numElementsX][numElementsY];

        for (int i = 0; i < numElementsX; i++) {
            for (int j = 0; j < numElementsY; j++) {
                bounds[i][j] = false;
            }
        }



        int rad = 2;
        for (int i = -rad; i <= rad; i++) {
            for (int j = -rad; j <= rad; j++) {
                if (Math.hypot(i, j) <= rad + 0.5f)
                    bounds[i + numElementsX/2][j + numElementsY/2] = false;
            }
        }
        for (int i = 0; i < 3; i++) {
            bounds[i + numElementsX/2][numElementsY/2] = false;
        }

        for (int i = 0; i < numElementsX; i++) {
            bounds[i][0] = true;
            bounds[i][numElementsY - 1] = true;
        }


    }

    public float getMax() {
        switch (displayMode) {
            case SPEED:
                return maxSpeed;
            case PRESSURE:
                return maxP;
        }
        return 0;
    }

    public float getMin() {
        switch (displayMode) {
            case PRESSURE:
                return minP;
            case SPEED:
                return 0;
        }
        return 0;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public float getTime() {
        return frame * deltaT;
    }

    public void render(Main main) {

        float scale = Math.min(width / numElementsX, height / numElementsY);

        int xOffset = (int) (width - scale * numElementsX);
        int yOffset = (int) (height - scale * numElementsY);

        main.pushMatrix();
        main.translate(0, (yOffset) /2);

        main.fill(255);
        main.stroke(0, 40);
        main.colorMode(PConstants.HSB);
        for (int x = 0; x < numElementsX; x++) {
            for (int y = 0; y < numElementsY; y++) {
                switch (displayMode) {
                    case SPEED:
                        main.fill(85 * (speeds[x][numElementsY - y - 1]/maxSpeed) + 170, 150, 250);
                        break;
                    case PRESSURE:
                        main.fill(getPressureColour(x, y), 150, 250);
                        break;
                }
                main.rect(scale * x, scale * y, scale, scale);
            }
        }
        main.colorMode(PConstants.RGB);

        main.pushMatrix();

        main.translate(scale/2, scale/2);

        main.fill(0);
        main.stroke(0);


        for (int x = 0; x < numElementsX; x++) {
            for (int y = 0; y < numElementsY; y++) {

                float norm = maxSpeed * 2f;

                if (norm < Float.MIN_VALUE) {
                    continue;
                }
                main.line(scale * x, scale * y, scale * (x + u[arrIndex][x][numElementsY - y - 1] / norm), scale * (y - v[arrIndex][x][numElementsY - y - 1] / norm));

            }
        }

        main.popMatrix();

        for (int x = 0; x < numElementsX; x++) {
            for (int y = 0; y < numElementsY; y++) {
                if (bounds[x][numElementsY - y - 1]) {
                    main.rect(scale * x, scale * y, scale, scale);
                }
            }
        }



        main.noFill();

        main.rect(0, 0, numElementsX * scale, numElementsY * scale);
        main.popMatrix();
        main.rect(0, 0, width, height);

    }

    private float getPressureColour(int x, int y) {
        float val = (p[x][numElementsY - y - 1] - minP) / (maxP - minP);
        return 85 * val + 170;
    }

    public void toggleMode() {
        for (int i = 0; i < DisplayMode.values().length; i++) {
            if (DisplayMode.values()[i] == displayMode) {
                displayMode = DisplayMode.values()[(i + 1) % DisplayMode.values().length];
                return;
            }
        }
    }

    //All of the CFD code nobody cares about

    public void update() {

        long startTime = System.currentTimeMillis();


        maxP = -Float.MAX_VALUE;
        minP = Float.MAX_VALUE;

        maxSpeed = 0;

        for (int x = 0; x < numElementsX; x++) {
            for (int y = 0; y < numElementsY; y++) {

                if (bounds[x][y])
                    continue;

                for (int i = 0; i < 10; i++) {
                    p[x][y] = updateP(x, y);
                }

                float newU = updateU(x, y);
                float newV = updateV(x, y);
                u[1 - arrIndex][x][y] = newU;
                v[1 - arrIndex][x][y] = newV;

                speeds[x][y] = (float) Math.hypot(newU, newV);
                if (speeds[x][y] > maxSpeed)
                    maxSpeed = speeds[x][y];

            }
        }

        for (int x = 0; x < numElementsX; x++) {
            for (int y = 0; y < numElementsY; y++) {
                if (p[x][y] > maxP)
                    maxP = p[x][y];
                if (p[x][y] < minP)
                    minP = p[x][y];

                if (speeds[x][y] > maxSpeed)
                    maxSpeed = speeds[x][y];
            }
        }

        frame++;
        arrIndex = frame % 2;

        System.out.printf("\tTook %d ms to render\n", System.currentTimeMillis() - startTime);

    }

    private float updateU(int x, int y) {
        return getU(x, y) - deltaT * (getU(x,y) * getdUdX(x, y) + getV(x, y) * getdUdY(x, y) + 1/rho * getdPdX(x, y) - nu * getGradU(x, y)) + deltaT * 1f;
    }

    private float updateV(int x, int y) {
        return getV(x, y) - deltaT * (getV(x, y) * getdVdY(x, y) + getU(x, y) * getdVdX(x, y) + 1/rho * getdPdY(x, y) - nu * getGradV(x, y));
    }

    private float updateP(int x, int y) {

        float val = 1 / deltaT;
        val *= (getU(x + 1, y) - getU(x - 1, y))/ (2 * deltaX) + (getV(x, y + 1) - getV(x, y - 1))/(2 * deltaY);
        float du = getdUdX(x, y);
        val += - du * du;
        du = getdUdY(x, y) * getdVdX(x, y);
        val += -2 * du;
        du = getdVdY(x, y);
        val += - du * du;

        val +=  (getP(x + 1, y) - getP(x - 1, y)) * deltaY2 + (getP(x, y + 1) - getP(x, y - 1)) * deltaX2;
        val *= -  (rho * deltaX2 * deltaY2);
        return val / (2 * (deltaX2 + deltaY2));

    }

    private float getdVdX(int x, int y) {
        if (getBounds(x + 1, y))
            return getV(x, y) - getV(x - 1, y)/deltaX;
        if (getBounds(x - 1, y))
            return getV(x + 1, y) - getV(x, y)/deltaX;
        return (getV(x + 1, y) - getV(x - 1, y)) / (2 * deltaX);
    }

    private float getdVdY(int x, int y) {        
        if (getBounds(x, y + 1))
        return getV(x, y) - getV(x, y - 1)/deltaY;
        if (getBounds(x, y - 1))
            return getV(x, y + 1) - getV(x, y) / deltaY;
        return (getV(x, y + 1) - getV(x, y - 1)) / (2 * deltaY);
    }

    private float getdUdX(int x, int y) {
        if (getBounds(x + 1, y))
        return getU(x, y) - getU(x - 1, y)/deltaX;
        if (getBounds(x - 1, y))
            return getU(x + 1, y) - getU(x, y)/deltaX;
        return (getU(x + 1, y) - getU(x - 1, y)) / (2 * deltaX);
    }

    private float getdUdY(int x, int y) {
        if (getBounds(x, y + 1))
            return getU(x, y) - getU(x, y - 1)/deltaY;
        if (getBounds(x, y - 1))
            return getU(x, y + 1) - getU(x, y) / deltaY;
        return (getU(x, y + 1) - getU(x, y - 1)) / (2 * deltaY);
    }

    private float getGradU(int x, int y) {
        return    (getU(x + 1, y    ) + getU(x - 1, y    ) - 2 * getU(x ,y)) / deltaX2
                + (getU(x    , y + 1) + getU(x    , y - 1) - 2 * getU(x ,y)) / deltaY2;
    }
    
    private float getGradV(int x, int y) {
        return    (getV(x + 1, y    ) + getV(x - 1, y    ) - 2 * getV(x, y)) / deltaX2
                + (getV(x    , y + 1) + getV(x    , y - 1) - 2 * getV(x, y)) / deltaY2;
    }

    private float getdPdX(int x, int y) {

        if (getBounds(x + 1, y))
                return getP(x, y) - getP(x - 1, y)/deltaX;
        if (getBounds(x - 1, y)) {
            return getP(x + 1, y) - getP(x, y)/deltaX;
        }
        return (getP(x + 1, y) - getP(x - 1, y)) / (2 * deltaX);
    }

    private float getdPdY(int x, int y) {
        if (y == 0 || y == numElementsY - 1)
            return 0;

        if (getBounds(x, y + 1))
            return getP(x, y) - getP(x, y - 1)/deltaY;
        if (getBounds(x, y - 1))
            return getP(x, y + 1) - getP(x, y) / deltaY;
        return (getP(x, y + 1) - getP(x, y - 1)) / (2 * deltaY);
    }

    private float getU(int x, int y) {

        x = (x + numElementsX) % numElementsX;
/*
        if (y == 0)
            return 0;
        if (y == numElementsY - 1)
            return 0;*/

        if (x >= numElementsX)
            return 0;
        if (x < 0)
            return 0;
        if (y >= numElementsY)
            return 0;
        if (y < 0)
            return 0;

        if (getBounds(x, y)) {
            if ((x >= numElementsX || getBounds(x + 1, y)) || (x < 0 || getBounds(x - 1, y)))
                return 0;
        }

        return u[arrIndex][x][y];
    }

    private float getV(int x, int y) {

        if (y == 0 || y == numElementsY - 1)
            return 0;

        x = (x + numElementsX) % numElementsX;

        if (x >= numElementsX)
            return 0;
        if (x < 0)
            return 0;
        if (y >= numElementsY)
            return 0;
        if (y < 0)
            return 0;

        if (getBounds(x, y)) {
            if ((y >= numElementsY || getBounds(x, y + 1)) || (y < 0 || getBounds(x, y - 1)))
                return 0;
        }

        return v[arrIndex][x][y];
    }

    private float getP(int x, int y) {

        if (y == 0 || y == numElementsY - 1)
            return 0;

        x = (x + numElementsX) % numElementsX;

        if (x >= numElementsX)
            return 0;
        if (x < 0)
            return 0;
        if (y >= numElementsY)
            return 0;
        if (y < 0)
            return 0;

        if (getBounds(x, y)) {
            if ((x >= numElementsX || getBounds(x + 1, y)) && (x < 0 || getBounds(x - 1, y)))
                return 0;
        }
        if (getBounds(x, y)) {
            if ((y >= numElementsY || getBounds(x, y + 1)) && (y < 0 || getBounds(x, y - 1)))
                return 0;
        }

        return p[x][y];
    }

    private boolean getBounds(int x, int y) {

        if (x >= numElementsX)
            return false;
        if (x < 0)
            return false;
        if (y >= numElementsY)
            return true;
        if (y < 0)
            return true;
        
        return bounds[x][y];
    }


}
