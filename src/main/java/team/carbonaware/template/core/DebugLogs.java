package team.carbonaware.template.core;

public final class DebugLogs {
  public static boolean allowDebugLogs = false;

  private DebugLogs() {}

  public static void log(String message) {
    if (allowDebugLogs) {
      System.out.println(message);
    }
  }
}
