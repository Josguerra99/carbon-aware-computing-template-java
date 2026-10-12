package team.carbonaware.template.core;

import java.time.Instant;
import java.util.List;

public abstract class PredictionModel {
  public abstract void train(List<Types.GhiSlot> trainingData) throws Exception;

  public abstract double forecast(Instant timestamp, double latitude, double longitude) throws Exception;
}
