# Data and calculations

[Guide index](README.md) · [Model details](MODELS.md) · [Samples and validation](SAMPLE-SETS-AND-VALIDATION.md)

## 1. Select the history and establish the target

The database history for the requested game is sorted oldest first. Original
source draw numbers are one-based and belong to this complete game history.
Starting draw `s`, row step `r`, and inclusive Last draw `c` first select source
rows `s, s+r, s+2r, …` up to `c`. Inclusive date bounds then remove rows outside
the date range. For example, start 2, step 5, last 18 selects 2, 7, 12, 17 before
date filtering. Original draw numbering is retained in later comparisons.

Call the resulting chronological selected history `H[0] … H[N-1]`. Feature
indices below are zero-based **selected-history** positions, not original draw
numbers. PB uses `M=69`; MM uses `M=75`, the application's historical analysis
universe. These bounds are not date-specific game-rule eligibility checks.

The nominal target is always original draw `c+1`. Last draw defaults to the latest
stored draw. If row step or date filters exclude draw `c`, the last feature draw
can precede the cutoff. The model makes no horizon adjustment for that gap.
With row step greater than one, historical labels describe the next *selected*
draw, even though the displayed target remains `c+1`.

No later draw is used to construct training features, labels, or the prior.
Changing the data import or inserting an older dated draw can change original
draw numbers on a subsequent request; the application does not persist a run's
source snapshot.

## 2. Construct labeled samples

Let `W` be the training-window setting:

```text
start = max(1, N - W)
trainingTransitions = N - start = min(W, N - 1)
trainingExamples = M * trainingTransitions
```

For every selected target index `i` from `start` through `N-1`, create one example
for each number `n` in `1 … M`:

```text
inputs = features(n, H[0 … i-1])
y = 1 if n is in the five white balls of H[i], otherwise 0
```

Features are calculated before the target draw is accumulated into history.
The Weka relation is `selected-draw-transitions`, with 11 numeric inputs and one
nominal class whose values are `no` then `yes`. The positive class is index 1.
Each transition supplies five positive examples and `M-5` negatives. There is no
application-level class balancing. Examples from a single draw are correlated;
the example count is not a count of independent lottery draws.

Each classifier learns **one shared binary membership function** across all
numbers. It does not train a separate classifier for each ball or directly learn
a joint distribution over five-ball combinations. Number is a numeric input.
For inference, create `M` additional feature rows at `i=N`, using all selected
history and a missing class label. These rows are scored, never fitted as labels.

| Selected draws N | Window W | Transitions | PB examples | MM examples |
| --- | --- | --- | --- | --- |
| 20 | 150 | 19 | 1,311 | 1,425 |
| 30 | 20 | 20 | 1,380 | 1,500 |
| 200 | 150 | 150 | 10,350 | 11,250 |
| 1,000 | 300 | 300 | 20,700 | 22,500 |

The minimum is 20 selected draws, not 20 transitions. The first selected draw
cannot be labeled because it has no earlier feature history. In the 30/20 case,
labels use indices 10–29; indices 0–9 still contribute to the feature prefixes.
The returned `trainingFrom` is the first **label date**, not the earliest feature
context date. `trainingThrough` is the last selected date.

## 3. The eleven feature formulas

For a sample at prefix length `i`, let `C(n,i)` count appearances of `n` in
`H[0 … i-1]`, and let `L(n,i)` be the last matching selected index, or `-1` if
never seen. `S(d)` is the sum of the five whites of draw `d`.

| Input name | Exact calculation | Interpretation |
| --- | --- | --- |
| `number` | `n / M` | Numeric location within the game's universe. |
| `frequency` | `C(n,i) / i` | Fraction of preceding selected draws containing the number. |
| `recent10` | Hits in `H[max(0,i-10) … i-1] / min(10,i)` | Up to ten preceding selected draws. |
| `recent30` | Hits in `H[max(0,i-30) … i-1] / min(30,i)` | Up to thirty preceding selected draws. |
| `since` | `(i - 1 - L(n,i)) / i` | Normalized completed selected draws since the last appearance; never seen is 1, latest appearance is 0. |
| `previousHit` | `1` if `n` is in `H[i-1]`, otherwise `0` | Previous selected draw membership. |
| `rangeFrequency` | Total preceding white appearances in bucket `floor(n/10) / (5*i)` | Share of all white-ball slots occupied by the number's range. |
| `meanSum` | `sum(S(H[j]), j=0…i-1) / (i*5*M)` | Normalized average preceding draw sum. |
| `previousSum` | `S(H[i-1]) / (5*M)` | Normalized latest selected sum. |
| `previousDeviation` | `populationStdDev(H[i-1].whites) / 40` | Exact population standard deviation, scaled by a fixed 40. |
| `odd` | `n % 2` | Even is 0; odd is 1. |

All divisions above are floating point. Range buckets are 1–9, 10–19, 20–29,
etc.; PB ends at 60–69 and MM at 70–75. Range frequency counts every ball in the
bucket, not only the candidate number and not the average of per-number counts.

For five white values `b1 … b5`:

```text
mean = (b1 + b2 + b3 + b4 + b5) / 5.0
populationStdDev = sqrt(sum((bk - mean)^2, k=1…5) / 5.0)
```

The ML feature uses the exact deviation, not its floor used in distribution/search
buckets. There are no date, special-ball, digit-pattern, pair-sum, or unseen-pattern
inputs. Delta does not change these prefix frequency formulas. Some Weka models
perform additional normalization internally, described in the model guide.

### Worked feature example

Consider this early prefix within a larger eligible training history:

| Selected prefix index | White balls | Sum |
| --- | --- | --- |
| 0 | 1, 2, 3, 4, 5 | 15 |
| 1 | 2, 6, 7, 8, 9 | 32 |
| 2 | 1, 2, 10, 11, 12 | 36 |

For PB, candidate `n=2`, and `i=3`, the input vector is:

```text
number            = 2/69                    = 0.0289855072
frequency         = 3/3                     = 1
recent10          = 3/3                     = 1
recent30          = 3/3                     = 1
since             = (3-1-2)/3               = 0
previousHit       = 1
rangeFrequency    = 12/(5*3)                 = 0.8
meanSum           = (15+32+36)/(3*5*69)      = 0.0801932367
previousSum       = 36/(5*69)                = 0.1043478261
previousDeviation = sqrt(22.16)/40           = 0.1176860230
odd               = 0
```

If the target at index 3 is `[1,4,8,10,13]`, this candidate's label is `no`.
Candidate 13 has label `yes` despite never appearing in that prefix; its prior
frequency features are zero and its `since` feature is 1. The target cannot alter
its inputs. This excerpt illustrates a calculation; fewer than 20 selected draws
cannot independently run the forecast.

## 4. Weight the training samples

Every number example belonging to target index `i` gets the same instance weight:

```text
age_i = (N - 1 - i) / max(1.0, N - start - 1)
weight_i = exp(-delta * age_i)
```

The newest labeled transition has weight 1 and the oldest has `exp(-delta)`.
Weka receives these weights in `DenseInstance`; how a classifier uses or internally
resamples them is model-specific. Weights are not probabilities that a draw occurs.

| Delta | Oldest/newest weight ratio | Effect |
| --- | --- | --- |
| 0 | 1 | Equal initial weights. |
| 1 | 0.367879 | Moderate recency emphasis. |
| 2 | 0.135335 | Stronger recency emphasis. |
| 6 | 0.002479 | Very small oldest contribution. |
| 10 | 0.0000454 | Very strong concentration on recent examples. |

Delta is not a learning rate, error tolerance, or sum/deviation distance. Large
delta can reduce effective sample support and interact with weighted minimum-leaf
sizes. It can make model fits less stable even with an unchanged example count.

## 5. Calculate the historical prior

The prior uses **all N selected draws**, not just the labeled window. For selected
index `j`:

```text
a_j = exp(-delta * (N - 1 - j) / max(1, N - 1))
A = sum(a_j, j=0…N-1)
C_n = sum(a_j * indicator(n in H[j]), j=0…N-1)
historical[n] = (C_n + 5.0/M) / (A + 1)
```

This adds one uniform pseudodraw: fractional count `5/M` for each ball, and one
draw to the denominator. It avoids an exactly zero prior for unseen numbers.
The priors sum to five, because each draw has five whites. They do not form a
categorical probability distribution summing to one. The two recency formulas
normalize ages over different spans: labeled transitions versus all selected draws.

## 6. Blend, rank, and form the guess

```text
learned[n] = classifier.distributionForInstance(target[n])[1]
learned[n] = clamp(learned[n], 0, 1)  // nonfinite values fail the model
blended[n] = mlWeight * learned[n] + (1 - mlWeight) * historical[n]
```

Rank by descending blended score, breaking exact ties by smaller ball number.
Take the first five distinct numbers, then sort those five ascending for display.
The returned `scores` list remains ranked; `whites` is numerically sorted. No
sum, deviation, unseen-pattern, or special-ball restriction is applied to this step.
A ball score is not a joint five-ball probability, and learned scores need not sum
to five. The app does not calibrate classifier outputs or compute a winning probability.

With delta 0, N=30, and six occurrences of a PB number:

```text
historical = (6 + 5/69) / 31 = 0.1958859280
```

If its learned score were 0.25 and ML weight were 0.7, its blended score would be
`0.7*0.25 + 0.3*0.1958859280 = 0.2337657784`. The learned score here is a
hypothetical value for arithmetic illustration, not a measured model output.

At ML weight 0 all successful models produce the same historical ranking; they
still train in the current implementation. At weight 1 ranking uses only the
classifier. Delta still affects fitting at weight 1.

## 7. Evaluate the frozen guess

The API collects every stored draw for the same game with original draw number
strictly greater than cutoff `c`, ignoring training date and row-step filters for
this comparison. Each model's five-ball guess is frozen once. There is no rolling
retraining and no update after observing a later result.

For guessed set `G` and later actual set `D`, `matchedBalls = size(G intersect D)`.
Order and special balls are excluded. Metrics are:

| Field | Calculation |
| --- | --- |
| `nextDrawMatches` | Overlap with original draw `c+1`; null if unavailable. |
| `averageMatches` | Sum of overlaps divided by number of later draws; null if none. |
| `exactMatches` | Number of later draws with all five whites matching. |
| `closestMatchedBalls` | Maximum later overlap; zero if no later draws. |
| `closestDraw` | Earliest later draw attaining that maximum; null if none. |
| `later` | Every later draw with date, actual whites, shared whites, overlap, original draw number, and `drawsAfterCutoff = drawNumber-c`. |

The earliest tie rule applies even when the maximum is zero. Distance is in
original draw numbers, not calendar days or sampled transitions. Repeated exact
matches on separate draws count separately. No later history is a valid forecast
with unavailable evaluation metrics, not a training failure.

The displayed random reference is `25/M`: expected overlap between a fixed set of
five and a uniform random five-element subset of the same M-number universe. It
is approximately 0.362319 for PB and 0.333333 for MM. It is neither a significance
test nor evidence of predictive skill. Choosing a model or tuning settings after
looking at these same future results makes them a tuning set, not an untouched test.
