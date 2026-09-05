# Lottery Java modules

Build from this repository's root with JDK 17 and Maven 3.9.x:

```bash
mvn clean install
```

This compiles, tests, packages, and installs the parent project and all 17 modules
into the local Maven repository (`~/.m2/repository`). Each module's JAR is also
written to its `target/` directory. No lottery applications are started and no
lottery data files are modified by the build.

On this Mac, Maven is bundled with IntelliJ IDEA CE but is not on the shell PATH.
Use the following command to select the installed JDK and bundled Maven:

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn' clean install
```

If Maven reports that `JAVA_HOME` is invalid, select JDK 17 as above. The full
reactor requires a newer JDK than the standalone Powerball script because
`web-element-extract` targets Java 16. The build is verified with JDK 17; the other
modules retain their existing Java 8 compilation targets. Lombok is configured
centrally in the parent, using 1.18.22, which supports JDK 17.

A successful build ends with `BUILD SUCCESS` and `SUCCESS` for all 18 reactor
entries (the parent plus 17 modules). The Powerball regression suite runs as one
JUnit test executing 19 checks; failures stop the install. Most other modules
currently have no automated tests. `production` and `pb-visual` have no source
files and therefore produce empty JARs; their empty-JAR warnings are expected.

To build only the Powerball module and its parent:

```bash
mvn -pl powerball-sync -am clean install
```

For running the Powerball validator/updater, checking its audit output, and
running it in IntelliJ, see [powerball-sync/README.md](powerball-sync/README.md).
