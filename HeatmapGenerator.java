package com.dermify.dermify.ml;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;

import org.json.JSONException;
import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.HashMap;
import java.util.Map;

public class HeatmapGenerator {

    private static final int GRID_SIZE = 7;
    private static final int NUM_CHANNELS = 1280;
    private static final int IMAGE_SIZE = ImagePreprocessor.IMAGE_SIZE;

    private final Interpreter interpreter;
    private final CamClassWeights classWeights;
    private int predOutputIndex = -1;
    private int featOutputIndex = -1;

    public HeatmapGenerator(Context context, String modelAssetName, String weightsAssetName)
            throws IOException, JSONException {
        interpreter = new Interpreter(loadModelFile(context, modelAssetName));
        classWeights = CamClassWeights.loadFromAssets(context, weightsAssetName);

        for (int i = 0; i < interpreter.getOutputTensorCount(); i++) {
            int[] shape = interpreter.getOutputTensor(i).shape();
            if (shape.length == 2) {
                predOutputIndex = i;
            } else if (shape.length == 4) {
                featOutputIndex = i;
            }
        }
        if (predOutputIndex == -1 || featOutputIndex == -1) {
            throw new IllegalStateException("Could not identify prediction/feature-map outputs.");
        }
    }

    private MappedByteBuffer loadModelFile(Context context, String modelName) throws IOException {
        AssetFileDescriptor fd = context.getAssets().openFd(modelName);
        FileInputStream inputStream = new FileInputStream(fd.getFileDescriptor());
        FileChannel fileChannel = inputStream.getChannel();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fd.getStartOffset(), fd.getDeclaredLength());
    }

    public Result predictWithHeatmap(Bitmap originalBitmap) {
        Bitmap resized = Bitmap.createScaledBitmap(originalBitmap, IMAGE_SIZE, IMAGE_SIZE, true);

        float[][] predictions = new float[1][classWeights.classNames.length];
        float[][][][] features = new float[1][GRID_SIZE][GRID_SIZE][NUM_CHANNELS];

        Object[] inputs = { ImagePreprocessor.bitmapToByteBufferRaw(resized) };
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(predOutputIndex, predictions);
        outputs.put(featOutputIndex, features);

        interpreter.runForMultipleInputsOutputs(inputs, outputs);

        int predictedClassIdx = argMax(predictions[0]);
        float confidence = predictions[0][predictedClassIdx];
        float[] weightsForClass = classWeights.weights[predictedClassIdx];

        float[][] heatmapSmall = HeatmapMath.computeHeatmap(features, weightsForClass);
        float[][] heatmapFull = HeatmapMath.bilinearResize(heatmapSmall, IMAGE_SIZE, IMAGE_SIZE);
        Bitmap overlay = HeatmapMath.overlayHeatmap(resized, heatmapFull, 0.4f);

        Result result = new Result();
        result.predictedClassName = classWeights.classNames[predictedClassIdx];
        result.confidence = confidence;
        result.allProbabilities = predictions[0];
        result.heatmapOverlay = overlay;
        return result;
    }

    private int argMax(float[] arr) {
        int best = 0;
        for (int i = 1; i < arr.length; i++) {
            if (arr[i] > arr[best]) best = i;
        }
        return best;
    }

    public void close() {
        interpreter.close();
    }

    public static class Result {
        public String predictedClassName;
        public float confidence;
        public float[] allProbabilities;
        public Bitmap heatmapOverlay;
    }
}