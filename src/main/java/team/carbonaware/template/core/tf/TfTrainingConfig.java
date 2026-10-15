package team.carbonaware.template.core.tf;

public record TfTrainingConfig(
    int maxSamples,
    int epochs,
    int batchSize,
    double learningRate,
    double validationSplit,
    int seed) {

  public static final TfTrainingConfig DEFAULT =
      new TfTrainingConfig(120_000, 12, 2048, 1e-3, 0.05, 42);

  public static final TfTrainingConfig DEFAULT_ENHANCED =
      new TfTrainingConfig(120_000, 12, 4096, 1e-3, 0.05, 42);
}
