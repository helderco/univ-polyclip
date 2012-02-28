#!/usr/bin/env python
# -*- coding: UTF-8 -*-
#
# Efficient Clipping of Arbitrary Polygons using OpenGPL
#
# Copyright (c) 2011, 2012 Helder Correia <helder.mc@gmail.com>
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

"""Efficient Clipping of Arbitrary Polygons using OpenGPL

Demonstrate the algorithm from Günther/Greiner with OpenGL.

You should have received the README file along with this program.
If not, see <https://github.com/helderco/polyclip>
"""


import OpenGL
OpenGL.ERROR_ON_COPY = True
from OpenGL.GL import *
from OpenGL.GLU import *
from OpenGL.GLUT import *

from polygon import *
from optparse import OptionParser, OptionGroup, OptionValueError

# subject polygon
spoly = [(1.5, 1.3), (7.5, 2.5), (4.0, 3.0), (4.5, 6.5)]

# clip polygon
cpoly = [(5.0, 4.5), (3.0, 5.5), (1.0, 4.0), (1.5, 3.5),
         (0.0, 2.0), (3.0, 2.3), (2.5, 1.0), (5.5, 0.0)]


class Graphics(object):
    """Use the algorithm by Günther/Greiner with OpenGL."""

    def __init__(self, options, default_subject=[], default_clipper=[]):
        self.options = options
        self.subject_polygon = options.subj_poly or default_subject
        self.clipper_polygon = options.clip_poly or default_clipper

    def run(self, title):
        glutInit(sys.argv)
        glutInitDisplayMode(GLUT_SINGLE | GLUT_RGB | GLUT_DEPTH)

        title += " - " + ("Original" if self.options.original else "Clipped")

        if self.options.wireframe:
            title += " (Wireframe)"

        glutInitWindowPosition(0, 0)
        glutInitWindowSize(300, 300)
        glutCreateWindow(title)
        self.init()

        glutKeyboardFunc(self.key)
        glutDisplayFunc(self.draw)
        glutReshapeFunc(self.reshape)

        glutMainLoop()

    def init(self):
        # glEnable(GL_DEPTH_TEST)
        # Setup the drawing area and shading mode
        glClearColor(0.0, 0.0, 0.0, 0.0)
        glShadeModel(GL_SMOOTH)

    def key(self, k, x, y):
        """Allows exiting upon Esc."""
        if ord(k) == 27: # Esc key
            sys.exit(0)

    def draw(self):
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT)

        if not self.options.original:
            glColor3f(0.5, 0.5, 1.0)
            clipped = clip_polygon(self.subject_polygon, self.clipper_polygon, self.options.operation)

            for poly in clipped:
                if self.options.debug:
                    print poly
                self.draw_polygon(poly.points)

        if self.options.original or self.options.clipper or self.options.subject:
            self.options.wireframe |= self.options.clipper | self.options.subject
            m = 0.0 if self.options.clipper and not self.options.wireframe else 0.5

            if self.options.original or self.options.clipper:
                glColor3f(1.0, 1.0 * m, 1.0 * m)
                self.draw_polygon(self.clipper_polygon)

            if self.options.original or self.options.subject:
                glColor3f(1.0 * m, 1.0 * m, 1.0)
                self.draw_polygon(self.subject_polygon)

        glFlush()

    def draw_polygon(self, points):
        glBegin(GL_LINE_LOOP if self.options.wireframe else GL_POLYGON)
        for x, y in points:
            glVertex2f(x, y)
        glEnd()

    # new window size or exposure
    def reshape(self, width, height):
        h = float(width) / float(height)
        glViewport(0, 0, width, height)
        glMatrixMode(GL_PROJECTION)
        glLoadIdentity()
        gluPerspective(45.0, h, 1.0, 20.0)
        glMatrixMode(GL_MODELVIEW)
        glLoadIdentity()
        ox, oy, oz = find_origin(self.subject_polygon, self.clipper_polygon)
        glTranslatef(ox, oy, oz)


class Arguments(object):
    """Define and parse command line arguments."""

    def __init__(self, *args, **kwargs):
        self.parser = OptionParser(*args, **kwargs)

        self.add_option("-o", "--show-original",
                        action="store_true", default=False, dest="original",
                        help="show original polygons")
        self.add_option("-w", "--wireframe",
                        action="store_true", default=False, dest="wireframe",
                        help="draw only the border lines")
        self.add_option("-c", "--show-clipper",
                        action="store_true", default=False, dest="clipper",
                        help="show clipper polygon as wireframe, on top of clipped")
        self.add_option("-s", "--show-subject",
                        action="store_true", default=False, dest="subject",
                        help="show subject polygon as wireframe, on top of clipped")
        self.add_option("-d", "--debug",
                        action="store_true", default=False, dest="debug",
                        help="show debug information on screen")

        oper = OptionGroup(self.parser, "Available Operations")

        oper.add_option("--union",
                        action="callback", callback=self.set_operation, dest="operation",
                        help="perform the union of the two polygons: A|B")
        oper.add_option("--intersection",
                        action="callback", callback=self.set_operation, dest="operation",
                        help="perform the intersection of the two polygons: A&B")
        oper.add_option("--difference",
                        action="callback", callback=self.set_operation, dest="operation",
                        help="difference between the polygons: A\\B (default)")
        oper.add_option("--reversed-diff",
                        action="callback", callback=self.set_operation, dest="operation",
                        help="reversed difference between the polygons: B\\A")

        self.parser.set_defaults(operation="difference")
        self.parser.add_option_group(oper)

        over = OptionGroup(self.parser, "Polygon Overrides")

        over.add_option("--subj-poly", type="string", metavar="POLY",
                        action="callback", callback=self.set_polygon,
                        help="override the vertices for the subject polygon")
        over.add_option("--clip-poly", type="string", metavar="POLY",
                        action="callback", callback=self.set_polygon,
                        help="override the vertices for the clipper polygon")

        over.set_description(
            """This program is provided as a demo, but you can override the pre-
            defined polygons without editing the file by using these options.
            POLY needs to be a string with pairs of floats (representing the
            the x and y coordinates of the vertexes), separated by semi-colons.

            Example: %s --subj-poly="1.5,1.25;7.5,2.5;4,3;4.5,6.5"
            """ % sys.argv[0])

        self.parser.add_option_group(over)

    def add_option(self, *args, **kwargs):
        self.parser.add_option(*args, **kwargs)

    def set_operation(self, option, opt_str, value, parser):
        setattr(parser.values, option.dest, opt_str[2:])

    def set_polygon(self, option, opt_str, value, parser):
        poly = parse_polygon(value)

        if not poly:
            raise OptionValueError("invalid syntax for polygon definition on option %s." % opt_str)

        setattr(parser.values, option.dest, poly)

    def parse_args(self, args):
        return self.parser.parse_args(args)


if __name__ == '__main__':
    print
    print "Efficient Clipping of Arbitrary Polygons using OpenGPL"
    print
    print "Copyright (C) 2011, 2012  Helder Correia"
    print "This program comes with ABSOLUTELY NO WARRANTY."
    print "This is free software, and you are welcome to redistribute"
    print "it under certain conditions; see source for details."
    print
    print "Run with -h or --help for available options."
    print "Press Esc to exit graphic window."
    print

    options = Arguments(epilog="\n").parse_args(sys.argv[1:])[0]
    Graphics(options, spoly, cpoly).run(title="Polygon Clipping")
