package com.dermify.dermify.ml;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class CamClassWeights {

    public String[] classNames;
    public float[][] weights; // [numClasses][1280]

    public static CamClassWeights loadFromAssets(Context context, String filename)
            throws IOException, JSONException {
        InputStream is = context.getAssets().open(filename);
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();

        JSONObject json = new JSONObject(sb.toString());
        JSONArray namesArr = json.getJSONArray("class_names");
        JSONArray weightsArr = json.getJSONArray("weights");

        String[] names = new String[namesArr.length()];
        for (int i = 0; i < namesArr.length(); i++) {
            names[i] = namesArr.getString(i);
        }

        float[][] w = new float[weightsArr.length()][];
        for (int i = 0; i < weightsArr.length(); i++) {
            JSONArray row = weightsArr.getJSONArray(i);
            w[i] = new float[row.length()];
            for (int j = 0; j < row.length(); j++) {
                w[i][j] = (float) row.getDouble(j);
            }
        }

        CamClassWeights result = new CamClassWeights();
        result.classNames = names;
        result.weights = w;
        return result;
    }
}