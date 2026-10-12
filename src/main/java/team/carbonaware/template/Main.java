package team.carbonaware.template;

import java.time.Instant;
import java.util.List;
import team.carbonaware.template.core.PredictionModel;
import team.carbonaware.template.core.Types;

public final class Main {
  public static final PredictionModel WORKSHOP_MODEL = new SimplePredictionModel();

  private Main() {}

  public static final class SimplePredictionModel extends PredictionModel {
    @Override
    public void train(List<Types.GhiSlot> trainingData) {
    }

    @Override
    public double forecast(Instant timestamp, double latitude, double longitude) {
      return 0.0;
    }
  }
}
