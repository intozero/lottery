# Lottery Java modules

## Web application

The Spring Boot **Lottery Workbench** provides a local web UI and persistent H2
database for draw history, sums/deviation, occurrence/recency, ranges, digit
patterns, combinations, imports, and official Powerball updates.

```bash
mvn -pl lottery-web -am clean install
bash lottery-web/run.sh
```

Open **http://127.0.0.1:8080**. See [lottery-web/README.md](lottery-web/README.md)
for IntelliJ setup, the bundled Maven command, database backups, tests, and API
details. [Module analysis](lottery-web/MODULE-ANALYSIS.md) maps the existing tools
to their web equivalents.


Build from this repository's root with JDK 17 and Maven 3.9.x:

```bash
mvn clean install
```

This compiles, tests, packages, and installs the parent project and all modules listed in the root POM
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
reactor requires JDK 17 for the Spring Boot web application. The console modules
retain their existing Java 8 compilation targets. Lombok is configured
centrally in the parent, using 1.18.22, which supports JDK 17.

A successful build ends with `BUILD SUCCESS` and `SUCCESS` for all reactor
entries in the root POM. The Powerball regression suite runs as one
JUnit test executing 19 checks; failures stop the install. The refined analysis modules and web module also run their own regression tests.

To build only the Powerball module and its parent:

```bash
mvn -pl powerball-sync -am clean install
```

For running the Powerball validator/updater, checking its audit output, and
running it in IntelliJ, see [powerball-sync/README.md](powerball-sync/README.md).
