package team.carbonaware.template.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import team.carbonaware.template.config.Env;
import team.carbonaware.template.core.CacheBundle.CachedBundle;
import team.carbonaware.template.core.WorkshopClient.DatasetMeta;
import team.carbonaware.template.core.WorkshopClient.WorkshopManifest;

public final class HistoricStore {
  private final WorkshopClient client;
  private final Path cacheDir;
  private final long ttlMs;
  private final boolean refresh;
  private final Map<String, MemEntry> mem = new HashMap<>();
  private WorkshopManifest manifest;

  public HistoricStore(WorkshopClient client, Path cacheDir, long ttlMs, boolean refresh) {
    this.client = client;
    this.cacheDir = cacheDir;
    this.ttlMs = ttlMs;
    this.refresh = refresh;
  }

  public static HistoricStore createDefault() {
    Path root = Path.of("").toAbsolutePath();
    return new HistoricStore(
        WorkshopClient.createDefault(),
        root.resolve("cache"),
        Env.HISTORIC_CACHE_TTL_MS,
        Env.REFRESH_HISTORIC
    );
  }

  public DatasetMeta getMeta(String datasetId) throws Exception {
    WorkshopManifest current = ensureManifest();
    MemEntry cached = mem.get(datasetId);
    if (cached != null
        && cached.configId.equals(current.configId())
        && equalsNullable(cached.csvBundleHash, current.csvBundleHash())
        && cached.meta != null
        && hasCoords(cached.meta.latitude(), cached.meta.longitude())
        && fresh(cached.metaFetchedAt, ttlMs)) {
      return cached.meta;
    }
    if (!refresh) {
      JsonNode disk = readJsonFile(cacheDir.resolve("meta").resolve(datasetId + ".json"));
      if (disk != null && cacheBundleMatches(diskEnvelope(disk), current) && disk.has("payload")) {
        DatasetMeta payload = JsonSupport.MAPPER.treeToValue(disk.get("payload"), DatasetMeta.class);
        if (hasCoords(payload.latitude(), payload.longitude()) && fresh(disk.path("fetchedAt").asLong(0), ttlMs)) {
          MemEntry row = entry(datasetId, current.configId(), current.csvBundleHash());
          row.meta = payload;
          row.metaFetchedAt = disk.path("fetchedAt").asLong(0);
          return payload;
        }
      }
    }
    DatasetMeta payload = client.fetchMeta(datasetId);
    if (!hasCoords(payload.latitude(), payload.longitude())) {
      throw new RuntimeException("El meta de " + datasetId + " no trae latitude/longitude");
    }
    long fetchedAt = System.currentTimeMillis();
    MemEntry row = entry(datasetId, current.configId(), current.csvBundleHash());
    row.meta = payload;
    row.metaFetchedAt = fetchedAt;
    ObjectNode envelope = JsonSupport.MAPPER.createObjectNode();
    envelope.put("configId", current.configId());
    if (current.csvBundleHash() != null) {
      envelope.put("csvBundleHash", current.csvBundleHash());
    }
    if (payload.csvFingerprint() != null) {
      envelope.put("csvFingerprint", payload.csvFingerprint());
    }
    envelope.put("fetchedAt", fetchedAt);
    envelope.set("payload", JsonSupport.MAPPER.valueToTree(payload));
    writeJsonFile(cacheDir.resolve("meta").resolve(datasetId + ".json"), envelope);
    return payload;
  }

  public Types.GhiHistoric getTrain(String datasetId) throws Exception {
    WorkshopManifest current = ensureManifest();
    MemEntry cached = mem.get(datasetId);
    if (cached != null
        && cached.configId.equals(current.configId())
        && equalsNullable(cached.csvBundleHash, current.csvBundleHash())
        && cached.train != null
        && fresh(cached.trainFetchedAt, ttlMs)) {
      return cached.train;
    }
    if (!refresh) {
      JsonNode disk = readJsonFile(cacheDir.resolve("train").resolve(datasetId + ".json"));
      if (disk != null && cacheBundleMatches(diskEnvelope(disk), current) && disk.has("payload")) {
        Types.GhiSeriesJson payload = JsonSupport.MAPPER.treeToValue(disk.get("payload"), Types.GhiSeriesJson.class);
        if (hasCoords(payload.latitude(), payload.longitude())
            && payload.slots() != null
            && fresh(disk.path("fetchedAt").asLong(0), ttlMs)) {
          Types.GhiHistoric historic = toHistoric(payload);
          MemEntry row = entry(datasetId, current.configId(), current.csvBundleHash());
          row.train = historic;
          row.trainFetchedAt = disk.path("fetchedAt").asLong(0);
          if (row.meta == null) {
            row.meta = new DatasetMeta(
                null,
                historic.datasetId() != null ? historic.datasetId() : datasetId,
                null,
                null,
                null,
                historic.latitude(),
                historic.longitude(),
                null,
                null,
                null
            );
            row.metaFetchedAt = disk.path("fetchedAt").asLong(0);
          }
          return historic;
        }
      }
    }
    Types.GhiSeriesJson payload = client.fetchTrainSeries(datasetId);
    if (!hasCoords(payload.latitude(), payload.longitude())) {
      throw new RuntimeException("El histórico de " + datasetId + " no trae latitude/longitude");
    }
    Types.GhiHistoric historic = toHistoric(payload);
    long fetchedAt = System.currentTimeMillis();
    MemEntry row = entry(datasetId, current.configId(), current.csvBundleHash());
    row.train = historic;
    row.trainFetchedAt = fetchedAt;
    row.meta = new DatasetMeta(
        current.configId(),
        historic.datasetId() != null ? historic.datasetId() : datasetId,
        null,
        null,
        null,
        historic.latitude(),
        historic.longitude(),
        null,
        null,
        payload.csvFingerprint()
    );
    row.metaFetchedAt = fetchedAt;
    ObjectNode envelope = JsonSupport.MAPPER.createObjectNode();
    envelope.put("configId", current.configId());
    if (current.csvBundleHash() != null) {
      envelope.put("csvBundleHash", current.csvBundleHash());
    }
    if (payload.csvFingerprint() != null) {
      envelope.put("csvFingerprint", payload.csvFingerprint());
    }
    envelope.put("fetchedAt", fetchedAt);
    envelope.set("payload", JsonSupport.MAPPER.valueToTree(payload));
    writeJsonFile(cacheDir.resolve("train").resolve(datasetId + ".json"), envelope);
    return historic;
  }

  public List<Types.HistoricDatasetInfo> listDatasets() throws Exception {
    ensureManifest();
    return client.listDatasets();
  }

  private WorkshopManifest ensureManifest() throws Exception {
    if (manifest != null) {
      return manifest;
    }
    WorkshopManifest remote = client.fetchManifest();
    JsonNode disk = refresh ? null : readJsonFile(cacheDir.resolve("manifest.json"));
    CachedBundle diskBundle = disk == null ? null : diskEnvelope(disk);
    if (CacheBundle.manifestCacheStale(diskBundle, remote)) {
      mem.clear();
      clearDisk();
    }
    if (refresh) {
      mem.clear();
    }
    manifest = remote;
    writeJsonFile(cacheDir.resolve("manifest.json"), JsonSupport.MAPPER.valueToTree(remote));
    return remote;
  }

  private MemEntry entry(String datasetId, String configId, String csvBundleHash) {
    MemEntry current = mem.get(datasetId);
    if (current != null
        && current.configId.equals(configId)
        && (csvBundleHash == null || csvBundleHash.equals(current.csvBundleHash))) {
      return current;
    }
    MemEntry next = new MemEntry(configId, csvBundleHash);
    mem.put(datasetId, next);
    return next;
  }

  private static boolean cacheBundleMatches(CachedBundle disk, WorkshopManifest remote) {
    return CacheBundle.cacheBundleMatches(disk, remote);
  }

  private static CachedBundle diskEnvelope(JsonNode disk) {
    return new CachedBundle(disk.path("configId").asText(""), optional(disk, "csvBundleHash"));
  }

  private static String optional(JsonNode node, String field) {
    JsonNode value = node.get(field);
    return value == null || value.isNull() ? null : value.asText();
  }

  private static boolean hasCoords(double latitude, double longitude) {
    return Double.isFinite(latitude) && Double.isFinite(longitude);
  }

  private static boolean fresh(long fetchedAt, long ttlMs) {
    if (fetchedAt <= 0) {
      return false;
    }
    if (ttlMs <= 0) {
      return true;
    }
    return System.currentTimeMillis() - fetchedAt < ttlMs;
  }

  private static boolean equalsNullable(String a, String b) {
    return a == null ? b == null : a.equals(b);
  }

  private static Types.GhiHistoric toHistoric(Types.GhiSeriesJson payload) {
    String datasetId = payload.datasetId() != null ? payload.datasetId() : "unknown";
    List<Types.GhiSlot> slots = new ArrayList<>();
    if (payload.slots() != null) {
      for (Types.GhiSlotJson slot : payload.slots()) {
        String raw = slot.time() != null ? slot.time() : slot.start();
        if (raw == null || raw.isBlank()) {
          continue;
        }
        Instant time;
        try {
          time = Instant.parse(raw);
        } catch (Exception ignored) {
          continue;
        }
        double ghi = slot.ghiWm2() != null ? slot.ghiWm2() : 0.0;
        slots.add(new Types.GhiSlot(
            datasetId,
            time,
            ghi,
            payload.latitude(),
            payload.longitude(),
            slot.cloudCoverPct(),
            slot.temperatureC()
        ));
      }
    }
    return new Types.GhiHistoric(payload.datasetId(), payload.latitude(), payload.longitude(), slots);
  }

  private JsonNode readJsonFile(Path path) {
    try {
      if (!Files.isRegularFile(path)) {
        return null;
      }
      return JsonSupport.MAPPER.readTree(Files.readString(path));
    } catch (IOException e) {
      return null;
    }
  }

  private void writeJsonFile(Path path, JsonNode payload) throws IOException {
    Files.createDirectories(path.getParent());
    Files.writeString(path, JsonSupport.MAPPER.writeValueAsString(payload));
  }

  private void clearDisk() throws IOException {
    deleteDir(cacheDir.resolve("meta"));
    deleteDir(cacheDir.resolve("train"));
  }

  private static void deleteDir(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    try (var paths = Files.walk(dir)) {
      paths.sorted(Comparator.reverseOrder()).forEach(p -> {
        try {
          Files.deleteIfExists(p);
        } catch (IOException ignored) {
        }
      });
    }
  }

  private static final class MemEntry {
    final String configId;
    final String csvBundleHash;
    DatasetMeta meta;
    long metaFetchedAt;
    Types.GhiHistoric train;
    long trainFetchedAt;

    MemEntry(String configId, String csvBundleHash) {
      this.configId = configId;
      this.csvBundleHash = csvBundleHash;
    }
  }
}
