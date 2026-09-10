# Lottery Workbench

A Java 17 application for exploring lottery history, validating and updating
Powerball results, and exporting tabular reports through a local web UI.

## Project structure

| Module | Responsibility |
| --- | --- |
| `lottery-core` | Shared draw model, history parsing, official Powerball source validation, statistics, range/digit/combination analysis, and text reports. No runtime framework dependencies. |
| `lottery-web` | Spring Boot application: web UI, REST API, transactional H2 storage, imports, and correction audit. Depends only on core and its web/database libraries. |

The eleven standalone console projects have been retired. Their useful analysis
and reporting capabilities are consolidated into these two modules. Use the web
UI for inputs and results; old console main classes and launch scripts are no
longer entry points. See the [capability mapping](lottery-web/MODULE-ANALYSIS.md).

`files/` retains the original history and reference files. `data/` holds your
local database; neither is deleted by a clean build. The consolidation preserves
the web UI, API routes, database schema, and text report layouts.

## Build and run

Use **JDK 17** and **Maven 3.9.x**, from this repository root:

```bash
mvn clean install
bash lottery-web/run.sh
```

Open **http://127.0.0.1:8080**. Stop with Ctrl+C. No Node.js, login, or separately
installed database is needed to run the application.

On this Mac, if Maven is not on PATH, use IntelliJ's bundled Maven:

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn' clean install
bash lottery-web/run.sh
```

In IntelliJ, open/reload the root `pom.xml`, select JDK 17, and run
`com.vipin.lottery.web.WebApplication` with the repository root as its working
directory. Use Maven **Lifecycle → install**, or `mvn install`, rather than
`mvn install:install`: the lifecycle compiles and packages the JAR before
installing it.

The root POM manages Java, dependency versions, and Java formatting consistently
for both modules. A successful build shows three reactor entries: the parent,
Lottery Core, and Lottery Web. The executable JAR is
`lottery-web/target/lottery-web-1.0-SNAPSHOT.jar`.

## Development checks

```bash
mvn clean install       # compile, test, check formatting, package, install both modules
mvn spotless:apply      # format Java after editing
mvn -pl lottery-core test
```

Core tests cover calculations, parsing, official-source validation, and exact
report output fixtures captured before consolidation. Web integration tests use
an isolated in-memory database and cover API behavior, transactions and CSRF.

Optional UI behavior tests need Node.js:

```bash
cd lottery-web
npm ci
npm test
```

See the [web guide](lottery-web/README.md) for features, configuration, data rules,
backups, and API details, or the [core guide](lottery-core/README.md) for Java reuse.
