package team.carbonaware.template.core;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import team.carbonaware.template.config.Env;

public interface WorkshopClient {
  WorkshopManifest fetchManifest() throws Exception;

  DatasetMeta fetchMeta(String datasetId) throws Exception;

  Types.GhiSeriesJson fetchTrainSeries(String datasetId) throws Exception;

  List<Types.HistoricDatasetInfo> listDatasets() throws Exception;

  record WorkshopManifest(
      String configId,
      String configName,
      int scenarioCount,
      String datasetsHash,
      String csvBundleHash,
      String generatedAt
  ) {}

  record DatasetMeta(
      String configId,
      String datasetId,
      String name,
      String city,
      String country,
      double latitude,
      double longitude,
      String timezone,
      String kind,
      String csvFingerprint
  ) {}

  static WorkshopClient create(String baseUrl) {
    return new HttpWorkshopClient(baseUrl.replaceAll("/$", ""));
  }

  static WorkshopClient createDefault() {
    return create(Env.MAIN_SERVER_URL);
  }

  final class HttpWorkshopClient implements WorkshopClient {
    private final String main;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(60)).build();

    HttpWorkshopClient(String main) {
      this.main = main;
    }

    private JsonNode readJson(String url) throws Exception {
      HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
      HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() >= 400) {
        throw new RuntimeException("No se pudo leer " + url + " (HTTP " + response.statusCode() + ")");
      }
      return JsonSupport.MAPPER.readTree(response.body());
    }

    @Override
    public WorkshopManifest fetchManifest() throws Exception {
      JsonNode node = readJson(main + "/api/workshop/manifest");
      return new WorkshopManifest(
          text(node, "configId"),
          text(node, "configName"),
          node.path("scenarioCount").asInt(0),
          text(node, "datasetsHash"),
          optionalText(node, "csvBundleHash"),
          text(node, "generatedAt")
      );
    }

    @Override
    public DatasetMeta fetchMeta(String datasetId) throws Exception {
      String encoded = URLEncoder.encode(datasetId, StandardCharsets.UTF_8);
      JsonNode node = readJson(main + "/api/workshop/datasets/" + encoded + "/meta");
      return new DatasetMeta(
          optionalText(node, "configId"),
          optionalText(node, "datasetId"),
          optionalText(node, "name"),
          optionalText(node, "city"),
          optionalText(node, "country"),
          node.path("latitude").asDouble(Double.NaN),
          node.path("longitude").asDouble(Double.NaN),
          optionalText(node, "timezone"),
          optionalText(node, "kind"),
          optionalText(node, "csvFingerprint")
      );
    }

    @Override
    public Types.GhiSeriesJson fetchTrainSeries(String datasetId) throws Exception {
      String encoded = URLEncoder.encode(datasetId, StandardCharsets.UTF_8);
      JsonNode node = readJson(main + "/api/historic/ghi/" + encoded + "/train");
      return JsonSupport.MAPPER.treeToValue(node, Types.GhiSeriesJson.class);
    }

    @Override
    public List<Types.HistoricDatasetInfo> listDatasets() throws Exception {
      JsonNode node = readJson(main + "/api/historic/ghi");
      JsonNode datasets = node.path("datasets");
      List<Types.HistoricDatasetInfo> out = new ArrayList<>();
      if (!datasets.isArray()) {
        return out;
      }
      for (JsonNode item : datasets) {
        String id = optionalText(item, "id");
        if (id == null || id.isBlank()) {
          continue;
        }
        out.add(new Types.HistoricDatasetInfo(
            id,
            optionalText(item, "name"),
            optionalText(item, "city"),
            optionalText(item, "country"),
            item.path("latitude").asDouble(0),
            item.path("longitude").asDouble(0),
            optionalText(item, "timezone"),
            optionalText(item, "kind")
        ));
      }
      return out;
    }

    private static String text(JsonNode node, String field) {
      JsonNode value = node.get(field);
      return value == null || value.isNull() ? "" : value.asText();
    }

    private static String optionalText(JsonNode node, String field) {
      JsonNode value = node.get(field);
      return value == null || value.isNull() ? null : value.asText();
    }
  }
}
