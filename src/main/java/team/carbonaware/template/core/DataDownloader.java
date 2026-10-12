package team.carbonaware.template.core;

import java.util.List;

/** @deprecated Use HistoricStore.getTrain / getMeta. */
public final class DataDownloader {
  private DataDownloader() {}

  public static List<Types.HistoricDatasetInfo> listHistoricDatasets(HistoricStore store) throws Exception {
    return store.listDatasets();
  }

  public static Types.GhiHistoric loadHistoric(HistoricStore store, String datasetId) throws Exception {
    return store.getTrain(datasetId);
  }
}
