package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/** A glowing icon that appears on the map and grants a temporary buff when collected. */
public class AbilityPickup extends GameObject {

    public enum Type {
        SHIELD(new Color(0.35f, 0.65f, 1f, 1f)),
        HASTE(new Color(0.4f, 1f, 0.45f, 1f)),
        MAGNET(new Color(1f, 0.65f, 0.2f, 1f)),
        STORM(new Color(0.72f, 0.42f, 1f, 1f));

        final Color color;

        Type(Color color) {
            this.color = color;
        }
    }

    private static final float SIZE = 0.34f;

    private final Type type;
    private float bobTimer;

    public AbilityPickup(float x, float y, Type type) {
        super(x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
        this.type = type;
    }

    @Override
    void update(float deltaTime) {
        bobTimer += deltaTime;
    }

    public Type getType() {
        return type;
    }

    public void drawDebug(ShapeRenderer shapeRenderer) {
        float bob = MathUtils.sin(bobTimer * 4f) * 0.05f;
        float centerX = rect.x + rect.width / 2f;
        float centerY = rect.y + rect.height / 2f + bob;
        float radius = rect.width / 2f;

        // Pulsing outer glow so the icon reads clearly against the field.
        float pulse = 0.5f + 0.5f * MathUtils.sin(bobTimer * 3f);
        shapeRenderer.setColor(type.color.r, type.color.g, type.color.b, 0.18f + 0.15f * pulse);
        shapeRenderer.circle(centerX, centerY, radius * 1.5f, 20);
        shapeRenderer.setColor(type.color.r, type.color.g, type.color.b, 0.3f);
        shapeRenderer.circle(centerX, centerY, radius * 1.05f, 20);

        switch (type) {
            case SHIELD -> drawShieldIcon(shapeRenderer, centerX, centerY, radius);
            case HASTE -> drawHasteIcon(shapeRenderer, centerX, centerY, radius, bobTimer);
            case MAGNET -> drawMagnetIcon(shapeRenderer, centerX, centerY, radius);
            case STORM -> drawStormIcon(shapeRenderer, centerX, centerY, radius, bobTimer);
        }
    }

    private void drawShieldIcon(ShapeRenderer sr, float cx, float cy, float r) {
        float[] pts = {
            -0.55f, 0.75f,
            0.55f, 0.75f,
            0.7f, 0.05f,
            0.0f, -0.85f,
            -0.7f, 0.05f
        };
        fillPolygon(sr, cx, cy, r, 0f, pts, type.color);

        // Inner emblem so it doesn't just look like a blob.
        sr.setColor(Color.WHITE);
        sr.rectLine(cx, cy + 0.4f * r, cx, cy - 0.35f * r, 0.05f * r);
    }

    private void drawHasteIcon(ShapeRenderer sr, float cx, float cy, float r, float time) {
        float wobble = MathUtils.sin(time * 6f) * 0.05f;
        float[] left = { -0.85f + wobble, 0.6f, -0.85f + wobble, -0.6f, -0.1f + wobble, 0f };
        float[] right = { 0.0f + wobble, 0.6f, 0.0f + wobble, -0.6f, 0.75f + wobble, 0f };
        fillPolygon(sr, cx, cy, r, 0f, left, type.color);
        fillPolygon(sr, cx, cy, r, 0f, right, type.color);
    }

    private void drawMagnetIcon(ShapeRenderer sr, float cx, float cy, float r) {
        float legWidth = 0.28f * r;
        float legHeight = 0.75f * r;
        float legOffset = 0.4f * r;
        float baseY = cy - 0.2f * r;

        sr.setColor(type.color);
        sr.arc(cx, baseY, 0.42f * r, 180f, 180f);
        sr.rect(cx - legOffset - legWidth / 2f, baseY, legWidth, legHeight);
        sr.rect(cx + legOffset - legWidth / 2f, baseY, legWidth, legHeight);

        // Classic red/blue pole tips.
        float tipY = baseY + legHeight - 0.12f * r;
        sr.setColor(Color.SCARLET);
        sr.rect(cx - legOffset - legWidth / 2f, tipY, legWidth, 0.12f * r);
        sr.setColor(Color.ROYAL);
        sr.rect(cx + legOffset - legWidth / 2f, tipY, legWidth, 0.12f * r);
    }

    private void drawStormIcon(ShapeRenderer sr, float cx, float cy, float r, float time) {
        float flicker = 0.85f + 0.15f * MathUtils.sin(time * 14f);
        float[] pts = {
            0.15f, 0.95f,
            -0.35f, 0.15f,
            -0.05f, 0.15f,
            -0.2f, -0.95f,
            0.45f, -0.05f,
            0.05f, -0.05f
        };
        Color bolt = new Color(type.color.r * flicker, type.color.g * flicker, type.color.b * flicker, 1f);
        fillPolygon(sr, cx, cy, r, 0f, pts, bolt);
    }

    /** Fills a small polygon (given as normalized -1..1 points around the origin) via a triangle fan. */
    private static void fillPolygon(ShapeRenderer sr, float cx, float cy, float r, float rotationRad, float[] pts, Color color) {
        int n = pts.length / 2;
        float cos = MathUtils.cos(rotationRad);
        float sin = MathUtils.sin(rotationRad);
        float[] wx = new float[n];
        float[] wy = new float[n];
        float sumX = 0f, sumY = 0f;

        for (int i = 0; i < n; i++) {
            float px = pts[i * 2];
            float py = pts[i * 2 + 1];
            float rx = px * cos - py * sin;
            float ry = px * sin + py * cos;
            wx[i] = cx + rx * r;
            wy[i] = cy + ry * r;
            sumX += wx[i];
            sumY += wy[i];
        }

        float centroidX = sumX / n;
        float centroidY = sumY / n;

        sr.setColor(color);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            sr.triangle(centroidX, centroidY, wx[i], wy[i], wx[j], wy[j]);
        }
    }

    public static Type randomType() {
        Type[] values = Type.values();
        return values[MathUtils.random(values.length - 1)];
    }
}
