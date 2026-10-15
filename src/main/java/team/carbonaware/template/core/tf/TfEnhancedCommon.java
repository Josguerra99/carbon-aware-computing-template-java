package team.carbonaware.template.core.tf;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import team.carbonaware.template.core.MathHelper;
import team.carbonaware.template.core.Types.GhiSlot;

/** sin/cos features (mirrors {@code src/core/tf/tfEnhancedCommon.ts}). */
public final class TfEnhancedCommon {
  public static final int ENHANCED_FEATURE_COUNT = 7;

  private TfEnhancedCommon() {}

  public static double[] enhancedFeatureVector(Instant timestamp, double latitude, double longitude) {
    double hour = MathHelper.solarHour(timestamp, longitude);
    double solarAngle = (2 * Math.PI * hour) / 24;
    int doy = MathHelper.dayOfYearUtc(timestamp);
    double doyAngle = (2 * Math.PI * doy) / 365.25;
    double unixDays = timestamp.toEpochMilli() / 86_400_000.0;
    return new double[] {
      Math.sin(solarAngle),
      Math.cos(solarAngle),
      Math.sin(doyAngle),
      Math.cos(doyAngle),
      latitude / 90,
      longitude / 180,
      unixDays / 10_000,
    };
  }

  public static TfCommon.TrainingArrays buildEnhancedTrainingArrays(
      List<GhiSlot> trainingData,
      TfTrainingConfig config) {
    if (trainingData.isEmpty()) {
      return new TfCommon.TrainingArrays(List.of(), List.of());
    }
    List<GhiSlot> rows = TfCommon.sampleRows(trainingData, config.maxSamples(), config.seed());
    List<double[]> xs = new ArrayList<>(rows.size());
    List<Double> ys = new ArrayList<>(rows.size());
    for (GhiSlot slot : rows) {
      xs.add(enhancedFeatureVector(slot.time(), slot.latitude(), slot.longitude()));
      ys.add(Math.max(0, slot.ghiWm2()));
    }
    return new TfCommon.TrainingArrays(xs, ys);
  }

}
