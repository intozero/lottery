# as-number: lottery digit-pattern analysis

`as-number` is an analysis module in the lottery repository. It reads dated draws,
joins their ball values into one digit stream, counts recurring substrings, and
reports exact combinations that occurred on multiple dates. It reads local data;
it does not fetch or validate official winning results. Use `powerball-sync` for
that separate task.

For example, the draw `1 2 3 4 5 6` becomes `123456`. Two such draws produce
`123456123456`. With window `2`, `12` occurs twice and `61` occurs once across the
boundary between draws. This is descriptive digit-pattern analysis, not a forecast
of future draws.

## Build and run

From the `lottery` repository root, with Maven and JDK 17:

```bash
mvn -pl as-number -am clean install
java -jar as-number/target/as-number-1.0-SNAPSHOT.jar
```

If `mvn` is not on your PATH on this Mac, use IntelliJ's bundled Maven:

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -pl as-number -am clean install
java -jar as-number/target/as-number-1.0-SNAPSHOT.jar
```

Use Maven's **Lifecycle → install** in IntelliJ, not **Plugins → install → install:install**:
the lifecycle creates the JAR before installing it. The JAR runs without additional
runtime dependencies and targets Java 8.

On startup, enter the window length at the console prompt and press Enter.
No program arguments are needed. Input is fixed at
`files/pb/pb-sorted.txt` and output at `as-number/target/as-number-report.txt`.
Run from the repository root. The input file is never changed.

```bash
# Start, then type 6 at the prompt
java -jar as-number/target/as-number-1.0-SNAPSHOT.jar

# Show usage
java -jar as-number/target/as-number-1.0-SNAPSHOT.jar --help
```

Example console interaction:

```text
Enter window length: 6
```

Blank, nonnumeric, or nonpositive values are rejected. End-of-input exits with an
error instead of waiting indefinitely. File paths and the window are not
command-line options; type the number in the console after starting the program.

A successful run prints the number of analyzed draws, date range, window length,
and absolute report path. Errors go to stderr and return exit code 1. `--help`
and successful analyses return 0. Run the module whenever you want a fresh report;
it does not run automatically.

## Run in IntelliJ

1. Import/reload the root Maven project.
2. Open `src/main/java/asnumber/AsNumber.java` inside this module.
3. Run its `main()` method with the `as-number` module classpath.
4. Set the working directory to the `lottery` repository root.
5. Leave program arguments empty.
6. In the Run console, type the window length (for example `10` or `6`) and press Enter.

## Input and counting rules

Each nonblank line must contain a valid date and exactly six ball values:

```text
10/17/2015  48  49  57  62  69  19
10/21/2015  30  32  42  56  57  11
```

- Dates use `M/d/yyyy`; spaces and tabs between fields are accepted.
- The first five balls must be distinct. The sixth ball may equal a white ball.
- Ball values must be 1–99. These are generic structural checks, not game-specific
  Powerball or Mega Millions range/schedule validation.
- Duplicate dates, invalid dates, missing balls, and empty input are rejected with
  an error. Missing special balls are never replaced with a fabricated value.
- Draws are analyzed chronologically. White balls are sorted within each draw;
  the sixth ball remains last. The source file's order is not modified.
- Digit values are unpadded and joined without separators, preserving the original
  module's representation. Different combinations can produce identical digit
  strings; combination detection therefore uses separated ball values independently.
- Every overlapping window is counted, including the last one and windows crossing
  draw boundaries. For example, `1111` contains three windows of length 2.
- The window must be positive and no greater than the total digit-stream length.

The report contains draw/date/digit totals, repeated combinations with their dates,
and every unique substring and its count. Substrings are ordered by count descending,
then lexicographically by digits for ties. Counts sum to `digits - window + 1`.
Repeated combinations on different dates remain part of the analysis.

The report is replaced on each successful run, not appended, so unchanged inputs
produce identical output. Invalid input leaves an existing report untouched.
Output cannot be the input file. A completed report is written by atomic replacement.
Reports under `target/` are removed by `mvn clean`; copy the report elsewhere if
you want to keep it across clean builds.

## What was cleaned up

The original code had no runnable entry point, repeatedly rescanned the entire digit
stream, missed the final window, inconsistently counted overlaps, used ambiguous
concatenation for duplicate combinations, and silently swallowed file errors. It also
substituted `99` for missing special balls and appended reports indefinitely.

The implementation now separates parsing (`FileRead`), an immutable draw (`LineDto`),
and analysis/CLI (`AsNumber`). It uses one pass over substring starting positions,
typed collections, strict parsing, explicit errors, and deterministic reports. The
old mutable DTO accessors, stateful `openFile/readFile` reader, and `numberCheck` /
`numberasString` methods were replaced; no other repository module called them.
Results intentionally differ from the old buggy counting behavior.
