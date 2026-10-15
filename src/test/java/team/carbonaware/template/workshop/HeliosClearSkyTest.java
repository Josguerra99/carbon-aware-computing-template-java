package team.carbonaware.template.workshop;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import team.carbonaware.template.core.Types.GhiSlot;

class HeliosClearSkyTest {
  private static final double LAT = 14.6;
  private static final double LON = -90.5;

  private static GhiSlot slot(long ms, double ghi) {
    return new GhiSlot("test", Instant.ofEpochMilli(ms), ghi, LAT, LON, null, null);
  }

  @Test
  void solarGeometryNoonAndNight() {
    long noon = Instant.parse("2024-06-01T18:00:00Z").toEpochMilli();
    long night = Instant.parse("2024-06-01T06:00:00Z").toEpochMilli();
    assertTrue(SolarGeometry.cosSolarZenith(noon, LAT, LON) > 0.9);
    assertTrue(SolarGeometry.cosSolarZenith(night, LAT, LON) < 0);
    assertEquals(0, SolarGeometry.clearSkyHourMean(night, LAT, LON, -60), 1e-9);
  }

  @Test
  void constantClearSkyRatio() throws Exception {
    List<GhiSlot> rows = new ArrayList<>();
    long start = Instant.parse("2021-01-01T00:00:00Z").toEpochMilli();
    long end = Instant.parse("2024-01-01T00:00:00Z").toEpochMilli();
    for (long ms = start; ms < end; ms += 3_600_000) {
      double cs = SolarGeometry.clearSkyHourMean(ms, LAT, LON, -60);
      rows.add(slot(ms, 0.7 * cs));
    }
    HeliosClearSky model = new HeliosClearSky();
    model.train(rows);
    long noon = Instant.parse("2024-03-10T18:00:00Z").toEpochMilli();
    double pred = model.forecast(Instant.ofEpochMilli(noon), LAT, LON);
    double expected = 0.7 * SolarGeometry.clearSkyHourMean(noon, LAT, LON, -60);
    assertEquals(expected, pred, 1.0);
    assertEquals(0, model.forecast(Instant.parse("2024-03-10T06:00:00Z"), LAT, LON), 1e-9);
  }
}
