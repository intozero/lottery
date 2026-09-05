# Powerball history validator and updater

The Java entry point is `com.vipin.lottery.powerball.PowerballSync`, located at
`powerball-sync/src/main/java/com/vipin/lottery/powerball/PowerballSync.java`.
It downloads official results, validates the draw dates and white/red balls in your
file, and optionally fixes errors, removes duplicate dates, and adds missing draws.

Run it whenever you want to check your data or capture newly published draws. It
does not run automatically. You do not need to run the class separately when using
`run.sh`: the script compiles and runs the same Java class for you.

## Run from the terminal

Run from the repository root with a JDK 8 or newer (no Maven or external libraries required):

```bash
# Check and write an audit, without modifying the numbers file
bash powerball-sync/run.sh

# Validate, back up, repair and update the numbers file
bash powerball-sync/run.sh --write

# Run the dependency-free regression tests
bash powerball-sync/test.sh
```

Live validation requires internet access. Check that both `java -version` and
`javac -version` work; the script needs a JDK, not just a Java runtime.

Default input: `files/pb_visual/pb-sorted.txt`. Use `--file PATH` for another file; relative paths are resolved from the repository root by the script. The module is also registered in the root Maven POM. Its regression tests run through `test.sh`.

## Run the Java class in IntelliJ

1. Open `powerball-sync/src/main/java/com/vipin/lottery/powerball/PowerballSync.java`.
2. Create an Application run configuration for `com.vipin.lottery.powerball.PowerballSync`, or use the Run icon beside its `main()` method.
3. Select the `powerball-sync` module for the classpath and a JDK 8 or newer.
4. Set the working directory to the `lottery` project root so the default input path resolves correctly.
5. Leave program arguments empty to validate only, or enter `--write` to apply repairs and add missing draws.
6. Run the configuration and review the console summary and audit file.

If IntelliJ does not recognize the module, import/reload the root `pom.xml` as a
Maven project. Running from IntelliJ and running `run.sh` execute the same functionality;
choose either approach.

## Verify the results

Validation prints counts for duplicate dates, incorrect rows, non-draw date rows,
and missing draws, plus the total verified draws and date range. Without `--write`,
these describe the proposed changes; the numbers file remains unchanged.

Review the line-by-line details in `files/pb_visual/pb-sorted.txt.audit.txt`.
When `--write` changes the file, it first saves the original beside it as
`pb-sorted.txt.<unique-id>.bak`.

After updating, run `bash powerball-sync/run.sh` again. With no new results published
in between, all issue counts should be zero and the console should say
`Already up to date; no changes.` Save the previous audit first if you want to keep
the repair details, because each successful check overwrites it.

Run `bash powerball-sync/test.sh` to check the implementation separately from your
live data. The regression suite uses local test data, requires no internet access,
and reports `Passed 19 checks.` on success.

## Source, validation rules, and file handling

The source is the [Texas Lottery official CSV download](https://www.texaslottery.com/export/sites/lottery/Games/Powerball/Winning_Numbers/download.html). It includes the full drawing history since Texas joined Powerball. The parser uses the five white balls and the Powerball column, not the Power Play multiplier. New York's dataset was evaluated but contained five missing historical draws at the time of implementation.

The updater supports the 5/69 + 1/26 game starting October 7, 2015. It validates every existing row against the official result for its date, restores missing/incorrect balls, sorts the white balls, removes duplicate dates, removes entries on non-draw dates, and fills every missing scheduled draw from the earliest valid input draw through the latest published result. Wednesday/Saturday draws and Monday draws starting August 23, 2021 are supported. Numbers may repeat across different dates, and the red ball may equal a white ball; neither is a duplicate drawing.

Output remains oldest first, `M/d/yyyy  W1  W2  W3  W4  W5  RED`, with two spaces between fields. Existing newline style and presence/absence of a final newline are preserved. It never adds history earlier than the file's earliest valid draw.

Each successful validation writes `pb-sorted.txt.audit.txt` beside the input, with the source URL, check time, counts and original/corrected lines. Counts overlap: a duplicate row can also have incorrect balls or a non-draw date. The audit is overwritten on the next check, so save it if you need a permanent record. `--write` creates a uniquely named `pb-sorted.txt.*.bak` containing the original bytes before an atomic replacement. Repeating the update with unchanged results leaves the file untouched and creates no additional backup.

Network errors, invalid source data, duplicate source dates, gaps in the official history, unparseable input dates, or input dates outside the supported/verified range cause a nonzero exit without rewriting the input. The feed must cover at least the last scheduled draw before today in America/New_York; today's draw is included only when published. If the feed has not published yesterday's result yet, retry later. A source failure does not refresh an existing audit; check its timestamp.

## Offline validation

For reproducible offline validation, download the unmodified Texas Lottery CSV and use:

```bash
bash powerball-sync/run.sh --source-csv /path/to/powerball.csv
bash powerball-sync/run.sh --source-csv /path/to/powerball.csv --write
```

Offline mode trusts the supplied snapshot, still checks source completeness and freshness, and records its path in the audit.
