package com.riprod.hexcode.core.drawing.registry;

import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.drawing.system.shapes.DollarOneFixedDetector;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class ShapeTemplateStore {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static void saveTemplate(String shapeId, FloatArrayList rawAngles) {
        try {
            Path templateDir = resolveTemplateDir();
            if (templateDir == null) {
                LOGGER.atSevere().log("cannot save template: no template directory found");
                return;
            }

            if (!Files.exists(templateDir)) {
                Files.createDirectories(templateDir);
            }

            int count = rawAngles.size() / 2;
            float[][] raw = new float[count][2];
            for (int i = 0; i < count; i++) {
                raw[i][0] = rawAngles.getFloat(i * 2);
                raw[i][1] = rawAngles.getFloat(i * 2 + 1);
            }

            float[][] processed = DollarOneFixedDetector.preprocess(raw);

            float[] flat = new float[processed.length * 2];
            for (int i = 0; i < processed.length; i++) {
                flat[i * 2] = processed[i][0];
                flat[i * 2 + 1] = processed[i][1];
            }

            int index = TemplateAsset.getTemplatesForShape(shapeId).size();
            String filename = shapeId + "_" + index + ".json";
            Path file = templateDir.resolve(filename);

            TemplateData data = new TemplateData();
            data.ShapeId = shapeId;
            data.Points = flat;
            data.IsTraining = true;

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }

            LOGGER.atInfo().log("saved training template for '" + shapeId + "' (" + processed.length + " points) to " + filename);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("failed to save template for " + shapeId);
        }
    }

    private static Path resolveTemplateDir() {
        for (Map.Entry<String, TemplateAsset> entry : TemplateAsset.getAssetMap().getAssetMap().entrySet()) {
            Path p = TemplateAsset.getAssetMap().getPath(entry.getKey());
            if (p != null) {
                return p.getParent();
            }
        }
        return null;
    }

    @SuppressWarnings("unused")
    private static class TemplateData {
        String ShapeId;
        float[] Points;
        Boolean IsTraining;
    }
}
