package team.carbonaware.template.core;

import team.carbonaware.template.core.WorkshopClient.WorkshopManifest;

public final class CacheBundle {
  private CacheBundle() {}

  public static boolean cacheBundleMatches(CachedBundle disk, WorkshopManifest remote) {
    if (disk == null || !disk.configId().equals(remote.configId())) {
      return false;
    }
    if (remote.csvBundleHash() != null && !remote.csvBundleHash().equals(disk.csvBundleHash())) {
      return false;
    }
    return true;
  }

  public static boolean manifestCacheStale(CachedBundle disk, WorkshopManifest remote) {
    return disk != null && !cacheBundleMatches(disk, remote);
  }

  public record CachedBundle(String configId, String csvBundleHash) {}
}
