package team.carbonaware.template.core.tf;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import team.carbonaware.template.core.MathHelper;
import team.carbonaware.template.core.Types.GhiSlot;

/** 12 cyclical features (mirrors {@code src/workshop/heliosFeatures.ts}). */
public final class HeliosCommon {
  public static final int HELIOS_FEATURE_COUNT = 12;

  private HeliosCommon() {}

  private static double solarHourContinuous(Instant timestamp, double longitude) {
    var zdt = timestamp.atZone(ZoneOffset.UTC);
    double utc =
        zdt.getHour()
            + zdt.getMinute() / 60.0
            + zdt.getSecond() / 3600.0
            + zdt.getNano() / 3_600_000_000_000.0;
    return ((utc + longitude / 15) % 24 + 24) % 24;
  }

  public static double[] heliosFeatureVector(Instant timestamp, double latitude, double longitude) {
    double hour = solarHourContinuous(timestamp, longitude);
    double solarAngle = (2 * Math.PI * hour) / 24;
    int doy = MathHelper.dayOfYearUtc(timestamp);
    double doyAngle = (2 * Math.PI * doy) / 365.25;
    double doy2 = doyAngle * 2;
    double lonAngle = (2 * Math.PI * longitude) / 360;
    double unixDays = timestamp.toEpochMilli() / 86_400_000.0;
    return new double[] {
      Math.sin(solarAngle),
      Math.cos(solarAngle),
      Math.sin(doyAngle),
      Math.cos(doyAngle),
      Math.sin(doy2),
      Math.cos(doy2),
      latitude / 90,
      longitude / 180,
      Math.abs(latitude) / 90,
      Math.sin(lonAngle),
      Math.cos(lonAngle),
      unixDays / 10_000,
    };
  }

  public static TfCommon.TrainingArrays buildHeliosTrainingArrays(
      List<GhiSlot> trainingData, TfTrainingConfig config) {
    if (trainingData.isEmpty()) {
      return new TfCommon.TrainingArrays(List.of(), List.of());
    }
    List<GhiSlot> rows = TfCommon.sampleRows(trainingData, config.maxSamples(), config.seed());
    List<double[]> xs = new ArrayList<>(rows.size());
    List<Double> ys = new ArrayList<>(rows.size());
    for (GhiSlot slot : rows) {
      xs.add(heliosFeatureVector(slot.time(), slot.latitude(), slot.longitude()));
      ys.add(Math.max(0, slot.ghiWm2()));
    }
    return new TfCommon.TrainingArrays(xs, ys);
  }
}
