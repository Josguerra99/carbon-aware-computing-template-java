package team.carbonaware.template.core.tf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import team.carbonaware.template.core.MathHelper;
import team.carbonaware.template.core.Types.GhiSlot;

/** Tabular features + sampling (mirrors {@code src/core/tf/tfCommon.ts}). */
public final class TfCommon {
  public static final int FEATURE_COUNT = 28;

  private TfCommon() {}

  public static double[] featureVector(Instant timestamp, double latitude, double longitude) {
    double[] out = new double[FEATURE_COUNT];
    int hour = (int) MathHelper.solarHour(timestamp, longitude);
    out[hour] = 1;
    out[24] = MathHelper.dayOfYearUtc(timestamp);
    out[25] = latitude;
    out[26] = longitude;
    out[27] = timestamp.atZone(java.time.ZoneOffset.UTC).getYear();
    return out;
  }

  public static TrainingArrays buildTrainingArrays(List<GhiSlot> trainingData, TfTrainingConfig config) {
    if (trainingData.isEmpty()) {
      return new TrainingArrays(List.of(), List.of());
    }
    List<GhiSlot> rows = sampleRows(trainingData, config.maxSamples(), config.seed());
    List<double[]> xs = new ArrayList<>(rows.size());
    List<Double> ys = new ArrayList<>(rows.size());
    for (GhiSlot slot : rows) {
      xs.add(featureVector(slot.time(), slot.latitude(), slot.longitude()));
      ys.add(Math.max(0, slot.ghiWm2()));
    }
    return new TrainingArrays(xs, ys);
  }

  static List<GhiSlot> sampleRows(List<GhiSlot> rows, int maxSamples, int seed) {
    if (rows.size() <= maxSamples) {
      return rows;
    }
    List<Integer> indices = new ArrayList<>();
    for (int i = 0; i < rows.size(); i++) {
      indices.add(i);
    }
    Random rng = new Random(seed);
    Collections.shuffle(indices, rng);
    List<GhiSlot> out = new ArrayList<>(maxSamples);
    for (int i = 0; i < maxSamples; i++) {
      out.add(rows.get(indices.get(i)));
    }
    return out;
  }

  public record TrainingArrays(List<double[]> xs, List<Double> ys) {}
}
