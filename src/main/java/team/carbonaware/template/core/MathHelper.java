package team.carbonaware.template.core;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.DoubleUnaryOperator;
import java.util.function.ToDoubleFunction;

public final class MathHelper {
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

  public static double solarHour(Instant timestamp, double longitude) {
    var utc = timestamp.atZone(ZoneOffset.UTC);
    double utcHours = utc.getHour() + utc.getMinute() / 60.0 + utc.getSecond() / 3600.0;
    return Math.round(((utcHours + longitude / 15) % 24 + 24) % 24) % 24;
  }

  public static DoubleUnaryOperator createLinearRegression(List<double[]> points) {
    double sumX = 0;
    double sumY = 0;
    double sumXY = 0;
    double sumXX = 0;
    for (double[] p : points) {
      sumX += p[0];
      sumY += p[1];
      sumXY += p[0] * p[1];
      sumXX += p[0] * p[0];
    }
    int n = points.size();
    double slope = (n * sumXY - sumX * sumY) / (n * sumXX - sumX * sumX);
    double intercept = (sumY - slope * sumX) / n;
    return x -> intercept + slope * x;
  }
}
