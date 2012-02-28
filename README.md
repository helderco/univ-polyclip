# Efficient Clipping of Arbitrary Polygons using OpenGPL

Based on the paper "Efficient Clipping of Arbitrary Polygons" by Günther Greiner (greiner[at]informatik.uni-erlangen.de) and Kai Hormann (hormann[at]informatik.tu-clausthal.de), ACM Transactions on Graphics 1998;17(2):71-83.

Available at: <http://www.inf.usi.ch/hormann/papers/Greiner.1998.ECO.pdf>


## Motivation

This work was created for educational purposes only, as an implementation in Java of the above algorithm, for a class in Graphical Computation.


## Files

To study the algorithm, inspect file `Polygon.java`. It can be imported and used in other contexts (e.g., not opengl).

The command line interface is provided as a demo (`PolygonClip.java`), running the algorithm with OpenGL. The rest of this document is about this feature.


## Requirements

Made with **JOGL 1**.

Used the [NetBeans OpenGL Pack](http://kenai.com/projects/netbeans-opengl-pack/pages/Home), which at this does does **not** support [JOGL 2](http://kenai.com/projects/jogl).

### JOGL 1 installation in Netbeans at this time

1. Download the NetBeans OpenGL Pack from the [plugin portal](http://plugins.netbeans.org/PluginPortal/faces/PluginDetailPage.jsp?pluginid=3260) and extract the archive;
2. Start NetBeans and open the Plugin Manager (Tools->Plugins);
3. Enable Force install into shared directories on the Settings page;
3. Add all modules (.nbm files) to the Downloaded plugins list and press Install;
4. The installation wizard will guide you now for the rest of the installation.


## Command line usage

Provided for demonstration or testing purposes. Can't see how it could be useful to an end user.

Supported operations are: union, intersection and difference.

Subject and clip polygon can be defined per command line option. Defaults for the subject and clip polygon are set at the beggining of the file for easy edit, but they can be overriden from the command line using the options `--subj-poly` and `--clip-poly`.

**Example:**

`java -jar polyclip.jar --subj-poly="1.5, 1.25; 7.5, 2.5; 4, 3; 4.5, 6.5"`

**Options:**

Run with `-h` or `--help` for available options.

`java -jar polyclip.jar --help`

