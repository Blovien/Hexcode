package com.riprod.hexcode.core.common.glyphs.icon;

import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import javax.imageio.ImageIO;

import com.google.gson.Gson;

public final class GlyphIconRasterizer {

    @FunctionalInterface
    public interface Resolver {
        Path resolve(String relPath);
    }

    private static final Gson GSON = new Gson();

    private static final int INTERNAL = 1024;
    private static final int FINAL = 256;
    private static final double REFERENCE_QUAD_UNITS = 64.0;
    private static final double REFERENCE_FILL = 0.55;
    private static final double PX_PER_UNIT = INTERNAL * REFERENCE_FILL / REFERENCE_QUAD_UNITS;

    private static final String MODELDEF_SUBPATH = "Server/Models/Hexcode/Glyphs";
    private static final String COMMON_ROOT = "Common";

    private GlyphIconRasterizer() {
    }

    public static byte[] rasterize(String modelPath, Resolver resolver) {
        ModelDef def = loadModelDef(modelPath, resolver);
        if (def == null || def.DefaultAttachments == null || def.DefaultAttachments.isEmpty()) {
            return null;
        }

        BufferedImage canvas = new BufferedImage(INTERNAL, INTERNAL, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = canvas.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setComposite(AlphaComposite.SrcOver);

        AffineTransform base = new AffineTransform();
        base.translate(INTERNAL / 2.0, INTERNAL / 2.0);
        base.scale(PX_PER_UNIT, -PX_PER_UNIT);

        boolean drewAny = false;
        for (Attachment att : def.DefaultAttachments) {
            if (att == null || att.Model == null || att.Texture == null) {
                continue;
            }
            BlockyModel model = loadJson(resolver.resolve(COMMON_ROOT + "/" + att.Model), BlockyModel.class);
            BufferedImage tex = loadImage(resolver.resolve(COMMON_ROOT + "/" + att.Texture));
            if (model == null || tex == null || model.nodes == null) {
                continue;
            }
            for (Node n : model.nodes) {
                drewAny |= drawNode(g, n, base, tex);
            }
        }
        g.dispose();

        if (!drewAny) {
            return null;
        }

        BufferedImage out = downscale(toWhite(canvas), FINAL);
        return encodePng(out);
    }

    private static boolean drawNode(Graphics2D g, Node node, AffineTransform parent, BufferedImage tex) {
        AffineTransform t = new AffineTransform(parent);
        if (node.position != null) {
            t.translate(node.position.x, node.position.y);
        }
        if (node.orientation != null) {
            t.rotate(2.0 * Math.atan2(node.orientation.z, node.orientation.w));
        }

        boolean drew = false;
        if (node.shape != null && node.shape.visible && "quad".equals(node.shape.type)) {
            drew = drawQuad(g, node.shape, t, tex);
        }
        if (node.children != null) {
            for (Node c : node.children) {
                drew |= drawNode(g, c, t, tex);
            }
        }
        return drew;
    }

    private static boolean drawQuad(Graphics2D g, Shape shape, AffineTransform nodeT, BufferedImage tex) {
        if (shape.settings == null || shape.settings.size == null) {
            return false;
        }
        double sx = shape.stretch != null ? shape.stretch.x : 1.0;
        double sy = shape.stretch != null ? shape.stretch.y : 1.0;
        double w = shape.settings.size.x * sx;
        double h = shape.settings.size.y * sy;
        if (w <= 0 || h <= 0) {
            return false;
        }

        AffineTransform t = new AffineTransform(nodeT);
        if (shape.offset != null) {
            t.translate(shape.offset.x, shape.offset.y);
        }

        boolean mirrorX = false;
        boolean mirrorY = false;
        double faceAngle = 0;
        if (shape.textureLayout != null && shape.textureLayout.front != null) {
            Face f = shape.textureLayout.front;
            if (f.mirror != null) {
                mirrorX = f.mirror.x;
                mirrorY = f.mirror.y;
            }
            faceAngle = Math.toRadians(f.angle);
        }
        if (faceAngle != 0) {
            t.rotate(faceAngle);
        }

        int tw = tex.getWidth();
        int th = tex.getHeight();
        t.scale(w / tw * (mirrorX ? -1 : 1), -h / th * (mirrorY ? -1 : 1));
        t.translate(-tw / 2.0, -th / 2.0);

        g.drawImage(tex, t, null);
        return true;
    }

    private static ModelDef loadModelDef(String modelPath, Resolver resolver) {
        return loadJson(resolver.resolve(MODELDEF_SUBPATH + "/" + modelPath + ".json"), ModelDef.class);
    }

    private static <T> T loadJson(Path p, Class<T> type) {
        if (p == null) {
            return null;
        }
        try {
            return GSON.fromJson(Files.readString(p), type);
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage loadImage(Path p) {
        if (p == null) {
            return null;
        }
        try (InputStream in = Files.newInputStream(p)) {
            return ImageIO.read(in);
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage toWhite(BufferedImage src) {
        int w = src.getWidth();
        int h = src.getHeight();
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int a = (src.getRGB(x, y) >>> 24) & 0xFF;
                out.setRGB(x, y, (a << 24) | 0x00FFFFFF);
            }
        }
        return out;
    }

    private static BufferedImage downscale(BufferedImage src, int size) {
        BufferedImage out = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = out.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g.drawImage(src, 0, 0, size, size, null);
        g.dispose();
        return out;
    }

    private static byte[] encodePng(BufferedImage img) {
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", bos);
            return bos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    // gson pojos -------------------------------------------------------------

    @SuppressWarnings("unused")
    static final class ModelDef {
        String Parent;
        String Model;
        String Texture;
        List<Attachment> DefaultAttachments;
    }

    @SuppressWarnings("unused")
    static final class Attachment {
        String Model;
        String Texture;
    }

    @SuppressWarnings("unused")
    static final class BlockyModel {
        List<Node> nodes;
    }

    @SuppressWarnings("unused")
    static final class Node {
        Vec3 position;
        Quat orientation;
        Shape shape;
        List<Node> children;
    }

    @SuppressWarnings("unused")
    static final class Shape {
        String type;
        Vec3 offset;
        Vec3 stretch;
        Settings settings;
        TextureLayout textureLayout;
        boolean visible;
    }

    static final class Settings {
        Size size;
    }

    static final class Size {
        double x;
        double y;
    }

    static final class TextureLayout {
        Face front;
    }

    static final class Face {
        Mirror mirror;
        double angle;
    }

    static final class Mirror {
        boolean x;
        boolean y;
    }

    static final class Vec3 {
        double x;
        double y;
        double z;
    }

    static final class Quat {
        double z;
        double w;
    }
}
