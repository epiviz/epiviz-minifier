epiviz-minifier
===============

Java program to minify epiviz javascript code

To run:

```
java -jar -Done-jar.silent=TRUE minifier.jar <path to epiviz>/index.php <output_path>
```

This generates four files and two directories:

* ```concat.js``` contains all the Epiviz library, concatenated into one file.
* ```min.js``` the minified Epiviz library
* ```concat.css``` contains all the CSS code
* ```min.css``` contains the minified CSS code
* ```css-img``` contains most of css images
* ```images``` contains the rest of the css images

To compile:

Assumes ant is installed:

```
ant jar
```

Creates the file `bin/minifier.jar`.

