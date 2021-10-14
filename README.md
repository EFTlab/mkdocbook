# mkdocbook

This is a self-contained Java program for converting DocBook manuals to PDF.

## Building

To build the jar, you'll need to install maven.

On Ubuntu:
``` sh
sudo apt install maven
```

On RedHat/CentOS:
``` sh
sudo dnf install maven
```

Then use maven to compile the project.  It will take a few minutes the first
time as it downloads all the dependencies.

``` sh
git clone git@github.com:EFTlab/mkdocbook.git
cd mkdocbook
mvn clean package
ls -l target/mkdocbook-*.jar
```

## Usage

Run the program directly from the jar, like this:

``` sh
java -jar target/mkdocbook-0.3.jar
```

To generate a PDF, run it like this:

``` sh
java -jar target/mkdocbook-0.3.jar \
 -x path/to/docbook-stylesheet.xsl \
 -i path/to/input-docbook.xml \
 -o path/to/output.pdf \
 -p some.param=foo \
 -p another.param=bar
```
