package team.carbonaware.template.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import team.carbonaware.template.core.WorkshopClient.DatasetMeta;
import team.carbonaware.template.core.WorkshopClient.WorkshopManifest;

class HistoricStoreTest {
  @TempDir
  Path cacheDir;

  @Test
  void refetchesTrainSeriesWhenConfigIdChanges() throws Exception {
    FakeClient client = new FakeClient();
    HistoricStore first = new HistoricStore(client, cacheDir, 0, false);
    assertEquals(1, first.getTrain("mexico").slots().size());
    assertEquals(1, client.trainFetches);

    HistoricStore cached = new HistoricStore(client, cacheDir, 0, false);
    cached.getTrain("mexico");
    assertEquals(1, client.trainFetches);

    client.configId = "level-b";
    HistoricStore next = new HistoricStore(client, cacheDir, 0, false);
    next.getTrain("mexico");
    assertEquals(2, client.trainFetches);
  }

  @Test
  void refetchesTrainSeriesWhenCsvBundleHashChanges() throws Exception {
    FakeClient client = new FakeClient();
    HistoricStore first = new HistoricStore(client, cacheDir, 0, false);
    first.getTrain("mexico");
    assertEquals(1, client.trainFetches);

    HistoricStore cached = new HistoricStore(client, cacheDir, 0, false);
    cached.getTrain("mexico");
    assertEquals(1, client.trainFetches);

    client.bundle = "csv-v2";
    HistoricStore next = new HistoricStore(client, cacheDir, 0, false);
    next.getTrain("mexico");
    assertEquals(2, client.trainFetches);
  }

  static final class FakeClient implements WorkshopClient {
    String configId = "level-a";
    String bundle = "csv-v1";
    int trainFetches;

    @Override
    public WorkshopManifest fetchManifest() {
      return new WorkshopManifest(configId, configId, 1, configId, bundle, "2026-01-01T00:00:00.000Z");
    }

    @Override
    public DatasetMeta fetchMeta(String datasetId) {
      return new DatasetMeta(null, datasetId, null, null, null, 19.43, -99.13, null, null, null);
    }

    @Override
    public Types.GhiSeriesJson fetchTrainSeries(String datasetId) {
      trainFetches += 1;
      return new Types.GhiSeriesJson(
          "mexico",
          "CDMX",
          19.43,
          -99.13,
          "W/m²",
          configId,
          List.of(new Types.GhiSlotJson("2026-01-01T06:00:00.000Z", null, 10.0, null, null)),
          null
      );
    }

    @Override
    public List<Types.HistoricDatasetInfo> listDatasets() {
      return List.of();
    }
  }
}
