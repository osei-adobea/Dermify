
package com.dermify.dermify.ml;

import android.graphics.Bitmap;
import android.graphics.Color;

public final class HeatmapMath {

    private HeatmapMath() {}

    public static float[][] computeHeatmap(float[][][][] features, float[] classWeights) {
        int gridSize = features[0].length;
        int numChannels = classWeights.length;

        float[][] heatmap = new float[gridSize][gridSize];
        float maxVal = 0f;

        for (int y = 0; y < gridSize; y++) {
            for (int x = 0; x < gridSize; x++) {
                float sum = 0f;
                for (int c = 0; c < numChannels; c++) {
                    sum += features[0][y][x][c] * classWeights[c];
                }
                float relu = Math.max(sum, 0f);
                heatmap[y][x] = relu;
                if (relu > maxVal) maxVal = relu;
            }
        }

        if (maxVal > 0f) {
            for (int y = 0; y < gridSize; y++) {
                for (int x = 0; x < gridSize; x++) {
                    heatmap[y][x] /= maxVal;
                }
            }
        }
        return heatmap;
    }

    public static float[][] bilinearResize(float[][] src, int targetW, int targetH) {
        int srcH = src.length, srcW = src[0].length;
        float[][] dst = new float[targetH][targetW];

        for (int y = 0; y < targetH; y++) {
            float srcY = (y + 0.5f) * srcH / targetH - 0.5f;
            int y0 = (int) Math.floor(srcY);
            int y1 = Math.min(y0 + 1, srcH - 1);
            y0 = Math.max(y0, 0);
            float wy = srcY - y0;

            for (int x = 0; x < targetW; x++) {
                float srcX = (x + 0.5f) * srcW / targetW - 0.5f;
                int x0 = (int) Math.floor(srcX);
                int x1 = Math.min(x0 + 1, srcW - 1);
                x0 = Math.max(x0, 0);
                float wx = srcX - x0;

                float top = src[y0][x0] * (1 - wx) + src[y0][x1] * wx;
                float bottom = src[y1][x0] * (1 - wx) + src[y1][x1] * wx;
                dst[y][x] = top * (1 - wy) + bottom * wy;
            }
        }
        return dst;
    }

    public static int jetColor(float value) {
        float r, g, b;
        if (value < 0.35f) {
            r = 0; g = value / 0.35f; b = 1;
        } else if (value < 0.65f) {
            r = (value - 0.35f) / 0.3f; g = 1; b = 1 - r;
        } else {
            r = 1; g = 1 - (value - 0.65f) / 0.35f; b = 0;
        }
        return Color.rgb((int) (r * 255), (int) (g * 255), (int) (b * 255));
    }

    public static Bitmap overlayHeatmap(Bitmap original, float[][] heatmap, float alpha) {
        int w = original.getWidth();
        int h = original.getHeight();
        Bitmap result = original.copy(Bitmap.Config.ARGB_8888, true);

        int[] pixels = new int[w * h];
        result.getPixels(pixels, 0, w, 0, 0, w, h);

        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int idx = y * w + x;
                int heatColor = jetColor(heatmap[y][x]);
                int orig = pixels[idx];

                int r = (int) (Color.red(heatColor) * alpha + Color.red(orig) * (1 - alpha));
                int g = (int) (Color.green(heatColor) * alpha + Color.green(orig) * (1 - alpha));
                int b = (int) (Color.blue(heatColor) * alpha + Color.blue(orig) * (1 - alpha));
                pixels[idx] = Color.rgb(r, g, b);
            }
        }

        result.setPixels(pixels, 0, w, 0, 0, w, h);
        return result;
    }
}