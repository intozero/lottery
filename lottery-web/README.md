# Lottery Workbench

A local Spring Boot application that brings the repository's lottery tools into a
single web UI, backed by a persistent H2 database.

## Start

Use **JDK 17** and Maven from the repository root:

```bash
mvn -pl lottery-web -am clean install
bash lottery-web/run.sh
```

Open **http://127.0.0.1:8080**. Stop with Ctrl+C. No Node.js, npm, external database,
or login is required to run the app.

If Maven is not on your PATH on this Mac:

```bash
JAVA_HOME="$(/usr/libexec/java_home -v 17)" \
  '/Applications/IntelliJ IDEA CE.app/Contents/plugins/maven/lib/maven3/bin/mvn' \
  -pl lottery-web -am clean install
bash lottery-web/run.sh
```

Or run the built JAR from the repository root:

```bash
java -jar lottery-web/target/lottery-web-1.0-SNAPSHOT.jar
```

In IntelliJ, reload the root Maven project, select JDK 17, and run
`com.vipin.lottery.web.WebApplication` with the **repository root as working
directory** and the `lottery-web` module classpath. The web module uses its own
Spring Boot parent; it does not change the Java 8 configuration of the console
modules. Use Maven **Lifecycle → install**, not the install plugin directly.

On first startup, each empty game's database history is seeded from:

- PB: `files/pb/pb-sorted.txt`
- MM: `files/archive/mm-sorted.txt`

Missing seed files are skipped; malformed seed files are logged and can be corrected
through a valid import. Existing database history is not reimported on restart.
The UI handles an empty database with an import prompt.

## The workspace

Choose PB or MM and optionally a date range. All history analyses and exports use
that selection. The UI provides searchable, sortable tables with 50-row pages,
responsive navigation, error/loading/empty states, and keyboard-accessible controls.

| Page | What it does |
| --- | --- |
| Overview | Draw count, average sum, latest stored draw, frequency chart, repeated combinations. |
| Draw history | Dated white/special balls, draw sums, repeated combinations, text export. |
| Number statistics | Total, since, last appearance, min/max draw gap; individual-number timeline; LAST/SIM/NUM_OCCUR exports. |
| Sums & deviation | Draw sums, running total/average, exact mean and population standard deviation, distributions, smallest/largest white-ball studies. |
| Range explorer | Cumulative range totals, observed patterns and shapes; all mathematically possible range patterns, unseen filtering, combination counts; RAN export. |
| Digit patterns | Configurable 1–100 digit windows, overlapping counts across draw boundaries, top 1,000 patterns. |
| Combinations | Ascending five-white-ball combinations filtered by maximum, target sum, and optional deviation floor. First 500 matches; truncation is explicit. |
| Data manager | Text upload/paste, official Powerball sync, exports, import activity, correction audit. |

Combination generation is mathematical and independent of history/date filters.
Import and official sync operate on the stored game history, regardless of the
date filter. This distinction is stated in the UI.

See [MODULE-ANALYSIS.md](MODULE-ANALYSIS.md) for the original module review and the
mapping of their basic capabilities.

## Database and data safety

H2 stores its files under `data/` relative to the launch directory:
`data/lottery.mv.db`. The run script always changes to the repository root, giving
it a stable location. Database files are Git-ignored and **not under target/**,
so a Maven clean build does not remove them.

Tables:

- `draws`: game/date, five sorted white balls, optional special ball, source,
  timestamp; unique constraint on game/date and structural value constraints.
- `imports`: successful import/sync source, counts added/corrected/unchanged, time.
- `draw_changes`: old and new values, game/date, correction source and timestamp.

Imports validate the entire file before writing. Transactions roll back all changes
if a conflict is found. Identical rows/dates are deduplicated; conflicting duplicates
inside an upload are rejected. Reimporting identical data adds no draws. A normal
import can fill an absent special ball but cannot replace known results.
Official sync can correct known values and records every correction.

For a full backup, **stop the app**, then copy the `data/` directory to another
location. Restore it while the app is stopped. History exports preserve the familiar
`M/d/yyyy  W1  W2  W3  W4  W5  SPECIAL` format, but do not contain the correction
audit. Original repository text files are never rewritten by this app.

`schema.sql` initializes the three tables idempotently. This is the initial schema;
future schema changes should use explicit migrations rather than editing existing
database files or relying on CREATE TABLE IF NOT EXISTS to alter columns.

## Import and sync rules

Uploads/pasted history accept a strict date plus five white balls and an optional
special ball, UTF-8 BOM, spaces/tabs, blank lines, and either newline convention.
Maximum: 3 MB / 30,000 unique draws per import. Future dates, duplicate white balls,
invalid dates, and conflicting duplicate dates are rejected with line context.

PB's broad white-ball bound is 69; MM uses the historical analysis universe 75.
Special balls accept 1–99 structurally. These import bounds are **not** validation
of date-specific game rules. Never-seen values in a fixed-universe/mixed-era
analysis should not be interpreted as eligible or “due” numbers.

Powerball sync downloads the [Texas Lottery official CSV](https://www.texaslottery.com/export/sites/lottery/Games/Powerball/Winning_Numbers/download.html).
It validates the modern 5/69 + 1/26 history from October 7, 2015, schedule
(Wednesday/Saturday, plus Monday from August 23, 2021), duplicate source dates,
coverage gaps, and freshness through the latest scheduled draw before today in
America/New_York. Today's draw is used only if published.

Every stored PB date must exist in the official source; unverifiable dates abort
the update. Missing draws are added from the earliest stored PB date, or October
7, 2015 if empty. A stale source, timeout, invalid response, or source gap fails
without changing stored history. Retry later if publication is delayed. Sync is
manual; there is no scheduled background update.

## Calculation definitions

- **Since:** completed draws after the most recent appearance, zero for a latest-draw
  hit. Never seen = count of selected draws.
- **Gaps:** distance between successive appearance draw indices; fewer than two
  appearances has no min/max gap. Not calendar days.
- **Sums/means:** five white balls only, with floating-point averages.
- **Deviation:** exact population standard deviation about the exact mean.
  Distribution/search buckets use its floor. This deliberately fixes the legacy
  code's integer-truncated mean and deviation.
- **Digit stream:** sorted, unpadded white values, then special ball, concatenated
  chronologically with no separator. Overlaps and cross-draw windows count.
  Missing special balls block this analysis. Exact repeated combinations are
  detected separately, using separated ball values.
- **Ranges:** 1–9, 10–19, etc., with PB ending at 69 and historical MM at 75.
  Pattern entries sum to five. Possible combinations per pattern are the product
  of binomial choices within each bucket. Unseen patterns mean absent from the
  selected history, not increased future likelihood.
- **Occupancy shapes:** nonempty bucket counts sorted descending.
- **First/last studies:** smallest/largest sorted white-ball frequencies and their
  sum. Number timelines provide cumulative per-draw occurrence/recency.

Large exhaustive enumerations from the legacy scripts are represented by bounded
combination searches and exact range-universe counts; the app does not start
unbounded jobs or silently claim a truncated list is complete.

## Configuration

| Variable | Default | Purpose |
| --- | --- | --- |
| PORT | 8080 | HTTP port. |
| LOTTERY_DB | ./data/lottery | H2 file path without .mv.db suffix; use an absolute path for a fixed external location. |
| LOTTERY_DB_PASSWORD | empty | Optional password for a newly created H2 database; existing databases require their existing password. |
| LOTTERY_ROOT | . | Repository path used for first-run seed files. |
| LOTTERY_SEED | true | Set false to start with an empty DB and import manually. |

The app binds to **127.0.0.1**, uses same-origin requests, Spring Security CSRF tokens
for mutations, SameSite session cookies, and a content security policy. It is a
single-user local application with no authentication. Keep this binding for local
use; internet/multi-user deployment requires authentication, TLS, and deployment
configuration. The H2 browser console is disabled. No Sites/Cloudflare runtime is
used because this is the requested JVM Spring Boot application.

## API overview

GET:
`/api/history`, `/api/analysis`, `/api/timeline`, `/api/ranges`, `/api/digits`,
`/api/combinations`, `/api/export`, `/api/imports`, `/api/changes`, `/api/csrf`.

History-derived endpoints accept `game`, optional ISO `from` and `to` dates.
Timeline requires `number`; digits requires `window`. Combinations requires
`maximum`, `sum`, optional `deviation`. Export supports
`history|sums|last|sim|num_occur|ran`.

POST:
`/api/import` (multipart game/file), `/api/import-text` (JSON game/text),
`/api/powerball/sync`. Obtain a CSRF token from `/api/csrf`, retain the session
cookie, and send the returned header/token pair on mutations. The UI does this
automatically.

The data service uses parameterized SQL and transactions. Calculation services
have no shared per-request mutable state. Existing `totsincecombined.Statistics`
and sum/combined report writers are reused directly; stateless web-native adapters
cover the older console-only modules.

## Verification

```bash
# Backend and repository build (10 web integration tests included)
mvn clean install

# Optional frontend behavior tests; Node is only needed for these tests
cd lottery-web
npm ci
npm test
```

Web integration tests use isolated in-memory H2 and no external network. They
cover transactions, rollback, corrections, special-ball filling, parser errors,
CSRF, uploads, calculations, date filters, exports, range counts, combinations,
and official-source rejection cases.

Five jsdom tests exercise all pages, search/pagination, date filters, analysis
forms, import/sync CSRF handling, and visible failures. These test DOM behavior,
not screenshot appearance or browser-specific rendering. A visual browser review
could not be performed in the development session because no browser automation
surface was available.

Live checks also exercise the packaged JAR, seeded history, exports, an idempotent
import, and real official sync. Use the latest import activity in the UI to see
what is stored and when it was updated.
