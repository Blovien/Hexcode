package com.riprod.hexcode.core.drawing.utils;

import java.util.ArrayDeque;
import java.util.List;

import com.hypixel.hytale.logger.HytaleLogger;
import com.riprod.hexcode.core.drawing.component.DrawnShapeComponent;
import com.riprod.hexcode.core.drawing.registry.ShapeAsset;
import com.riprod.hexcode.core.drawing.system.ShapeCacheManager;

public class ShapeComparator {
    private static final HytaleLogger LOGGER = HytaleLogger.forEnclosingClass();

    private static final int GRID = 32;
    private static final int RADIUS = 3;
    private static final float PRECISION_WEIGHT = 0.7f;
    private static final float RECALL_WEIGHT = 0.3f;

    // for center check
    private static final int CENTER_MIN = 14;
    private static final int CENTER_MAX = 18;

    public static DrawnShapeComponent getShape(boolean[][] grid) {
        int[][] drawnDT = distanceTransform(grid);

        float bestAccuracy = -1f;
        ShapeAsset bestMatch = null;

        for (var entry : ShapeAsset.getAssetMap().getAssetMap().entrySet()) {
            ShapeAsset asset = entry.getValue();
            float accuracy = compareShapes(grid, drawnDT, asset);
            if (accuracy > bestAccuracy) {
                bestAccuracy = accuracy;
                bestMatch = asset;
            }
            if (accuracy > 0f) {
                LOGGER.atInfo().log("Compared against shape " + asset.getId() + " with accuracy " + accuracy);
            }
        }

        LOGGER.atInfo().log("Best shape match: " + (bestMatch != null ? bestMatch.getId() : "none") + " with accuracy "
                + bestAccuracy);

        return bestMatch != null ? new DrawnShapeComponent(bestMatch.getId(), bestAccuracy, bestMatch) : null;
    }

    static boolean centerCheck(boolean[][] drawn, ShapeAsset shapeAsset) {
        if (shapeAsset.getCenterFilled() == null)
            return true;

        boolean hasCenter = false;
        for (int y = CENTER_MIN; y < CENTER_MAX && !hasCenter; y++)
            for (int x = CENTER_MIN; x < CENTER_MAX && !hasCenter; x++)
                hasCenter = drawn[y][x];

        return hasCenter == shapeAsset.getCenterFilled();
    }

    public static float compareShapes(boolean[][] drawn, int[][] drawnDT, ShapeAsset shapeAsset) {
        if (!centerCheck(drawn, shapeAsset))
            return 0f;

        int[][] templateDT = ShapeCacheManager.getDistanceTransform(shapeAsset.getId());
        boolean[][] template = ShapeCacheManager.getImageData(shapeAsset.getId());
        if (templateDT == null || template == null)
            return 0f;

        float score = score(drawn, drawnDT, template, templateDT);

        if (Boolean.TRUE.equals(shapeAsset.getCanRotate())) {
            boolean[][] rotated = rotate90(drawn);
            int[][] rotatedDT = distanceTransform(rotated);
            score = Math.max(score, score(rotated, rotatedDT, template, templateDT));
        }

        return score;
    }

    static float score(boolean[][] drawn, int[][] drawnDT,
            boolean[][] template, int[][] templateDT) {
        float precision = 0f;
        int drawnCount = 0;
        float recall = 0f;
        int templateCount = 0;

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (drawn[y][x]) {
                    drawnCount++;
                    precision += Math.max(0f, 1f - (float) templateDT[y][x] / RADIUS);
                }
                if (template[y][x]) {
                    templateCount++;
                    recall += Math.max(0f, 1f - (float) drawnDT[y][x] / RADIUS);
                }
            }
        }

        if (drawnCount == 0 || templateCount == 0)
            return 0f;

        precision /= drawnCount;
        recall /= templateCount;

        return PRECISION_WEIGHT * precision + RECALL_WEIGHT * recall;
    }

    static int[][] distanceTransform(boolean[][] grid) {
        int[][] dist = new int[GRID][GRID];
        ArrayDeque<int[]> queue = new ArrayDeque<>();

        for (int y = 0; y < GRID; y++) {
            for (int x = 0; x < GRID; x++) {
                if (grid[y][x]) {
                    queue.add(new int[] { x, y });
                } else {
                    dist[y][x] = Integer.MAX_VALUE;
                }
            }
        }

        int[][] dirs = { { -1, -1 }, { -1, 0 }, { -1, 1 }, { 0, -1 }, { 0, 1 }, { 1, -1 }, { 1, 0 }, { 1, 1 } };
        while (!queue.isEmpty()) {
            int[] cell = queue.poll();
            int nd = dist[cell[1]][cell[0]] + 1;
            for (int[] d : dirs) {
                int nx = cell[0] + d[0], ny = cell[1] + d[1];
                if (nx >= 0 && nx < GRID && ny >= 0 && ny < GRID && dist[ny][nx] > nd) {
                    dist[ny][nx] = nd;
                    queue.add(new int[] { nx, ny });
                }
            }
        }

        return dist;
    }

    static boolean[][] rotate90(boolean[][] grid) {
        boolean[][] out = new boolean[GRID][GRID];
        for (int y = 0; y < GRID; y++)
            for (int x = 0; x < GRID; x++)
                out[x][GRID - 1 - y] = grid[y][x];
        return out;
    }

    public static Float calculateVolatility(List<DrawnShapeComponent> shapes) {
        Float cumulative = 0.0f;
        for (DrawnShapeComponent shape : shapes) {
            cumulative += shape.getVolatility();
        }
        // average for all shapes - discard total structure volatility
        return cumulative / shapes.size();
    }

    public static Float calculateEfficiency(List<DrawnShapeComponent> shapes) {
	Float totalSpeed = 0.0f;
	
	for (DrawnShapeComponent shape : shapes) {
		totalSpeed += shape.getEfficiency();
	}
	// Average speed across all shapes
	return totalSpeed / shapes.size();
}
}
