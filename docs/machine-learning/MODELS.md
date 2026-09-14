# All ten models

[Guide index](README.md) · [Shared calculations](DATA-AND-CALCULATIONS.md) · [Samples and validation](SAMPLE-SETS-AND-VALIDATION.md)

All models use the same weighted binary examples and 11 inputs described in the
calculation guide. Each receives its own copy of the prepared Weka `Instances`.
The table and sections below distinguish settings explicitly applied by the
application from behavior inherited from **Weka 3.8.6**. Seed 1 is used for
configured randomized algorithms; deterministic algorithms do not need a seed.
The [options snapshot](weka-3.8.6-options.txt) records runtime options, but Weka
source remains necessary to interpret defaults omitted from that output.

| ID | Java classifier | Application settings | Internal sample use |
| --- | --- | --- | --- |
| `random-forest` | `trees.RandomForest` | 30 trees, depth 8, seed 1, one execution slot | Weighted bootstrap bags, each 100% of training example count. |
| `logistic` | `functions.Logistic` | Ridge 0.0001, maximum 100 iterations | Full weighted training set. |
| `naive-bayes` | `bayes.NaiveBayes` | Defaults | Full weighted training set. |
| `j48` | `trees.J48` | Minimum objects 10 | Full weighted set with confidence-based pruning. |
| `rep-tree` | `trees.REPTree` | Depth 8, minimum 10, seed 1 | Internal randomized three-fold grow/prune division, then backfitting. |
| `random-tree` | `trees.RandomTree` | Depth 8, minimum 10, seed 1 | Full weighted training set; random attribute subsets. |
| `knn` | `lazy.IBk` | k = 15 | Retains all prepared examples; queries nearest neighbors. |
| `adaboost` | `meta.AdaBoostM1` | Up to 20 iterations, seed 1 | Repeatedly reweights the same examples. |
| `logitboost` | `meta.LogitBoost` | 20 iterations, seed 1 | Repeated weighted regression fits on working targets. |
| `neural-network` | `functions.MultilayerPerceptron` | Hidden layer 8, 30 epochs, seed 1, GUI off | Full training set in seeded randomized order; no validation holdout. |

Class names above are under `weka.classifiers`. No model gets special-ball labels,
future draws, or a separate external dataset. A model's `learned` result is its
positive-class distribution entry. The shared prior/blending/top-five procedure
then produces its displayed guess.

## 1. Random forest — `random-forest`

**Logic.** Train 30 randomized classification trees and average their class
distributions. Each tree recursively splits numeric feature values to improve
class separation, using entropy/information-gain criteria. It examines a random
subset of attributes at a node, reducing dependence between trees.

**Samples and weights.** Weka bagging takes bootstrap samples with replacement.
The bag-size percentage is 100: the number of sampling draws equals the original
example count, but some examples repeat and some are absent. Sampling incorporates
original instance weights. RandomForest represents repeated sampled copies through
weights. A bootstrap row is a number/transition example, not an entire lottery draw;
all M examples of one transition are not kept together as a sampling unit.

**Configuration.** Application: 30 trees, maximum depth 8, seed 1, one execution
slot. Default `K=0` selects `floor(log2(11))+1 = 4` candidate attributes for 11
predictors. The random split search may investigate further attributes if it has
not found a useful split, so four is not a universal hard limit on inspected
attributes. The forest's base-tree minimum weight is 1, unlike the standalone
RandomTree setting of 10. Out-of-bag error calculation is not enabled.

**Score.** If tree t supplies positive-class leaf probability `p_t`, the forest
score is their averaged distribution, conceptually `sum(p_t)/30`. Leaf
probabilities reflect sampled weighted class counts. The application does not
extract votes as a five-ball joint probability.

**Implications.** Delta changes bootstrap sampling influence. Random feature
selection can capture nonlinear interactions, but correlated training rows and
rare positive labels remain. Thirty trees and depth 8 bound size; they do not
establish predictive reliability. There is no reported out-of-bag validation.

Source: pinned `RandomForest.java`, `Bagging.java`, `RandomTree.java`;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/trees/RandomForest.html).

## 2. Logistic regression — `logistic`

**Logic.** Fit a linear log-odds function of the inputs. In binary notation:

```text
z = intercept + sum(beta_k * x_k)
p(yes | x) = 1 / (1 + exp(-z))
objective = weighted negative log likelihood + ridge * sum(nonintercept beta_k^2)
```

This expresses the positive-class model conceptually; Weka's coefficient storage
uses its own reference-class convention. The application reads distribution index
1 rather than interpreting coefficient signs.

**Samples and preprocessing.** All weighted examples are fitted together, with
no holdout or bootstrap. Weka replaces missing values, removes useless attributes,
converts nominal predictors to binary as necessary, and standardizes numeric
predictors internally. Here all 11 predictors are already numeric. Weighted
means are `sum(w*x)/sum(w)`. For sufficient total weight, the standard-deviation
calculation uses the weighted squared deviations and denominator `sum(w)-1`.
Degenerate attributes receive special handling. These are fitted training
statistics, not future-draw statistics.

**Configuration.** Ridge `1e-4`; maximum optimization iterations 100. The intercept
is not penalized. The ridge applies in the internally normalized fitting space;
Weka converts coefficients back for its model representation. A 100-iteration
limit does not guarantee convergence. There is no cross-validation selection of
ridge or iteration count.

**Score.** The normalized class distribution supplies `p(yes)`, then the common
blend applies. No calibration stage follows fitting.

**Implications.** Delta weights the likelihood contributions and weighted fitting
statistics. This model can learn additive effects but has no automatically
constructed nonlinear feature interactions. Number is ordinal numeric input, so
its linear effect does not independently parameterize every white ball. Correlated
frequency features can make coefficients sensitive; ridge reduces, but does not
eliminate, this issue.

Source: pinned `Logistic.java`;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/functions/Logistic.html).

## 3. Naive Bayes — `naive-bayes`

**Logic.** Estimate each feature distribution separately within each class and
multiply their likelihoods under a conditional-independence assumption:

```text
unnormalized(c) = prior(c) * product(likelihood(feature_k | c))
p(yes | x) = unnormalized(yes) / (unnormalized(no) + unnormalized(yes))
prior(c) = (weightedClassCount(c) + 1) / (totalWeight + 2)
```

This class prior is Weka's learned yes/no prior. It is distinct from the
application's per-ball historical prior used in blending.

**Samples and estimators.** All weighted training rows contribute to class counts
and per-class numeric estimators. Default NaiveBayes uses `NormalEstimator`, with
kernel estimation and supervised discretization disabled. Every predictor is
numeric, including `odd` and `previousHit`: those two do not receive Bernoulli
estimators simply because their values are zero or one.

Weka calculates numeric precision from sorted observed values and their distinct
spacing, with a default precision of 0.01 where needed. Estimators accumulate
weighted rounded values, estimate mean and standard deviation, and impose a
minimum standard deviation tied to precision (`precision/6`). Likelihood is
normal-CDF mass over the value's precision bin, rather than a bare Gaussian
probability density at a point. Weka floors very small likelihood contributions
and rescales during multiplication to limit numerical underflow.

**Configuration and score.** The application sets no nondefault NaiveBayes
options. The normalized posterior for `yes` is the learned score. Delta affects
weighted class priors and numeric distributions.

**Implications.** Cumulative frequency, recent frequencies, and previous-hit inputs
are strongly related; conditional independence is an approximation. Multiplying
correlated evidence can produce overconfident scores. These are not calibrated
odds of a lottery result.

Source: pinned `NaiveBayes.java`, `NormalEstimator.java`, `DiscreteEstimator.java`;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/bayes/NaiveBayes.html).

## 4. J48 decision tree — `j48`

**Logic.** Weka's C4.5 implementation chooses splits using information gain and
gain ratio, including numeric split-point handling. For weighted class fractions
`p_c` at a node:

```text
entropy = -sum(p_c * log2(p_c))
gain = parentEntropy - weightedMean(childEntropies)
gainRatio = gain / splitInformation
```

C4.5 also applies eligibility rules such as the average-gain gate and numeric
threshold correction; it does not simply choose the largest raw ratio among all
possible thresholds. It then collapses/prunes the tree using estimated error.

**Samples and configuration.** The full weighted dataset is used. The application
sets minimum objects to 10. Defaults include confidence factor 0.25, pruning,
subtree raising and collapsing enabled, and numeric MDL correction enabled.
Reduced-error pruning is disabled, so there is no separate random pruning holdout
in this configuration. There is **no explicit maximum depth of 8 for J48**.
Minimum-object checks relate to weighted support, not a guaranteed number of
independent draws.

**Score.** Follow the feature thresholds to a leaf and return its class
distribution based on weighted training support. Laplace output smoothing is off
by default. Confidence factor 0.25 controls pruning estimates; it is not a
confidence level for the displayed forecast.

**Implications.** Delta changes split statistics, support, and pruning outcomes.
A single tree gives piecewise constant scores and may produce many tied ball
scores; final ties favor smaller numbers. The app does not display the learned
tree or establish that any split represents a causal relationship.

Source: pinned `J48.java` and `trees/j48` implementation;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/trees/J48.html).

## 5. Reduced-error pruning tree — `rep-tree`

**Logic.** Grow a fast decision tree using information gain for the nominal yes/no
class, then prune branches when held-out error favors a simpler subtree. Numeric
predictor thresholds divide the weighted training examples. Leaf distributions
provide membership scores.

**Internal sample sets.** Weka randomizes the prepared examples with seed 1 and
stratifies nominal classes into three folds. Two folds grow the tree and one fold
is used for reduced-error pruning (`trainCV(3,0,random)` and `testCV(3,0)`).
After pruning, held-out data are backfitted into retained-tree statistics.
These are random number/example partitions: rows from the same selected draw may
appear on both sides. This internal pruning split is not chronological validation
and does not use any post-cutoff draw.

**Configuration.** Maximum depth 8, minimum support 10, seed 1. Defaults: three
pruning folds, pruning enabled, initial count 0. The emitted variance threshold
0.001 concerns regression behavior; this task's class is nominal. Initial count 0
means no additive leaf pseudocount is requested.

**Score and implications.** The resulting leaf class distribution supplies
`p(yes)`. Delta changes weighted training/pruning statistics. Pruning can reduce
overfitting to the prepared sample, but its random holdout cannot establish
forward-time forecasting quality. The returned training-example count describes
the original full set, not just the two growth folds.

Source: pinned `REPTree.java`;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/trees/REPTree.html).

## 6. Random tree — `random-tree`

**Logic.** Fit a single tree with randomized candidate attribute selection at
nodes, using classification split statistics. It is the same classifier family
used inside RandomForest, but this application configures and fits it separately.

**Samples and configuration.** Use the complete weighted prepared dataset once,
without a bootstrap bag. Maximum depth 8, minimum support 10, seed 1. Default
`K=0` computes four candidate attributes from 11 predictors, with the same useful-
split search qualification discussed for the forest. Default backfitting folds
are zero and there is no pruning holdout. The tree is not an average of 30 trees.

**Score.** A target feature vector follows splits to a leaf; weighted class
fractions supply the distribution, with Weka's handling of empty/unsupported
branches when applicable. Delta changes support and split selection.

**Implications.** Random attribute selection introduces variety, but one tree
can have higher variance and more ties than an ensemble. Weighted minimum support
10 differs from the forest base-tree default of 1, so the standalone model is not
merely the first forest tree.

Source: pinned `RandomTree.java`;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/trees/RandomTree.html).

## 7. k-nearest neighbors — `knn`

**Logic.** IBk stores training examples. For each candidate ball's target feature
vector, it searches for the nearest examples under Euclidean distance:

```text
distance(x,q) = sqrt(sum(normalizedDifference(x_k,q_k)^2))
```

The class attribute is excluded. Numeric differences are range-normalized by
Weka's distance function. `LinearNNSearch` performs a linear search; IBk passes
query information to the search object, which can update distance ranges to
include query values. Query vectors contain only selected-history features,
not future actual labels. This normalization is additional to the application's
fixed feature scaling.

**Samples and configuration.** k = 15; default distance weighting is off;
`LinearNNSearch` with `EuclideanDistance`; automatic cross-validation selection of
k is off. Weka's `-W 0` means no additional FIFO training-instance cap. It does
not disable the application's selected-transition window. All prepared examples
are retained, and exact boundary distance ties can produce more than 15 neighbors.

**Score.** Turning off *distance* weighting does not turn off *instance* weighting.
Votes still incorporate the recency weights attached to neighbors. With `L` stored
training examples, the two-class default vote calculation is:

```text
p(yes) = (1/L + sum(w_neighbor for positive neighbors))
         / (2/L + sum(w_neighbor for all neighbors))
```

The small class pseudocount is Weka's vote smoothing, separate from the shared
historical prior. Delta changes vote influence; it does not directly multiply
the feature distance by recency.

**Implications.** Neighbors can be different numbers at different draw times,
because the model pools all numbers. Correlated features can count similar
information multiple times in distance. Repeated feature rows and distance ties
matter. There is no learned joint five-ball combination structure.

Source: pinned `IBk.java`, `LinearNNSearch.java`, `NormalizableDistance.java`;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/lazy/IBk.html).

## 8. AdaBoost — `adaboost`

**Logic.** AdaBoostM1 repeatedly fits a weak classifier, increasing the influence
of examples that the current learner misclassifies. The default weak learner is
`DecisionStump`, a one-split classifier using weighted split statistics.

**Samples and configuration.** Up to 20 iterations, seed 1. The default stump
supports weighted examples, so the default configuration reweights rather than
resamples rows. Weight threshold 100 includes the complete set. The initial
weights are the application's delta weights; subsequent rounds change them in
response to classification errors.

For weighted error `e_t` in a usable round:

```text
beta_t = log((1-e_t)/e_t)
misclassified example weight *= (1-e_t)/e_t
all example weights are renormalized to preserve their total
```

Weka can stop early at effectively zero error or error at least 0.5. Its beta
convention has no extra factor of one-half. First-round stopping has special
handling so an available first learner can still supply output.

**Score.** With multiple performed rounds, accumulate `beta_t` for each weak
learner's predicted class, then normalize those class totals using Weka's
log-score-to-probability transformation (softmax). With only one learner, Weka
returns that learner's class distribution instead. It is not simply the fraction
of stumps voting `yes`.

**Implications.** The rare positive class makes accuracy-based weak-learner error
quite different from useful five-ball ranking quality. Boosting emphasizes hard
examples, which need not be recent ones. Delta initializes weights but does not
remain their only influence. The app does not tune class costs or calibrate the
boosted distribution.

Source: pinned `AdaBoostM1.java`, `DecisionStump.java`;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/meta/AdaBoostM1.html).

## 9. LogitBoost — `logitboost`

**Logic.** Build an additive logistic model by fitting regression weak learners to
working responses derived from the current probability errors. The default base
learner is again `DecisionStump`, used here with a numeric regression target.

For binary label `y` and current positive probability `p`, the conceptual working
response is `(y-p)/(p*(1-p))`. Weka computes it by class and clips its magnitude:

```text
z = min(1/p, 3)                 when y=1
z = max(-1/(1-p), -3)           when y=0
workingWeight = originalInstanceWeight * (y-p)/z
```

Without clipping, the latter curvature factor reduces to `p*(1-p)`. With clipping,
using that simpler formula would misdescribe this implementation. Working weights
are rescaled to the original total weight, then the stump fits weighted squared
error on `z`.

**Samples and configuration.** The same training rows are reused each round; no
post-cutoff samples are supplied. Application: 20 iterations, seed 1. Defaults:
shrinkage 1, response clipping 3, weight threshold 100, no resampling, uniform
initial class probabilities (0.5/0.5), one internal fold (no iteration-selection
cross-validation). The default likelihood improvement threshold is effectively
disabled. No extra user-facing learning-rate or iteration control is exposed.

**Score.** Class logits are updated by centered regression predictions scaled by
`(K-1)/K`. For two classes Weka uses a binary shortcut: fit one regression function
`f`, use the negative for the other class, and add opposite half-scaled increments
(with shrinkage 1). Convert logits `F_c` to probabilities:

```text
p(c) = exp(F_c) / sum(exp(F_otherClass))
```

The positive entry is the learned score. This differs from AdaBoost's hard
misclassification reweighting and weighted class-vote output.

**Implications.** Delta supplies the original instance weights that multiply the
working curvature weights. Iterative probability refinement can fit structure in
the prepared data, but neither the logistic objective nor 20 iterations establishes
future skill. No held-out calibration or chronological tuning occurs.

Source: pinned `LogitBoost.java`, `DecisionStump.java`;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/meta/LogitBoost.html).

## 10. Multilayer perceptron — `neural-network`

**Logic.** Weka's classic feed-forward network learns nonlinear transformations
through backpropagation. This configuration has 11 numeric inputs, one hidden
layer of eight units, and two nominal-class output units (`no`, `yes`). Hidden
and output units use sigmoid activations:

```text
hidden_j = sigmoid(b_j + sum(W_jk * normalizedInput_k))
a_c = sigmoid(b_c + sum(V_cj * hidden_j))
learned = a_yes / (a_no + a_yes)
```

This is normalized sigmoid output, not a single output neuron or an assumed
modern softmax/cross-entropy network. The classic backpropagation updates use
output error and sigmoid derivatives with momentum.

**Samples and preprocessing.** All prepared examples are used, in seeded
randomized order. Attribute normalization is on by default: numeric predictors
are centered at `(max+min)/2` and divided by `(max-min)/2`, using training ranges
and handling constant ranges specially. Training values normally lie in [-1,1];
future feature values can lie outside those ranges. Nominal-to-binary conversion
is enabled but the predictor schema is already numeric. Numeric-class normalization
settings do not apply to this nominal yes/no class.

**Configuration and fitting.** Application: hidden layer `8`, 30 epochs, seed 1,
GUI disabled. Defaults: learning rate 0.3, momentum 0.2, learning-rate decay off,
validation percentage 0. Therefore no validation examples are held aside and the
validation patience option of 20 is inactive. The effective example learning-rate
factor is `0.3 * instanceWeight`; momentum contributes separately to weight
updates. Weka's reset behavior is enabled: numerical divergence can trigger a
retry at a reduced learning rate.

**Score and implications.** The normalized `yes` output is blended with the
historical prior. Delta changes per-example update influence. The network has
nonlinear capacity, but only eight hidden units and 30 epochs are requested; the
app does not search architectures or verify convergence. This is a tabular
classifier, not a recurrent network or sequence model with hidden draw-to-draw
state. All temporal information must come from the supplied prefix features.

Source: pinned `MultilayerPerceptron.java` and `functions/neural` implementation;
[API reference](https://weka.sourceforge.io/doc.dev/weka/classifiers/functions/MultilayerPerceptron.html).

## Errors and degenerate fits

The application catches each model's training/scoring exception independently and
returns `Training failed: <exception class>` for that model, with empty guesses,
scores, and comparisons. It does not substitute another algorithm. Other models
in an `all` request can still complete. Nonfinite learned outputs are failures;
finite outputs are clamped to [0,1] before blending.

Some Weka classifiers have built-in constant-distribution/ZeroR behavior for
degenerate datasets. Such internal library behavior is distinct from application
fallback and is not separately annotated in the returned result. A successful
response therefore does not guarantee a complex or informative fitted model.
