package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/** Non-interactive scenery scattered across the arena so the field reads as a designed place, not empty grass. */
public class GroundDecoration {

    public enum Type { GRASS_TUFT, ROCK, CRACK, RUNE }

    private final Type type;
    private final float x, y;
    private final float rotation;
    private final float scale;
    private final float phase; // animation offset so runes/grass don't all pulse in sync
    private final Color color;

    public GroundDecoration(float x, float y, Type type) {
        this.x = x;
        this.y = y;
        this.type = type;
        this.rotation = MathUtils.random(0f, 360f);
        this.scale = MathUtils.random(0.75f, 1.35f);
        this.phase = MathUtils.random(0f, MathUtils.PI2);

        this.color = switch (type) {
            case GRASS_TUFT -> new Color(0.15f + MathUtils.random(0.1f), 0.35f + MathUtils.random(0.1f), 0.08f, 1f);
            case ROCK -> new Color(0.28f, 0.27f, 0.25f, 1f);
            case CRACK -> new Color(0.05f, 0.08f, 0.05f, 0.5f);
            case RUNE -> new Color(0.45f, 0.65f, 0.95f, 1f);
        };
    }

    /** Assumes the ShapeRenderer is already begun in Filled mode. */
    public void draw(ShapeRenderer sr, float elapsedTime) {
        switch (type) {
            case GRASS_TUFT -> drawGrass(sr);
            case ROCK -> drawRock(sr);
            case CRACK -> drawCrack(sr);
            case RUNE -> drawRune(sr, elapsedTime);
        }
    }

    private void drawGrass(ShapeRenderer sr) {
        sr.setColor(color);
        float bladeWidth = 0.03f * scale;
        for (int i = -1; i <= 1; i++) {
            float baseX = x + i * 0.05f * scale;
            float lean = i * 0.05f * scale;
            sr.triangle(
                baseX, y,
                baseX + bladeWidth, y,
                baseX + lean, y + 0.16f * scale
            );
        }
    }

    private void drawRock(ShapeRenderer sr) {
        sr.setColor(color);
        sr.circle(x, y, 0.07f * scale, 6);
        sr.setColor(Math.min(1f, color.r * 1.3f), Math.min(1f, color.g * 1.3f), Math.min(1f, color.b * 1.3f), color.a);
        sr.circle(x - 0.02f * scale, y + 0.02f * scale, 0.03f * scale, 6);
    }

    private void drawCrack(ShapeRenderer sr) {
        sr.setColor(color);
        float rad = rotation * MathUtils.degreesToRadians;
        float dx = MathUtils.cos(rad) * 0.22f * scale;
        float dy = MathUtils.sin(rad) * 0.22f * scale;
        sr.rectLine(x - dx, y - dy, x + dx, y + dy, 0.012f);
        sr.rectLine(x, y, x + dy * 0.4f, y - dx * 0.4f, 0.01f);
    }

    private void drawRune(ShapeRenderer sr, float elapsedTime) {
        float pulse = 0.5f + 0.5f * MathUtils.sin(elapsedTime * 1.5f + phase);
        sr.setColor(color.r, color.g, color.b, 0.15f + 0.25f * pulse);
        sr.circle(x, y, 0.16f * scale, 16);
        sr.setColor(color.r, color.g, color.b, 0.35f + 0.4f * pulse);
        float rad = (rotation + elapsedTime * 12f) * MathUtils.degreesToRadians;
        for (int i = 0; i < 4; i++) {
            float a = rad + i * MathUtils.PI / 2f;
            sr.rectLine(x, y, x + MathUtils.cos(a) * 0.14f * scale, y + MathUtils.sin(a) * 0.14f * scale, 0.015f);
        }
    }

    public static Type randomType() {
        float r = MathUtils.random();
        if (r < 0.55f) return Type.GRASS_TUFT;
        if (r < 0.75f) return Type.ROCK;
        if (r < 0.9f) return Type.CRACK;
        return Type.RUNE;
    }
}
