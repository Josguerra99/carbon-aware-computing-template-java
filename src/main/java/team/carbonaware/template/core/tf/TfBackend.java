package team.carbonaware.template.core.tf;

/** Mirrors Node {@code ensureTfBackend} logging (Java template uses TensorFlow Java when wired). */
public final class TfBackend {
  private TfBackend() {}

  public static String ensureTfBackend() {
    String pref = System.getenv("TFJS_BACKEND");
    if (pref == null) {
      pref = System.getenv("TF_BACKEND");
    }
    pref = pref == null ? "" : pref.trim().toLowerCase();
    String name = pref.equals("cpu") ? "cpu" : "auto";
    System.out.println("TensorFlow backend: " + name + " (configure tf-cpu / tf-gpu Maven profile)");
    return name;
  }
}
