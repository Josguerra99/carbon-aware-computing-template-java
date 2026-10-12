package team.carbonaware.template.core;

import java.time.Instant;
import java.util.List;

public final class Types {
  private Types() {}

  public record GhiSlotJson(String time, String start, Double ghiWm2, Double cloudCoverPct, Double temperatureC) {}

  public record GhiSeriesJson(
      String datasetId,
      String location,
      double latitude,
      double longitude,
      String unit,
      String source,
      List<GhiSlotJson> slots,
      String csvFingerprint
  ) {}

  public record HistoricDatasetInfo(
      String id,
      String name,
      String city,
      String country,
      double latitude,
      double longitude,
      String timezone,
      String kind
  ) {}

  public record GhiSlot(
      String datasetId,
      Instant time,
      double ghiWm2,
      double latitude,
      double longitude,
      Double cloudCoverPct,
      Double temperatureC
  ) {}

  public record GhiHistoric(String datasetId, double latitude, double longitude, List<GhiSlot> slots) {}

  public record ForecastPoint(String time, double predWm2) {}
}
