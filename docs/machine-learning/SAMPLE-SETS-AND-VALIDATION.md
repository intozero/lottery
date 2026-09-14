# Sample sets, API, and validation

[Guide index](README.md) · [Calculations](DATA-AND-CALCULATIONS.md) · [Models](MODELS.md)

## Production data: what actually trains the models

Forecasts read the current game's stored database history, normally in
`data/lottery.mv.db`. They do not reread a CSV or download training data on each
forecast. With seeding enabled, an empty game's initial history comes from:

| Game | Initial seed file | Analysis white-ball universe |
| --- | --- | --- |
| PB | [files/pb/pb-sorted.txt](../../files/pb/pb-sorted.txt) | 1–69 |
| MM | [files/archive/mm-sorted.txt](../../files/archive/mm-sorted.txt) | 1–75 |

Existing game history is not reseeded at every startup. Subsequent uploads,
pasted imports, and official Powerball synchronization can add or correct stored
records. Missing seed files can leave a game empty. See the
[web data rules](../../lottery-web/README.md#import-and-sync-rules) for import
validation and game-era limitations.

The training sample consists only of database draws surviving game, original
start/end draw, row step, and inclusive date filtering. White-ball values are
sorted in `DrawRecord`. Special balls may be present in storage but never enter
ML features, labels, or comparisons. There is no pretrained dataset, internet
training corpus, random simulation dataset, or separate source for each model.

## The distinct sets in one request

| Set | Membership | Purpose |
| --- | --- | --- |
| Full game history | All stored game draws, oldest first | Establish original draw numbers and available bounds. |
| Selected history | Start/step/end selection, then date filters | Feature context and historical prior. Minimum 20 draws. |
| Labeled transition set | Last `min(W,N-1)` eligible selected targets | Fit classifiers; M examples per target. |
| Earlier feature context | Selected draws before the first labeled target | Supply prefix statistics; these draws are not independently labeled in this window. |
| Inference rows | One feature vector per number, after all selected history | Produce one frozen guess per model; missing labels. |
| Internal model samples | Forest bootstrap bags; REPTree growth/pruning folds; boosting reweighted sets; KNN neighborhoods | Algorithm-specific fitting/query mechanics, not additional external data. |
| Later comparison set | Every stored game draw with original number greater than Last draw | Retrospective evaluation only, without date or row-step restriction. |

There is no application-level random train/test split. There is also no
walk-forward retraining, nested validation, calibration set, hyperparameter search,
or final untouched test period selected automatically. REPTree's internal random
pruning split is not a substitute for chronological evaluation. RandomForest's
bootstrap samples are not newly observed lottery draws. All ten results remain
separate; `all` does not vote across models or select an automatic winner.

## Exact automated sample fixtures

These fixtures check implementation behavior. They are not a benchmark of real
lottery prediction accuracy and are not used during normal application forecasts.

### Core fixture A: deterministic 30-draw histories

[MachineLearningForecastTest.java](../../lottery-core/src/test/java/com/vipin/lottery/core/MachineLearningForecastTest.java)
constructs PB and MM histories as follows:

```text
for i = 0 … 29:
    date = 2026-01-01 plus i days
    white[j] = ((i*7 + j*13) mod M) + 1, for j = 0 … 4
    sort the five whites
    special = 1
```

The first three draws for both universes are `[1,14,27,40,53]`,
`[8,21,34,47,60]`, and `[15,28,41,54,67]`. These dates are daily fixture dates,
not a reproduction of a real draw schedule.

The all-model smoke test uses PB, 30 selected draws, cutoff 30, window 20,
delta 1, ML weight 0.7, and no later draws. It checks all ten models succeed,
return five distinct sorted in-range numbers and 69 finite ball-score entries,
obey blending, and report unavailable next-draw evaluation correctly. It does
not assert that any chosen number is a real future winner.

### Core fixture B: future-data independence and exact matches

Using the MM 30-draw generator, Naive Bayes, delta 2, ML weight 1, and window 20,
the test first forecasts without future data. It then constructs later records
at original draw numbers 31 and 33 with whites **copied from that forecast** and
special value 99. Both directly injected records use February 1 in this core
fixture; they bypass database uniqueness rules and are not a realistic import.

The test verifies that supplying these later records does not change guesses or
scores, that both exact matches are found, that next-draw overlap is five, average
overlap is five, and the earliest closest draw is 31. Distances are 1 and 3.
These perfect matches are deliberately manufactured assertions about evaluation
and data isolation; they are not evidence that Naive Bayes predicted actual draws.

### Core fixture C: two frequency regimes

Thirty PB draws use whites `[1,2,3,4,5]` for the first 20 and `[6,7,8,9,10]` for
the final ten, on January 1–30, 2026, with no special balls. Naive Bayes runs with
window 20 and **ML weight 0**. Delta 0 chooses the historically more frequent
first group; delta 6 chooses the recent second group.

This checks the exponential historical prior and final ranking. Because ML weight
is zero, it does not establish that a learned classifier detected a predictive
regime change. The classifier still trains in the current implementation.

### Core fixture D: invalid settings and cutoff isolation

Tests reject fewer than 20 selected draws, unknown models, invalid delta values
(including nonfinite values), invalid ML weight, an oversized training window,
and evaluation rows at or before the training cutoff. These are contract checks,
not accuracy measurements.

### Web integration fixtures

[WebIntegrationTest.java](../../lottery-web/src/test/java/com/vipin/lottery/web/WebIntegrationTest.java)
uses an isolated in-memory H2 database. The ML integration case inserts 44 PB
records using the same arithmetic generator, January 1–February 13, 2026.

One request uses `lastDraw=40`, `to=2026-01-25`, Naive Bayes, delta 2, ML weight
0.25, and window 20. Expected results include:

- Nominal target draw 41 despite the earlier date-filter endpoint.
- 25 selected draws, 20 transitions, and 1,380 training examples.
- Label dates January 6–25; earlier selected dates still provide feature context.
- All four later source draws 41–44 evaluated, despite being outside the training date filter.

A second request uses start 2, last 40, step 2: selected original draws are
`2,4,…,40`. Twenty selected draws yield 19 transitions and 1,311 PB examples,
while the nominal target remains 41. Other requests verify rejection of invalid
models/settings, out-of-history last draw, and too-short selections.

### UI fixtures

[workbench.test.cjs](../../lottery-web/src/test/ui/workbench.test.cjs) uses mocked
HTTP responses and jsdom. ML checks cover request settings, result rendering,
the 20-draw minimum, settings persistence, clearing stale results, validation,
and ignoring obsolete asynchronous responses. A displayed mock score or overlap
is fixture data. These tests do not invoke Weka or measure prediction performance.

## API contract

`GET /api/ml-forecast` trains and evaluates in memory; it does not modify stored
draws. Example request against the running local app:

```text
http://127.0.0.1:8080/api/ml-forecast?game=PB&startDraw=1&lastDraw=200&rowStep=1&model=all&delta=1&mlWeight=0.7&trainingWindow=150
```

The example requires at least 200 stored PB draws. For actual available bounds,
use `/api/history-bounds?game=PB` or the UI. Parameters:

| Parameter | Default | Meaning |
| --- | --- | --- |
| `game` | PB | PB or MM. |
| `from`, `to` | Unset | Inclusive ISO dates for training selection. |
| `startDraw` | 1 | Positive original starting draw number. |
| `lastDraw` | Latest stored draw | Inclusive cutoff, within stored history. |
| `rowStep` | 1 | Positive original draw-number sampling interval. |
| `model` | all | One documented model ID or all. |
| `delta` | 1 | Finite 0–10. |
| `mlWeight` | 0.7 | Finite 0–1. |
| `trainingWindow` | 150 | Integer 20–300. |

The response wrapper contains `targetDraw` (cutoff + 1), `lastDraw`, and `result`.

| Result field | Meaning |
| --- | --- |
| `library` | Weka 3.8.6. |
| `settings` | Model, delta, ML weight, and window used. |
| `selectedDraws` | N, after all training filters. |
| `trainingTransitions` | `min(W,N-1)`. |
| `trainingExamples` | Transitions multiplied by M. |
| `trainingFrom`, `trainingThrough` | First labeled and last selected dates. |
| `randomExpectedMatches` | `25.0/M`. |
| `models` | One result per requested classifier. |

Each model result includes `id`, `name`, `error`, `whites`, `scores`,
`nextDrawMatches`, `averageMatches`, `exactMatches`, `closestMatchedBalls`,
`closestDraw`, and `later`. Each score has `number`, `learned`, `historical`,
`blended`. Each later row has `drawNumber`, `date`, `drawsAfterCutoff`, `actual`,
`shared`, `matchedBalls`. See [evaluation definitions](DATA-AND-CALCULATIONS.md#7-evaluate-the-frozen-guess)
for exact formulas and tie handling.

Invalid request settings, fewer than 20 selected draws, and an out-of-range cutoff
produce a request error. A classifier-specific exception is instead represented
inside its model result so other requested classifiers can finish. Failed models
have an error message and empty whites, scores, and later rows; nullable metrics
are null and count fields are zero. A successful forecast with no later data has
`error=null`, populated whites/scores, empty later rows, null next/average/closest
draw, and zero exact/closest-match counts. Inspect `error` and `closestDraw` rather
than treating every zero as a measured evaluation result.

## Reproducibility and interpretation

To reproduce a result, retain the actual database history or a matching history
export, source draw-number mapping, game, date/start/end/step filters, all ML
settings, repository revision, Java version, and Weka version. The response alone
does not contain the full training snapshot or serialized models. Randomized
classifiers use fixed seeds, and the forest uses one execution slot, but changes
to data, row order, dependencies, or numerical implementation may change outputs.

All calculations are based on the historical universe configured in this project.
Mixing eras with different eligible ball ranges can introduce rule-change effects
into apparent frequencies. Strong past scores can also reflect correlated inputs,
random variation, and repeated tuning against the same later comparison period.
The current tests establish calculation and integration behavior, not improved
winning odds, calibration, or generalization to unseen lottery results.

For a future predictive-quality study, freeze settings using only earlier data,
evaluate multiple untouched chronological cutoffs, and report the evaluation
protocol and uncertainty alongside a suitable random baseline. Retraining at each
cutoff would require an explicit walk-forward evaluation procedure; the current
UI's frozen-guess comparison does not perform it automatically.

## Maintaining these documents

When changing `MachineLearningForecast`, update the feature/weight equations,
model settings, sample-count examples, and API field definitions together. When
upgrading Weka, inspect the pinned source and recapture each configured classifier's
`getOptions()` output; defaults can change even if the application factory does not.
Do not reinterpret synthetic exact-match fixtures as empirical model accuracy.

For behavioral changes, run the existing checks from the repository root:

```bash
mvn verify
npm --prefix lottery-web test
```

These commands exercise the implementation and UI contracts. Documentation-only
changes can instead be checked for valid links, consistent settings/formulas, and
clean diffs without rerunning model training.
