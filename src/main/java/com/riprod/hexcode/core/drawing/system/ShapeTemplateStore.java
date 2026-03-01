package com.riprod.hexcode.core.drawing.system;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.drawing.registry.ShapeAsset;

import it.unimi.dsi.fastutil.floats.FloatArrayList;

public class ShapeTemplateStore {

    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path TEMPLATE_DIR = Paths.get("Server", "Hexcode", "ShapeTemplates");

    private static final Map<String, List<float[][]>> templates = new ConcurrentHashMap<>();
    private static boolean loaded = false;

    public static void ensureLoaded() {
        if (loaded) return;
        loaded = true;

        try {
            if (!Files.exists(TEMPLATE_DIR)) {
                Files.createDirectories(TEMPLATE_DIR);
            }
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(TEMPLATE_DIR, "*.json")) {
                for (Path file : stream) {
                    loadFile(file);
                }
            }
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("failed to load shape templates");
        }

        ShapeAsset.getAssetMap().getAssetMap().forEach((key, asset) -> {
            if (templates.containsKey(key)) return;
            Path assetPath = ShapeAsset.getAssetMap().getPath(key);
            if (assetPath == null) return;
            Path templatePath = assetPath.resolveSibling(key + ".template.json");
            if (Files.exists(templatePath)) {
                loadFile(templatePath);
            }
        });

        int total = templates.values().stream().mapToInt(List::size).sum();
        LOGGER.atInfo().log("ShapeTemplateStore loaded " + total + " templates for " + templates.size() + " shapes.");
    }

    private static void loadFile(Path file) {
        try (Reader reader = Files.newBufferedReader(file)) {
            TemplateData data = GSON.fromJson(reader, TemplateData.class);
            if (data == null || data.shapeId == null || data.points == null) return;

            float[][] points = new float[data.points.length][2];
            for (int i = 0; i < data.points.length; i++) {
                points[i][0] = data.points[i][0];
                points[i][1] = data.points[i][1];
            }

            templates.computeIfAbsent(data.shapeId, k -> new ArrayList<>()).add(points);
        } catch (Exception e) {
            LOGGER.atWarning().withCause(e).log("failed to load template: " + file);
        }
    }

    public static void saveTemplate(String shapeId, FloatArrayList rawAngles) {
        try {
            if (!Files.exists(TEMPLATE_DIR)) {
                Files.createDirectories(TEMPLATE_DIR);
            }

            int count = rawAngles.size() / 2;
            float[][] points = new float[count][2];
            for (int i = 0; i < count; i++) {
                points[i][0] = rawAngles.getFloat(i * 2);
                points[i][1] = rawAngles.getFloat(i * 2 + 1);
            }

            TemplateData data = new TemplateData();
            data.shapeId = shapeId;
            data.points = points;

            int index = templates.computeIfAbsent(shapeId, k -> new ArrayList<>()).size();
            templates.get(shapeId).add(points);

            String filename = shapeId + "_" + index + ".json";
            Path file = TEMPLATE_DIR.resolve(filename);

            try (Writer writer = Files.newBufferedWriter(file)) {
                GSON.toJson(data, writer);
            }

            LOGGER.atInfo().log("saved template for '" + shapeId + "' (" + count + " points) to " + filename);
        } catch (IOException e) {
            LOGGER.atSevere().withCause(e).log("failed to save template for " + shapeId);
        }
    }

    public static Map<String, List<float[][]>> getTemplates() {
        ensureLoaded();
        return templates;
    }

    public static List<float[][]> getTemplatesFor(String shapeId) {
        ensureLoaded();
        return templates.getOrDefault(shapeId, List.of());
    }

    public static boolean hasTemplates() {
        ensureLoaded();
        return !templates.isEmpty();
    }

    public static void clearCache() {
        templates.clear();
        loaded = false;
    }

    private static class TemplateData {
        String shapeId;
        float[][] points;
    }
}
