package team.carbonaware.template.server;

import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import team.carbonaware.template.Main;
import team.carbonaware.template.config.Env;
import team.carbonaware.template.core.DebugLogs;
import team.carbonaware.template.core.HistoricStore;
import team.carbonaware.template.core.LoadData;
import team.carbonaware.template.core.WorkshopClient;

public final class ServerApp {
  private static final long HOUR_MS = 3_600_000;
  private static final int MAX_HOURS = 24 * 100;
  private static final int FORECAST_CONCURRENCY = 16;

  private ServerApp() {}

  public static void main(String[] args) throws Exception {
    HistoricStore historicStore = HistoricStore.createDefault();
    System.out.println("Training prediction model...");
    Main.WORKSHOP_MODEL.train(LoadData.load(historicStore));
    System.out.println("Prediction model trained");
    printStartupBanner();

    Javalin app = Javalin.create(config -> config.showJavalinBanner = false);
    app.before(ctx -> ctx.header("Access-Control-Allow-Origin", "*"));
    app.options("/*", ctx -> ctx.status(HttpStatus.OK));
    app.exception(Exception.class, ServerApp::handleUncaught);

    app.get("/", ctx -> ctx.html(
        "<!DOCTYPE html><html><head><meta charset=\"utf-8\">"
            + "<title>Hello world</title></head><body><h1>Hello world</h1></body></html>"
    ));

    app.get("/forecast/{datasetId}", ctx -> handleForecast(ctx, historicStore));

    app.start("0.0.0.0", Env.PORT);
  }

  private static void handleForecast(Context ctx, HistoricStore historicStore) throws Exception {
    String datasetId = ctx.pathParam("datasetId");
    Instant start = parseUtc(ctx.queryParam("start"));
    Instant end = parseUtc(ctx.queryParam("end"));
    if (start == null || end == null || !end.isAfter(start)) {
      ctx.status(HttpStatus.BAD_REQUEST).json(Map.of(
          "error", "start and end are required and end must be after start"
      ));
      return;
    }
    List<Instant> hours = hourlyUtcRange(start, end);
    if (hours.size() > MAX_HOURS) {
      ctx.status(HttpStatus.BAD_REQUEST).json(Map.of("error", "range too long"));
      return;
    }
    double latitude;
    double longitude;
    try {
      WorkshopClient.DatasetMeta place = historicStore.getMeta(datasetId);
      latitude = place.latitude();
      longitude = place.longitude();
    } catch (Exception e) {
      System.out.println("No se pudo leer el histórico de " + datasetId);
      e.printStackTrace(System.out);
      ctx.status(HttpStatus.BAD_GATEWAY).json(Map.of(
          "error", "No se pudo leer el histórico de " + datasetId + ": " + rootMessage(e)
      ));
      return;
    }
    DebugLogs.allowDebugLogs = true;
    List<Map<String, Object>> slots = new ArrayList<>();
    try {
      final double lat = latitude;
      final double lon = longitude;
      slots = LoadData.mapPool(hours, FORECAST_CONCURRENCY, timestamp -> {
        double value = Main.WORKSHOP_MODEL.forecast(timestamp, lat, lon);
        Map<String, Object> slot = new HashMap<>();
        slot.put("time", DateTimeFormatter.ISO_INSTANT.format(timestamp));
        slot.put("predWm2", Double.isFinite(value) ? value : 0.0);
        return slot;
      });
    } finally {
      DebugLogs.allowDebugLogs = false;
    }
    ctx.json(Map.of("datasetId", datasetId, "slots", slots));
  }

  private static void handleUncaught(Exception e, Context ctx) {
    Throwable root = rootCause(e);
    System.out.println("Error en " + ctx.method() + " " + ctx.path() + ": " + root);
    e.printStackTrace(System.out);
    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json(Map.of(
        "error", root.getClass().getName() + ": " + rootMessage(root)
    ));
  }

  private static Throwable rootCause(Throwable error) {
    Throwable root = error;
    while (root.getCause() != null && root.getCause() != root) {
      root = root.getCause();
    }
    return root;
  }

  private static String rootMessage(Throwable error) {
    Throwable root = rootCause(error);
    String message = root.getMessage();
    return message == null || message.isBlank() ? root.getClass().getSimpleName() : message;
  }

  private static Instant parseUtc(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    try {
      return Instant.parse(value);
    } catch (Exception e) {
      return null;
    }
  }

  private static List<Instant> hourlyUtcRange(Instant start, Instant end) {
    Instant first = start.atZone(ZoneOffset.UTC).truncatedTo(ChronoUnit.HOURS).toInstant();
    List<Instant> times = new ArrayList<>();
    for (Instant stamp = first; stamp.isBefore(end); stamp = stamp.plusMillis(HOUR_MS)) {
      times.add(stamp);
    }
    return times;
  }

  private static void printStartupBanner() {
    System.out.println("Template forecast en http://localhost:" + Env.PORT);
    System.out.println("Inicio: http://localhost:" + Env.PORT + "/");
    System.out.println(
        "Ejemplo: http://localhost:" + Env.PORT + "/forecast/chiquimula?"
            + "start=2026-08-03T00:00:00.000Z&end=2026-09-02T00:00:00.000Z"
    );
    System.out.println("Histórico: " + Env.MAIN_SERVER_URL + "/api/historic/ghi/:datasetid");
  }
}
