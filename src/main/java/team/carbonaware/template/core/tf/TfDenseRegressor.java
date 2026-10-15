package team.carbonaware.template.core.tf;

import java.util.List;
import java.util.Random;

/**
 * CPU MLP with the same layer sizes as {@code main.ts} (256→128→64→32→1). Mirrors tfCommon training
 * without requiring TensorFlow Java graph building in workshop Main.java.
 */
public final class TfDenseRegressor {
  /** Base {@code TfNeuralNetwork} (28 → 256 → 128 → 64 → 32 → 1). */
  public static final int[] BASE_LAYER_SIZES = {
    TfCommon.FEATURE_COUNT, 256, 128, 64, 32, 1
  };

  /** {@code TfNeuralNetworkEnhanced} (7 inputs, same hidden stack). */
  public static final int[] ENHANCED_LAYER_SIZES = {
    TfEnhancedCommon.ENHANCED_FEATURE_COUNT, 256, 128, 64, 32, 1
  };

  /** {@code HeliosComposer} (12 → 512 → 256 → 128 → 64 → 1). */
  public static final int[] HELIOS_LAYER_SIZES = {
    HeliosCommon.HELIOS_FEATURE_COUNT, 512, 256, 128, 64, 1
  };

  private final int[] layerSizes;
  private final double[][][] weights;
  private final double[][] biases;
  private final Random rng;

  public TfDenseRegressor(int seed) {
    this(BASE_LAYER_SIZES, seed);
  }

  public TfDenseRegressor(int[] layerSizes, int seed) {
    this.layerSizes = layerSizes.clone();
    this.rng = new Random(seed);
    int layers = layerSizes.length - 1;
    weights = new double[layers][][];
    biases = new double[layers][];
    for (int l = 0; l < layers; l++) {
      int in = layerSizes[l];
      int out = layerSizes[l + 1];
      weights[l] = new double[out][in];
      biases[l] = new double[out];
      double scale = Math.sqrt(2.0 / in);
      for (int o = 0; o < out; o++) {
        for (int i = 0; i < in; i++) {
          weights[l][o][i] = rng.nextGaussian() * scale;
        }
        biases[l][o] = 0;
      }
    }
  }

  public void fit(List<double[]> xs, List<Double> ys, TfTrainingConfig config) {
    if (xs.isEmpty()) {
      throw new IllegalArgumentException("No training rows");
    }
    int n = xs.size();
    int batch = Math.min(config.batchSize(), n);
    double lr = config.learningRate();
    for (int epoch = 0; epoch < config.epochs(); epoch++) {
      for (int start = 0; start < n; start += batch) {
        int end = Math.min(start + batch, n);
        for (int i = start; i < end; i++) {
          double[] x = xs.get(i);
          double y = ys.get(i);
          trainOne(x, y, lr);
        }
      }
    }
  }

  public double predict(double[] x) {
    double[] h = x;
    int layers = weights.length;
    for (int l = 0; l < layers; l++) {
      h = forwardLayer(h, weights[l], biases[l], l < layers - 1);
    }
    return Math.max(0, h[0]);
  }

  private void trainOne(double[] x, double target, double lr) {
    double[][] acts = new double[layers() + 1][];
    acts[0] = x;
    for (int l = 0; l < layers(); l++) {
      acts[l + 1] = forwardLayer(acts[l], weights[l], biases[l], l < layers() - 1);
    }
    double[] delta = new double[] {acts[layers()][0] - target};
    for (int l = layers() - 1; l >= 0; l--) {
      double[] aPrev = acts[l];
      double[] aCur = acts[l + 1];
      double[] dNext = new double[aPrev.length];
      for (int o = 0; o < weights[l].length; o++) {
        double grad = delta[o];
        if (l < layers() - 1) {
          grad *= reluDeriv(aCur[o]);
        }
        biases[l][o] -= lr * grad;
        for (int i = 0; i < aPrev.length; i++) {
          weights[l][o][i] -= lr * grad * aPrev[i];
          dNext[i] += grad * weights[l][o][i];
        }
      }
      delta = dNext;
    }
  }

  private int layers() {
    return weights.length;
  }

  private static double[] forwardLayer(double[] in, double[][] w, double[] b, boolean relu) {
    double[] out = new double[w.length];
    for (int o = 0; o < w.length; o++) {
      double sum = b[o];
      for (int i = 0; i < in.length; i++) {
        sum += w[o][i] * in[i];
      }
      out[o] = relu ? relu(sum) : sum;
    }
    return out;
  }

  private static double relu(double v) {
    return Math.max(0, v);
  }

  private static double reluDeriv(double a) {
    return a > 0 ? 1 : 0;
  }
}
