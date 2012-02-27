#!/usr/bin/env python
# -*- coding: UTF-8 -*-
#
# Efficient Clipping of Arbitrary Polygons using OpenGPL
#
# Copyright (c) 2011 Helder Correia <helder.mc@gmail.com>
#
# This program is free software: you can redistribute it and/or modify
# it under the terms of the GNU General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# any later version.
#
# This program is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public License
# along with this program.  If not, see <http://www.gnu.org/licenses/>.
#
# ########################################################################
#
# Created for educational purposes only.
#
# Based on the paper "Efficient Clipping of Arbitrary Polygons" by Günther
# Greiner (greiner[at]informatik.uni-erlangen.de) and Kai Hormann
# (hormann[at]informatik.tu-clausthal.de), ACM Transactions on Graphics
# 1998;17(2):71-83.
#
# Available at:
#
#      http://www.inf.usi.ch/hormann/papers/Greiner.1998.ECO.pdf
#
# Supported opperations are: union, intersection and difference.
#
# Type `polyclip.py -h` for available options. Press `Esc` to exit.
#

import OpenGL
OpenGL.ERROR_ON_COPY = True
from OpenGL.GL import *
from OpenGL.GLU import *
from OpenGL.GLUT import *
from OpenGL.constants import GLfloat
from optparse import OptionParser, OptionGroup

# subject polygon
spoly = [(1.5, 1.25), (7.5, 2.5), (4.0, 3.0), (4.5, 6.5)]

# clip polygon
cpoly = [(5.0, 4.5), (3.0, 5.5), (1.0, 4.0), (1.5, 3.5),
         (0.0, 2.0), (3.0, 2.25), (2.5, 1.0), (5.5, 0.0)]

title = "Polygon Clipping"

class Vertex(object):
    def __init__(self, vertex, alpha=0.0, intersect=False, entry=True, checked=False):
        
        if isinstance(vertex, Vertex):
            vertex = (vertex.x, vertex.y)
            # checked = True
        
        self.x, self.y = vertex     # point coordinates
        self.next = None            # Vertex object
        self.prev = None            # Vertex object
        self.neighbour = None       # Vertex object
        self.entry = entry          # True if entry, False if exit
        self.alpha = alpha
        self.intersect = intersect
        self.checked = checked
    
    def isInside(self, poly):
        """Test if a vertex lies inside a polygon (odd-even rule)
        
        This function calculates the "winding" number for a point, which
        represents the number of times a ray emitted from the point to
        infinity intersects any edge of the polygon.
        
        An even winding number means the point lies OUTSIDE the polygon; 
        an odd number means it lies INSIDE it.
        """
        winding_number = 0
        infinity = Vertex((1000000, self.y))
        for q in poly.iter():
            if not q.intersect and intersect(self, infinity, q, poly.next(q.next)):
                winding_number += 1
        return (winding_number % 2) != 0
    
    def setChecked(self):
        self.checked = True
        if self.neighbour and not self.neighbour.checked:
            self.neighbour.setChecked()
    
    def __repr__(self):
        return "(%.2f, %.2f) <-> %s(%.2f, %.2f)%s <-> (%.2f, %.2f) %s" % (
            self.prev.x, self.prev.y, 
            "i" if self.intersect else " ", 
            self.x, self.y,
            ("e" if self.entry else "x") if self.intersect else " ",
            self.next.x, self.next.y,
            " !" if self.intersect and not self.checked else ""
        )

class Polygon(object):
    first = None
    def add(self, vertex):
        """Add a vertex object to the polygon 
        (vertex is added at the 'end' of the list")"""
        if not self.first:
            self.first = vertex
            self.first.next = vertex
            self.first.prev = vertex
        else:
            next = self.first
            prev = next.prev
            next.prev = vertex
            vertex.next = next
            vertex.prev = prev
            prev.next = vertex
    
    def insert(self, vertex, start, end):
        """Insert and sort a vertex between a specified pair of vertices
        
        This function inserts a vertex (most likely an intersection point)
        between two other vertices (start and end). These other vertices
        cannot be intersections (that is, they must be actual vertices of
        the original polygon). If there are multiple intersection points
        between the two vertices, then the new vertex is inserted based on
        its alpha value
        """
        curr = start
        while curr != end and curr.alpha < vertex.alpha:
            curr = curr.next
        
        vertex.next = curr
        prev = curr.prev
        vertex.prev = prev
        prev.next = vertex
        curr.prev = vertex
    
    def next(self, v):
        """Return the next non intersecting vertex after the one specified"""
        c = v
        while c.intersect:
            c = c.next
        return c
    
    @property
    def nextPoly(self):
        """Return the next polygon (pointed by the first vertex)"""
        return self.first.nextPoly
    
    @property
    def first_intersect(self):
        """Return the first unchecked intersection point in the polygon"""
        for v in self.iter():
            if v.intersect and not v.checked:
                break
        return v
    
    @property
    def points(self):
        """Return the polygon's points as a list of tuples
        (ordered coordinates pair)
        """
        p = []
        for v in self.iter():
            p.append((v.x, v.y))
        return p
    
    def unprocessed(self):
        """Check if any unchecked intersections remain in the polygon"""
        for v in self.iter():
            if v.intersect and not v.checked:
                return True
        return False
    
    
    def union(self, clip):
        return self.clip(clip, False, False)
        
    def intersection(self, clip):
        return self.clip(clip, True, True)
        
    def difference(self, clip):
        return self.clip(clip, False, True)
        
    def clip(self, clip, s_entry, c_entry):
        """Calculate the difference between two polygons (subject and clipper).
        This is the meat and bones of the algorithm.
        """
        # phase one - find intersections
        for s in self.iter(): # for each vertex Si of subject polygon do
            if not s.intersect:
                for c in clip.iter(): # for each vertex Cj of clip polygon do
                    if not c.intersect:
                        try:
                            i, alphaS, alphaC = intersect(s, self.next(s.next),
                                                          c, clip.next(c.next))
                            iS = Vertex(i, alphaS, intersect=True, entry=False)
                            iC = Vertex(i, alphaC, intersect=True, entry=False)
                            iS.neighbour = iC
                            iC.neighbour = iS
                            
                            self.insert(iS, s, self.next(s.next))
                            clip.insert(iC, c, clip.next(c.next))
                        except TypeError:
                            pass # this simply means intersect() returned None
        
        # phase two - identify entry/exit points
        s_entry ^= self.first.isInside(clip) 
        for s in self.iter():
            if s.intersect:
                s.entry = s_entry
                s_entry = not s_entry
        
        c_entry ^= clip.first.isInside(self) 
        for c in clip.iter():
            if c.intersect:
                c.entry = c_entry
                c_entry = not c_entry
        
        # phase three - construct a list of clipped polygons
        list = []
        while self.unprocessed():
            current = self.first_intersect
            clipped = Polygon()
            clipped.add(Vertex(current))
            while True:
                current.setChecked()
                if current.entry:
                    while True:
                        current = current.next
                        clipped.add(Vertex(current))
                        if current.intersect:
                            break
                else:
                    while True:
                        current = current.prev
                        clipped.add(Vertex(current))
                        if current.intersect:
                            break
                
                current = current.neighbour
                if current.checked:
                    break
            
            list.append(clipped)
        
        if not list:
            list.append(self)
        
        return list
    
    def __repr__(self):
        """String representation of the polygon for debugging"""
        count, out = 1, "\n"
        for s in self.iter():
            out += "%02d: %s\n" % (count, str(s))
            count += 1
        return out
    
    def iter(self):
        """Iterator generator for this doubly linked list"""
        s = self.first
        while True:
            yield s
            s = s.next
            if s == self.first:
                return
    
    def show_points(self):
        """Draw points in screen for debugging purposes"""
        glBegin(GL_POINTS)
        for s in self.iter():
            glVertex2f(s.x, s.y)
        glEnd()


def intersect(s1, s2, c1, c2):
    """Algorithm based on: http://paulbourke.net/geometry/lineline2d/"""
    den = (c2.y - c1.y) * (s2.x - s1.x) - (c2.x - c1.x) * (s2.y - s1.y)
    
    if not den:
        return None
    
    us = ((c2.x - c1.x) * (s1.y - c1.y) - (c2.y - c1.y) * (s1.x - c1.x)) / den
    uc = ((s2.x - s1.x) * (s1.y - c1.y) - (s2.y - s1.y) * (s1.x - c1.x)) / den
    
    if (us == 0 or us == 1) and (0 <= uc <= 1) or \
       (uc == 0 or uc == 1) and (0 <= us <= 1):
        print "whoops! degenerate case!"
        return None
    
    elif (0 < us < 1) and (0 < uc < 1):
        x = s1.x + us * (s2.x - s1.x)
        y = s1.y + us * (s2.y - s1.y)
        
        return (x, y), us, uc
    
    return None


parser = OptionParser()
parser.add_option("-o", "--show-original",
    action="store_true", default=False, dest="original",
    help="show original polygons")
parser.add_option("-w", "--wireframe",
    action="store_true", default=False, dest="wireframe",
    help="draw only the border lines")
parser.add_option("-c", "--show-clipper",
    action="store_true", default=False, dest="clipper",
    help="show clip polygon as wireframe, on top of clipped")
parser.add_option("-s", "--show-subject",
    action="store_true", default=False, dest="subject",
    help="show subject polygon as wireframe, on top of clipped")
parser.add_option("-d", "--debug",
    action="store_true", default=False, dest="debug",
    help="show debug information on screen")

def set_operation(option, opt_str, value, parser):
    """set the clipping operation based on command line option"""
    setattr(parser.values, option.dest, opt_str[2:])

parser.set_defaults(operation="difference")

oper = OptionGroup(parser, "Available Operations")
oper.add_option("--union", 
    action="callback", callback=set_operation, dest="operation",
    help="perform the union of the two polygons: A|B")
oper.add_option("--intersection", 
    action="callback", callback=set_operation, dest="operation",
    help="perform the intersection of the two polygons: A&B")
oper.add_option("--difference", 
    action="callback", callback=set_operation, dest="operation",
    help="difference between the polygons: A\\B (default)")
oper.add_option("--reversed-diff", 
    action="callback", callback=set_operation, dest="operation",
    help="reversed difference between the polygons: B\\A")
parser.add_option_group(oper)

def parse_polygon(option, opt_str, value, parser):
    """construct a polygon based on string from command line option"""
    global spoly, cpoly
    try:
        poly = []
        for vertex in value.split(";"):
            x, y = vertex.split(",", 2)
            poly.append((float(x), float(y)))
        
        if opt_str == "--subject-poly":
            spoly = poly
        if opt_str == "--clip-poly":
            cpoly = poly
        
    except ValueError:
        import sys
        sys.stderr.write(
            "Invalid syntax for polygon definition on option %s. "
            "Using default.\n" % opt_str)

over = OptionGroup(parser, "Polygon Overrides")
over.add_option("--subject-poly", type="string", 
    action="callback", callback=parse_polygon,
    help="override the vertices for the subject polygon")
over.add_option("--clip-poly", type="string", 
    action="callback", callback=parse_polygon,
    help="override the vertices for the clip polygon")
parser.add_option_group(over)

(options, args) = parser.parse_args()

def find_origin():
    """Find the center coordinate for the given points"""
    x, y = [], []
    
    for s in spoly:
        x.append(s[0])
        y.append(s[1])
    
    for c in cpoly:
        x.append(c[0])
        y.append(c[1])
    
    x_min, x_max = min(x), max(x)
    y_min, y_max = min(y), max(y)
    
    width  = x_max - x_min
    height = y_max - y_min
    
    return -x_max/2, -y_max/2, -(1.5*width+1.5*height)/2

def draw():
    global options
    
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)
    
    if not options.original:
        glColor3f(0.5, 0.5, 1.0)
        clip_polygon(spoly, cpoly)
    
    if options.original or options.clipper or options.subject:
        options.wireframe |= options.clipper | options.subject
        m = 0.0 if options.clipper and not options.wireframe else 0.5
        
        if options.original or options.clipper:
            glColor3f(1.0, 1.0*m, 1.0*m)
            draw_polygon(cpoly)
        
        if options.original or options.subject:
            glColor3f(1.0*m, 1.0*m, 1.0)
            draw_polygon(spoly)
    
    glFlush()


def draw_polygon(points):
    glBegin(GL_LINE_LOOP if options.wireframe else GL_POLYGON)
    for x, y in points:
        glVertex2f(x, y)
    glEnd()


def clip_polygon(subject, clipper):
    Subject = Polygon()
    Clipper = Polygon()
    
    for s in subject:
        Subject.add(Vertex(s))
    
    for c in clipper:
        Clipper.add(Vertex(c))
    
    clipped = Clipper.difference(Subject) \
        if options.operation == "reversed-diff" \
        else Subject.__getattribute__(options.operation)(Clipper)
    
    for poly in clipped:
        if options.debug:
            print poly
        draw_polygon(poly.points)


# new window size or exposure
def reshape(width, height):
    h = float(width) / float(height)
    glViewport(0, 0, width, height)
    glMatrixMode(GL_PROJECTION)
    glLoadIdentity()
    gluPerspective(45.0, h, 1.0, 20.0)
    glMatrixMode(GL_MODELVIEW)
    glLoadIdentity()
    ox, oy, oz = find_origin()
    glTranslatef(ox, oy, oz)


def init():
    # glEnable(GL_DEPTH_TEST)
    # Setup the drawing area and shading mode
    glClearColor(0.0, 0.0, 0.0, 0.0)
    glShadeModel(GL_SMOOTH)


# exit upon ESC
def key(k, x, y):
    if ord(k) == 27: # Escape
        sys.exit(0)


if __name__ == '__main__':
    glutInit(sys.argv)
    glutInitDisplayMode(GLUT_SINGLE | GLUT_RGB | GLUT_DEPTH)
    
    title += " - " + ("Original" if options.original else "Clipped")
    
    if options.wireframe:
        title += " (Wireframe)"
    
    glutInitWindowPosition(0, 0)
    glutInitWindowSize(300, 300)
    glutCreateWindow(title)
    init()
    
    glutDisplayFunc(draw)
    glutReshapeFunc(reshape)
    glutKeyboardFunc(key)
    
    glutMainLoop()

