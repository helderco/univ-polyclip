# Efficient Clipping of Arbitrary Polygons using OpenGPL

Based on the paper "Efficient Clipping of Arbitrary Polygons" by Günther Greiner (greiner[at]informatik.uni-erlangen.de) and Kai Hormann (hormann[at]informatik.tu-clausthal.de), ACM Transactions on Graphics 1998;17(2):71-83.

Available at: <http://www.inf.usi.ch/hormann/papers/Greiner.1998.ECO.pdf>


## Motivation

This work was created for educational purposes only, as an implementation in Python of the above algorithm, for a class in Graphical Computation.


## Requirements

Supports Python 2.5 or later.

Requires **PyOpenGL** (version 3 as of this writing). If you have pip, install is easy:

`pip install pyopengl`


## Usage

The script can be divided in two parts. The first one is the algorithm, and the latter is it's usage with OpenGL (classes `Graphics` and `Arguments`).

Besides having the code for the algorithm to be studied, a command line interface is provided for demonstration or testing purposes, for running with OpenGL.

Supported operations are: union, intersection and difference.

### Polygon overrides

Subject and clip polygon can be defined per command line option. Defaults for the subject and clip polygon are set at the beggining of the file for easy edit, but they can be overriden from the command line using the options `--subj-poly` and `--clip-poly`.

**Example:**

`polyclip.py --subj-poly="1.5, 1.25; 7.5, 2.5; 4, 3; 4.5, 6.5"`

### Options

Type `polyclip.py -h` for available options. Press `Esc` to exit.

### Import

It is also possible to import and use the defined functions and classes in your own script. Currently there is no documentation for what you can use from import, so you'll have to study the code for now.
