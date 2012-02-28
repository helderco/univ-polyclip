/**
 * Efficient Clipping of Arbitrary Polygons using OpenGPL
 * Copyright (c) 2011, 2012 Helder Correia <helder.mc@gmail.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package compgraphics;

import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Arrays;
import java.util.List;
import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;

public class PolygonClip implements GLEventListener {

    // default Subject polygon - blue
    float[][] spoly = {
        {1.50f, 1.25f},
        {7.50f, 2.50f},
        {4.00f, 3.00f},
        {4.50f, 6.50f},
    };

    // default Clip polygon - red
    float[][] cpoly = {
        {5.00f, 4.50f},
        {3.00f, 5.50f},
        {1.00f, 4.00f},
        {1.50f, 3.50f},
        {0.00f, 2.00f},
        {3.00f, 2.25f},
        {2.50f, 1.00f},
        {5.50f, 0.00f},
    };

    // options set from command line
    private boolean opt_wireframe = false;
    private boolean opt_original = false;
    private boolean opt_clipper = false;
    private boolean opt_subject = false;
    private boolean opt_debug = false;
    private char opt_operation = '\\';

    public PolygonClip(String[] args) {
        for (String option : Arrays.asList(args)) {
            if (option.equals("-w") || option.equals("--wireframe")) {
                opt_wireframe = true;
            }
            if (option.equals("-o") || option.equals("--show-original")) {
                opt_original = true;
            }
            if (option.equals("-c") || option.equals("--show-clipper")) {
                opt_clipper = true;
            }
            if (option.equals("-s") || option.equals("--show-subject")) {
                opt_subject = true;
            }
            if (option.equals("-d") || option.equals("--debug")) {
                opt_debug = true;
            }
            if (option.equals("--difference")) {
                opt_operation = '\\';
            }
            if (option.equals("--union")) {
                opt_operation = '|';
            }
            if (option.equals("--intersection")) {
                opt_operation = '&';
            }
            if (option.equals("--reversed-diff")) {
                opt_operation = '/';
            }
            try {
                if (option.startsWith("--subj-poly=")) {
                    spoly = parsePolygon(option.substring(14));
                }
                if (option.startsWith("--clip-poly=")) {
                    cpoly = parsePolygon(option.substring(11));
                }
            } catch (Exception e) {
                System.err.format(
                    "Invalid syntax for polygon definition on option %s. " +
                    "Using default.\n", option
                );
            }
        }
    }

    /**
     * Parse a string representing a polygon into a float[][] array
     * Semi-colons (;) separate vertices, while collons (,) separate coordinates
     */
    private float[][] parsePolygon(String values) {
        String[] vertexes = values.substring(1, values.length()-1).split(";");
        float[][] poly = new float[vertexes.length][2];
        for (int i = 0; i < vertexes.length; i++) {
            String[] coord = vertexes[i].split(",", 2);
            float x = Float.parseFloat(coord[0]);
            float y = Float.parseFloat(coord[1]);
            poly[i] = new float[] {x, y};
        }
        return poly;
    }

    public static void main(String[] args) {
        System.out.println();
        System.out.println("Efficient Clipping of Arbitrary Polygons using OpenGPL");
        System.out.println();
        System.out.println("Copyright (C) 2011, 2012  Helder Correia");
        System.out.println("This program comes with ABSOLUTELY NO WARRANTY.");
        System.out.println("This is free software, and you are welcome to redistribute");
        System.out.println("it under certain conditions; see source for details.");
        System.out.println();
        System.out.println("Run with -h or --help for available options.");
        System.out.println();
        
        for (String option : Arrays.asList(args)) {
            if (option.equals("-h") || option.equals("--help")) {
                System.out.println("Usage: java -jar " + args[0] + " [options]");
                System.out.println();

                String[][] options = {
                    {"Options", ""},
                    {"-h, --help",          "show this help message and exit"},
                    {"-d, --debug",         "show debug information on screen"},
                    {"-o, --show-original", "show original polygons"},
                    {"-c, --show-clipper",  "show clipper polygon as wireframe, on top of clipped"},
                    {"-s, --show-subject",  "show subject polygon as wireframe, on top of clipped"},
                    {"-w, --wireframe",     "draw only the border lines"},
                    {"", ""},
                    {"Available Operations", ""},
                    {"  --union",         "perform the union of the two polygons: A|B"},
                    {"  --intersection",  "perform the intersection of the two polygons: A&B"},
                    {"  --difference",    "difference between the polygons: A\\B (default)"},
                    {"  --reversed-diff", "difference between the polygons: B\\A"},
                    {"", ""},
                    {"Defining Arbitrary Polygons", ""},
                    {"  This program is provided as a demo, but you can override the pre-", ""},
                    {"  defined polygons without editing the file by using these options.", ""},
                    {"  POLY needs to be a string with pairs of floats (representing the", ""},
                    {"  the x and y coordinates of the vertexes), separated by semi-colons.", ""},
                    {"", ""},
                    {"  --subj-poly=POLY", "override the vertices for the subject polygon"},
                    {"  --clip-poly=POLY", "override the vertices for the clipper polygon"},
                    {"", ""},
                    {"  POLY syntax:", "--subj-poly=\"1.5, 1.25; 7.5, 2.5; 4, 3; 4.5, 6.5\""},
                    {"", ""},
                };

                for (String[] help : Arrays.asList(options)) {
                    System.out.format("  %-21s %s\n", help[0], help[1]);
                }
                System.exit(0);
            }
        }

        Frame frame = new Frame("Polygon Clipping");
        GLCanvas canvas = new GLCanvas();

        canvas.addGLEventListener(new PolygonClip(args));
        frame.add(canvas);
        frame.setSize(300, 300);
        frame.addWindowListener(new WindowAdapter() {

            @Override
            public void windowClosing(WindowEvent e) {
                // Run this on another thread than the AWT event queue to
                // make sure the call to Animator.stop() completes before
                // exiting
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        System.exit(0);
                    }
                }).start();
            }
        });
        // Center frame
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }

    public void init(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        // Enable VSync
        gl.setSwapInterval(1);

        // Setup the drawing area and shading mode
        gl.glClearColor(0.0f, 0.0f, 0.0f, 0.0f);
        gl.glShadeModel(GL.GL_SMOOTH);
    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        GL gl = drawable.getGL();
        GLU glu = new GLU();

        if (height <= 0) { // avoid a divide by zero error!
            height = 1;
        }
        final float h = (float) width / (float) height;
        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.0f, h, 1.0, 20.0);
        gl.glMatrixMode(GL.GL_MODELVIEW);
        gl.glLoadIdentity();

        // Since we allow overriding the polygon definitions, we don't want
        // to have it drawn outside view, so we'll try centering them on screen
        float x_min = 0.0f, x_max = 0.0f;
        float y_min = 0.0f, y_max = 0.0f;

        for (int i = 0; i < spoly.length; i++) {
            if (spoly[i][0] < x_min) x_min = spoly[i][0];
            if (spoly[i][1] < y_min) y_min = spoly[i][1];
            if (spoly[i][0] > x_max) x_max = spoly[i][0];
            if (spoly[i][1] > y_max) y_max = spoly[i][1];
        }
        for (int i = 0; i < cpoly.length; i++) {
            if (cpoly[i][0] < x_min) x_min = cpoly[i][0];
            if (cpoly[i][1] < y_min) y_min = cpoly[i][1];
            if (cpoly[i][0] > x_max) x_max = cpoly[i][0];
            if (cpoly[i][1] > y_max) y_max = cpoly[i][1];
        }

        float x_range = x_max - x_min;
        float y_range = y_max - y_min;

        gl.glTranslatef(-x_max/2, -y_max/2, -(1.5f*x_range+1.5f*y_range)/2);
    }

    public void display(GLAutoDrawable drawable) {
        GL gl = drawable.getGL();

        // Clear the drawing area
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        if (!opt_original) {
            gl.glColor3f(0.5f, 0.5f, 1.0f);
            clip_polygon(gl, spoly, cpoly);
        }

        // We can see the original polygons on top of the clipped ones,
        // by passing the appropriate command-line options
        if (opt_original || opt_clipper || opt_subject) {
            opt_wireframe |= opt_clipper | opt_subject;
            float m = opt_clipper && !opt_wireframe ? 0.0f : 0.5f;

            if (opt_original || opt_clipper) {
                gl.glColor3f(1.0f, 1.0f*m, 1.0f*m);
                draw_polygon(gl, cpoly);
            }
            if (opt_original || opt_subject) {
                gl.glColor3f(1.0f*m, 1.0f*m, 1.0f);
                draw_polygon(gl, spoly);
            }
        }

        gl.glFlush();
    }

    public void draw_polygon(GL gl, float[][] point) {
        gl.glBegin(opt_wireframe ? GL.GL_LINE_LOOP : GL.GL_POLYGON);
        for (int i = 0; i < point.length; i++) {
            gl.glVertex2f(point[i][0], point[i][1]);
        }
        gl.glEnd();
    }

    public void clip_polygon(GL gl, float[][] subj, float[][] clip) {
        Polygon subject = new Polygon(subj);
        Polygon clipper = new Polygon(clip);

        List<Polygon> clipped;
        
        if (opt_debug)
            System.out.println("Operation: " +
                (opt_operation!='/' ? "A"+opt_operation+"B" : "B\\A"));

        switch (opt_operation) {
            case '|': clipped = subject.union(clipper); break;
            case '&': clipped = subject.intersection(clipper); break;
            case '/': clipped = clipper.difference(subject); break;
            default : clipped = subject.difference(clipper);
        }

        for (Polygon p : clipped) {
            draw_polygon(gl, p.points());
            if (opt_debug)
                System.out.println(p);
        }
    }
    
    public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
        // do nothing
    }    
}
