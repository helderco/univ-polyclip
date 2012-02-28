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

import java.util.ArrayList;
import java.util.List;

/**
 * Based on the paper "Efficient Clipping of Arbitrary Polygons" by Günther
 * Greiner (greiner[at]informatik.uni-erlangen.de) and Kai Hormann
 * (hormann[at]informatik.tu-clausthal.de), ACM Transactions on Graphics
 * 1998;17(2):71-83.
 *
 * Available at: http://www.inf.usi.ch/hormann/papers/Greiner.1998.ECO.pdf
 */

/**
 * This class is almost exactly as described in the paper by Günther/Greiner.
 * Basically it is a node in a circular doubly linked list.
 */
class Vertex {
    /** Coordinates of the vertex */
    float x, y;

    /** References to the next and previous vertices of the polygon */
    Vertex next, prev;

    /** Reference to the corresponding intersection vertex in the other polygon */
    Vertex neighbour;

    /** Intersection points relative distance from previous vertex */
    float alpha = 0.0f;

    /** True if intersection is an entry point to another polygon;
     * False if it is an exit point */
    boolean entry = true;

    /** True if vertex is an intersection */
    boolean intersect = false;

    /** True if the vertex has been checked (last phase) */
    boolean checked = false;

    /** Constructor */
    public Vertex(float x, float y) {
        this.x = x;
        this.y = y;
    }

    /** Constructor using a two-value array, to represent the coordinates */
    public Vertex(float[] coord) {
        this.x = coord[0];
        this.y = coord[1];
    }

    /** Create an intersection vertex */
    static Vertex intersection(float x, float y, float alpha) {
        Vertex vertex = new Vertex(x, y);
        vertex.alpha = alpha;
        vertex.intersect = true;
        vertex.entry = false;
        return vertex;
    }

    /** Set the vertex as checked */
    public void setChecked() {
        this.checked = true;
        if (neighbour != null && !neighbour.checked) {
            neighbour.setChecked();
        }
    }

    /**
     * Test if a vertex lies inside a polygon (odd-even rule)
     *
     * This function calculates the "winding" number for a point, which
     * represents the number of times a ray emitted from the point to
     * infinity intersects any edge of the polygon.
     *
     * An even winding number means the point lies OUTSIDE the polygon;
     * an odd number means it lies INSIDE it.
     */
    boolean isInside(Polygon poly) {
        int winding_number = 0;
        Vertex infinity = new Vertex(10000, this.y);
        Vertex q = poly.first;
        Intersection i;
        do {
            i = new Intersection(this, infinity, q, poly.getNext(q.next));
            if (!q.intersect && i.test()) {
                winding_number++;
            }
            q = q.next;
        } while (!q.equals(poly.first));
        return (winding_number % 2) != 0;
    }

    /** String representation of the vertex for debugging purposes */
    @Override
    public String toString() {
        return String.format(
            "(%.2f, %.2f) <-> %s(%.2f, %.2f)%s <-> (%.2f, %.2f) %s",
            prev.x, prev.y, (intersect ? 'i' : ' '),
            x,      y,      (intersect ? (entry ? 'e' : 'x') : ' '),
            next.x, next.y, (intersect && !checked ? '!' : ' ')
        );
    }
}

/**
 * Test the intersection between two lines (two pairs of two points)
 *
 * If the test passes, the object will supply the coordinates of the
 * intersection and the subject and clipper alphas as us and uc respectively.
 */
class Intersection {
    float x, y;
    float us, uc;
    
    Intersection(Vertex s1, Vertex s2, Vertex c1, Vertex c2) {
        float den = (c2.y - c1.y) * (s2.x - s1.x) - (c2.x - c1.x) * (s2.y - s1.y);

        if (den == 0.0f) {
            return;
        }

        us = ((c2.x - c1.x) * (s1.y - c1.y) - (c2.y - c1.y) * (s1.x - c1.x)) / den;
        uc = ((s2.x - s1.x) * (s1.y - c1.y) - (s2.y - s1.y) * (s1.x - c1.x)) / den;

        if (test()) {
            x = s1.x + us * (s2.x - s1.x);
            y = s1.y + us * (s2.y - s1.y);
        }
    }

    final boolean test() {
        return (0 < us && us < 1) && (0 < uc && uc < 1);
    }
}

/**
 * Manages a circular doubly linked list of Vertex objects that
 * represents a polygon.
 *
 * The class consists of basic methods to manage the list and methods to
 * implement boolean operations between polygon objects.
 */
public class Polygon {

    /** The first Vertex in the linked list */
    Vertex first;

    /** Number of vertices in the list */
    int vertices = 0;

    /** Constructor */
    Polygon() {}

    /** Constructor which accepts an array of float[] {x, y} */
    public Polygon(float[][] p) {
        for (int i = 0; i < p.length; i++) {
            add(new Vertex(p[i]));
        }
    }

    /**
     * Add a vertex object to the polygon
     * (vertex is added at the 'end' of the list')
     *
     * @param vertex
     */
    private void add(Vertex vertex) {
        if (first == null) {
            first = vertex;
            first.next = vertex;
            first.prev = vertex;
        } else {
            Vertex next, prev;
            next = first;
            prev = next.prev;
            next.prev = vertex;
            vertex.next = next;
            vertex.prev = prev;
            prev.next = vertex;
        }
        vertices++;
    }

    /**
     * Insert and sort a vertex between a specified pair of vertices
     *
     * This function inserts a vertex (most likely an intersection point)
     * between two other vertices (start and end). These other vertices
     * cannot be intersections (that is, they must be actual vertices of
     * the original polygon). If there are multiple intersection points
     * between the two vertices, then the new vertex is inserted based on
     * its alpha value.
     *
     * @param vertex  Vertex to insert
     * @param start   Start Vertex to become prev of the inserted
     * @param end     End Vertex to become next of the inserted
     */
    void insert(Vertex vertex, Vertex start, Vertex end) {
        Vertex prev, curr = start;
        while (!curr.equals(end) && curr.alpha < vertex.alpha) {
            curr = curr.next;
        }
        vertex.next = curr;
        prev = curr.prev;
        vertex.prev = prev;
        prev.next = vertex;
        curr.prev = vertex;
        vertices++;
    }

    /** Return the next non intersection vertex after the one specified */
    Vertex getNext(Vertex v) {
        Vertex c = v;
        while (c.intersect) {
            c = c.next;
        }
        return c;
    }

    /** Return the first unchecked intersection point in the polygon */
    Vertex getFirstIntersect() {
        Vertex v = first;
        do {
            if (v.intersect && !v.checked) {
                break;
            }
            v = v.next;
        } while (!v.equals(first));
        return v;
    }

    /** Check if any unchecked intersections remain in the polygon */
    boolean hasUnprocessed() {
        Vertex v = first;
        do {
            if (v.intersect && !v.checked) {
                return true;
            }
            v = v.next;
        } while (!v.equals(first));
        return false;
    }

    /** Return a multidimensional array with the vertexes' coordinates */
    public float[][] points() {
        float[][] points = new float[vertices][2];
        Vertex v = first;
        for (int i = 0; i < vertices; i++) {
            points[i] = new float[] {v.x, v.y};
            v = v.next;
        }
        return points;
    }

    /** Calculate the union between two polygons */
    public List<Polygon> union(Polygon poly) {
        return clip(poly, false, false);
    }

    /** Calculate the intersection between two polygons */
    public List<Polygon> intersection(Polygon poly) {
        return clip(poly, true, true);
    }

    /** Calculate the difference between two polygons */
    public List<Polygon> difference(Polygon poly) {
        return clip(poly, false, true);
    }

    /**
     * Clip the polygon using this as the subject, and the first argument as the clipper.
     *
     * This is where the algorithm gets executed. It allows you to make
     * a UNION, INTERSECT or DIFFERENCE operation between two polygons.
     *
     * Given two polygons A, B the following operations may be performed:
     *
     * A|B ... A OR B (Union of A and B)
     * A&B ... A AND B (Intersection of A and B)
     * A\B ... A - B
     * B\A ... B - A
     * 
     * The entry records store the direction the algorithm should take when
     * it arrives at that entry point in an intersection. Depending on the
     * operation requested, the direction is set as follows for entry points
     * (f=foreward, b=back; exit points are always set to the opposite):
     *
     *       Entry
     *       A   B
     *       -----
     * A|B   b   b
     * A&B   f   f
     * A\B   b   f
     * B\A   f   b
     *
     * f = True, b = False when stored in the entry record
     *
     * @param clip     clip polygon
     * @param s_entry  entry flag for the subject polygon
     * @param c_entry  entry flag for the clipper polygon
     */
    public List<Polygon> clip(Polygon clip, boolean s_entry, boolean c_entry) {
        // Phase one - find intersections
        Vertex s = this.first;
        Vertex c = clip.first;

        do { // for each vertex Si of subject polygon do
            if (!s.intersect) {
                do { // for each vertex Cj of clip polygon do
                    if (!c.intersect) {
                        Intersection i = new Intersection(
                            s, this.getNext(s.next), c, clip.getNext(c.next)
                        );
                        if (i.test()) {
                            Vertex iS = Vertex.intersection(i.x, i.y, i.us);
                            Vertex iC = Vertex.intersection(i.x, i.y, i.uc);

                            iS.neighbour = iC;
                            iC.neighbour = iS;

                            this.insert(iS, s, this.getNext(s.next));
                            clip.insert(iC, c, clip.getNext(c.next));
                        }
                    }
                    c = c.next;
                } while (!c.equals(clip.first));
            }
            s = s.next;
        } while (!s.equals(this.first));

        // phase two - identify entry/exit points
        s = this.first;
        c = clip.first;
        
        s_entry ^= s.isInside(clip);
        c_entry ^= c.isInside(this);

        do {
            if (s.intersect) {
                s.entry = s_entry;
                s_entry = !s_entry;
            }
            s = s.next;
        } while (!s.equals(this.first));

        do {
            if (c.intersect) {
                c.entry = c_entry;
                c_entry = !c_entry;
            }
            c = c.next;
        } while (!c.equals(clip.first));

        // phase three - construct a list of clipped polygons
        List<Polygon> list = new ArrayList<Polygon>();

        while (hasUnprocessed()) {
            Vertex current = getFirstIntersect();
            Polygon clipped = new Polygon();
            clipped.add(new Vertex(current.x, current.y));
            do {
                current.setChecked();
                if (current.entry) {
                    do {
                        current = current.next;
                        clipped.add(new Vertex(current.x, current.y));
                    } while (!current.intersect);
                    
                } else {
                    do {
                        current = current.prev;
                        clipped.add(new Vertex(current.x, current.y));
                    } while (!current.intersect);
                }
                current = current.neighbour;
            } while (!current.checked);
            
            list.add(clipped);
        }

        if (list.isEmpty()) {
            list.add(this);
        }

        return list;
    }

    /** String representation of the polygon for debugging purposes */
    @Override
    public String toString() {
        int count = 1;
        String out = "Printing polygon:\n";
        Vertex s = first;
        do {
            out += String.format("%02d: %s\n", count++, s);
            s = s.next;
        } while (!s.equals(first));
        return out;
    }
}
