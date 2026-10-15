package team.carbonaware.template.core.tf;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class TfDenseRegressorTest {
  @Test
  void fitsAndPredictsNonNegative() {
    TfDenseRegressor reg = new TfDenseRegressor(42);
    List<double[]> xs =
        List.of(
            new double[28],
            new double[] {
              1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 100, 14, -90,
              2024
            });
    xs.get(1)[0] = 1;
    reg.fit(xs, List.of(0.0, 500.0), new TfTrainingConfig(10, 2, 2, 0.01, 0, 1));
    double y = reg.predict(xs.get(1));
    assertTrue(y >= 0);
  }
}
