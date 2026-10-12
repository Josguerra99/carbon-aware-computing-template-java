package team.carbonaware.template.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import team.carbonaware.template.config.Env;

public final class LoadData {
  private static final int TRAIN_CONCURRENCY = 4;

  private LoadData() {}

  public static List<Types.GhiSlot> load(HistoricStore store) throws Exception {
    Set<String> allowed = datasetFilter();
    List<Types.HistoricDatasetInfo> datasets = store.listDatasets();
    List<Types.HistoricDatasetInfo> selected = new ArrayList<>();
    for (Types.HistoricDatasetInfo dataset : datasets) {
      if (allowed == null || allowed.contains(dataset.id())) {
        selected.add(dataset);
      }
    }
    if (selected.isEmpty()) {
      throw new RuntimeException("No hay datasets de entrenamiento (revisa TRAIN_DATASETS)");
    }
    try (ExecutorService pool = Executors.newFixedThreadPool(Math.min(TRAIN_CONCURRENCY, selected.size()))) {
      List<Future<List<Types.GhiSlot>>> futures = new ArrayList<>();
      for (Types.HistoricDatasetInfo dataset : selected) {
        futures.add(pool.submit(() -> store.getTrain(dataset.id()).slots()));
      }
      List<Types.GhiSlot> flat = new ArrayList<>();
      for (Future<List<Types.GhiSlot>> future : futures) {
        flat.addAll(future.get());
      }
      return flat;
    }
  }

  private static Set<String> datasetFilter() {
    String raw = Env.TRAIN_DATASETS;
    if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("all") || raw.equals("*")) {
      return null;
    }
    Set<String> ids = new HashSet<>();
    for (String part : raw.split(",")) {
      String trimmed = part.trim();
      if (!trimmed.isEmpty()) {
        ids.add(trimmed);
      }
    }
    return ids.isEmpty() ? null : ids;
  }

  public static <T, R> List<R> mapPool(List<T> items, int concurrency, PoolTask<T, R> fn) throws Exception {
    if (items.isEmpty()) {
      return List.of();
    }
    List<R> results = new ArrayList<>(items.size());
    for (int i = 0; i < items.size(); i++) {
      results.add(null);
    }
    try (ExecutorService pool = Executors.newFixedThreadPool(Math.min(concurrency, items.size()))) {
      List<Future<Void>> futures = new ArrayList<>();
      for (int i = 0; i < items.size(); i++) {
        final int index = i;
        futures.add(pool.submit(() -> {
          results.set(index, fn.apply(items.get(index)));
          return null;
        }));
      }
      for (Future<Void> future : futures) {
        future.get();
      }
    }
    return results;
  }

  @FunctionalInterface
  public interface PoolTask<T, R> {
    R apply(T item) throws Exception;
  }
}
