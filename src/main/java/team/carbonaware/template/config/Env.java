package team.carbonaware.template.config;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class Env {
  private static final Map<String, String> DOT_ENV = loadDotEnv(Path.of(".env"));

  public static final String MAIN_SERVER_URL = get("MAIN_SERVER_URL", "http://192.168.0.232:3915").replaceAll("/$", "");
  public static final int PORT = parseInt(get("PORT", "3916"), 3916);
  public static final String TRAIN_DATASETS = get("TRAIN_DATASETS", "all").trim();
  public static final long HISTORIC_CACHE_TTL_MS = parseLong(get("HISTORIC_CACHE_TTL_MS", "0"), 0);
  public static final boolean REFRESH_HISTORIC = isTruthy(get("REFRESH_HISTORIC", "0"));

  private Env() {}

  public static String get(String key, String defaultValue) {
    String fromEnv = System.getenv(key);
    if (fromEnv != null && !fromEnv.isBlank()) {
      return fromEnv;
    }
    String fromFile = DOT_ENV.get(key);
    if (fromFile != null && !fromFile.isBlank()) {
      return fromFile;
    }
    return defaultValue;
  }

  private static Map<String, String> loadDotEnv(Path path) {
    if (!Files.isRegularFile(path)) {
      return Map.of();
    }
    try {
      Map<String, String> out = new HashMap<>();
      for (String line : Files.readAllLines(path)) {
        String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        int eq = trimmed.indexOf('=');
        if (eq <= 0) {
          continue;
        }
        out.put(trimmed.substring(0, eq).trim(), trimmed.substring(eq + 1).trim());
      }
      return Collections.unmodifiableMap(out);
    } catch (Exception e) {
      return Map.of();
    }
  }

  private static int parseInt(String raw, int fallback) {
    try {
      return Integer.parseInt(raw);
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static long parseLong(String raw, long fallback) {
    try {
      long value = Long.parseLong(raw);
      return value > 0 ? value : 0;
    } catch (NumberFormatException e) {
      return fallback;
    }
  }

  private static boolean isTruthy(String raw) {
    String v = raw.trim().toLowerCase();
    return v.equals("1") || v.equals("true") || v.equals("yes");
  }
}
