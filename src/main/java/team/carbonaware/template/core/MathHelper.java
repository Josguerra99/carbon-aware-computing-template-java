package team.carbonaware.template.core;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;

public final class MathHelper {
  /** Sample {@code (x, y)} — {@code x} is doy, UTC ms, solar hour, etc. depending on the model. */
  public record XYPoint(double x, double y) {}

  private MathHelper() {}

  public static int dayOfYearUtc(Instant timestamp) {
    var date = timestamp.atZone(ZoneOffset.UTC).toLocalDate();
    return date.getDayOfYear();
  }

  private static double[] solveSystem(double[][] matrix, double[] vector) {
    int n = matrix.length;
    double[][] rows = new double[n][n + 1];
    for (int i = 0; i < n; i++) {
      System.arraycopy(matrix[i], 0, rows[i], 0, n);
      rows[i][n] = vector[i];
    }
    for (int col = 0; col < n; col++) {
      int pivot = col;
      for (int row = col + 1; row < n; row++) {
        if (Math.abs(rows[row][col]) > Math.abs(rows[pivot][col])) {
          pivot = row;
        }
      }
      double[] tmp = rows[col];
      rows[col] = rows[pivot];
      rows[pivot] = tmp;
      double diag = rows[col][col];
      if (Math.abs(diag) < 1e-12) {
        return null;
      }
      for (int j = col; j <= n; j++) {
        rows[col][j] /= diag;
      }
      for (int row = 0; row < n; row++) {
        if (row == col) {
          continue;
        }
        double factor = rows[row][col];
        for (int j = col; j <= n; j++) {
          rows[row][j] -= factor * rows[col][j];
        }
      }
    }
    double[] out = new double[n];
    for (int i = 0; i < n; i++) {
      out[i] = rows[i][n];
    }
    return out;
  }

  public static ToDoubleFunction<double[]> fitByFeatures(List<double[]> samples, List<Double> y) {
    int n = samples.size();
    int k = samples.isEmpty() ? 0 : samples.getFirst().length;
    if (n < k || k == 0) {
      return null;
    }
    double[][] ata = new double[k][k];
    double[] aty = new double[k];
    for (int i = 0; i < n; i++) {
      double[] row = samples.get(i);
      double target = y.get(i);
      for (int a = 0; a < k; a++) {
        aty[a] += row[a] * target;
        for (int b = 0; b < k; b++) {
          ata[a][b] += row[a] * row[b];
        }
      }
    }
    double[] weights = solveSystem(ata, aty);
    if (weights == null) {
      return null;
    }
    return features -> {
      double sum = 0;
      for (int i = 0; i < weights.length; i++) {
        sum += weights[i] * features[i];
      }
      return sum;
    };
  }

  public static double[] sineFeatures(double doy) {
    double angle = (2 * Math.PI * (doy - 1)) / 365.25;
    return new double[] {1, Math.sin(angle), Math.cos(angle)};
  }

  private static double plusCube(double value, double knot) {
    double delta = value - knot;
    return delta > 0 ? delta * delta * delta : 0;
  }

  /** Cubic + truncated cubic spline basis on x in [0, 1] (linear-regression demo). */
  public static double[] polySplineFeatures(double xn) {
    return new double[] {
      1,
      xn,
      xn * xn,
      xn * xn * xn,
      plusCube(xn, 0.2),
      plusCube(xn, 0.4),
      plusCube(xn, 0.6),
      plusCube(xn, 0.8),
    };
  }

  public static double[] sineFeaturesFromTimeMs(double tMs, double tMin, double tMax) {
    double xn = (tMs - tMin) / (tMax - tMin == 0 ? 1 : tMax - tMin);
    double angle = 2 * Math.PI * xn;
    return new double[] {1, Math.sin(angle), Math.cos(angle)};
  }

  /** {@code x} on each point is day-of-year (TS: {@code doy}). */
  public static DoubleUnaryOperator createSineCosineRegression(List<XYPoint> points) {
    if (points.size() < 3) {
      return null;
    }
    var samples = points.stream().map(p -> sineFeatures(p.x())).toList();
    var y = points.stream().map(XYPoint::y).toList();
    var predict = fitByFeatures(samples, y);
    if (predict == null) {
      return null;
    }
    return doy -> predict.applyAsDouble(sineFeatures(doy));
  }

  /** Sin/cos on normalized UTC ms within the sample span. */
  public static DoubleUnaryOperator createSineCosineRegressionOnTimeMs(List<XYPoint> points) {
    if (points.size() < 3) {
      return null;
    }
    double tMin = points.getFirst().x();
    double tMax = points.getFirst().x();
    for (XYPoint point : points) {
      tMin = Math.min(tMin, point.x());
      tMax = Math.max(tMax, point.x());
    }
    final double min = tMin;
    final double max = tMax;
    var samples =
        points.stream().map(p -> sineFeaturesFromTimeMs(p.x(), min, max)).toList();
    var y = points.stream().map(XYPoint::y).toList();
    var predict = fitByFeatures(samples, y);
    if (predict == null) {
      return null;
    }
    return tMs -> predict.applyAsDouble(sineFeaturesFromTimeMs(tMs, min, max));
  }

  public static DoubleUnaryOperator createPolynomialSplineRegression(List<XYPoint> points) {
    if (points.size() < 8) {
      return null;
    }
    double minX = points.getFirst().x();
    double maxX = points.getFirst().x();
    for (XYPoint point : points) {
      minX = Math.min(minX, point.x());
      maxX = Math.max(maxX, point.x());
    }
    double span = maxX - minX == 0 ? 1 : maxX - minX;
    final double min = minX;
    var samples =
        points.stream().map(p -> polySplineFeatures((p.x() - min) / span)).toList();
    var y = points.stream().map(XYPoint::y).toList();
    var predict = fitByFeatures(samples, y);
    if (predict == null) {
      return null;
    }
    return x -> predict.applyAsDouble(polySplineFeatures((x - min) / span));
  }

  public static DoubleUnaryOperator createParabola(double height, double start, double end) {
    double mid = (start + end) / 2;
    double half = (end - start) / 2;
    return x -> height * (1 - Math.pow((x - mid) / half, 2));
  }

  public static double solarHour(Instant timestamp, double longitude) {
    var utc = timestamp.atZone(ZoneOffset.UTC);
    double utcHours = utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0;
    return Math.round(((utcHours + longitude / 15) % 24 + 24) % 24) % 24;
  }

  public static DoubleUnaryOperator createLinearRegression(List<XYPoint> points) {
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumXX = 0;
    for (XYPoint p : points) {
      sumX += p.x();
      sumY += p.y();
      sumXY += p.x() * p.y();
      sumXX += p.x() * p.x();
    }
    int n = points.size();
    double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    double intercept = (sumY - slope * sumX) / n;
    return x -> intercept + slope * x;
  }
}
