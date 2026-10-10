package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.MathUtils;

/** Draws simple ability icons at runtime so the new abilities work without any extra art files. */
public final class IconFactory {
    private IconFactory() {
    }

    public static Texture create(AbilityPickup.Type type) {
        int s = 64;
        Pixmap pm = new Pixmap(s, s, Pixmap.Format.RGBA8888);
        Color c = type.color;

        pm.setColor(0f, 0f, 0f, 0f);
        pm.fill();

        // Disc + ring
        pm.setColor(c.r * 0.22f, c.g * 0.22f, c.b * 0.22f, 1f);
        pm.fillCircle(32, 32, 30);
        pm.setColor(c);
        for (int r = 28; r <= 30; r++) {
            pm.drawCircle(32, 32, r);
        }

        pm.setColor(1f, 1f, 1f, 1f);
        switch (type) {
            case FREEZE -> {
                // Snowflake: three crossing lines
                for (int i = 0; i < 3; i++) {
                    float a = MathUtils.PI / 2f + i * MathUtils.PI / 3f;
                    float dx = MathUtils.cos(a) * 19f;
                    float dy = MathUtils.sin(a) * 19f;
                    line(pm, 32 - dx, 32 - dy, 32 + dx, 32 + dy);
                }
                pm.setColor(c);
                pm.fillCircle(32, 32, 4);
            }
            case NUKE -> {
                // Starburst: core + 8 rays
                for (int i = 0; i < 8; i++) {
                    float a = i * MathUtils.PI / 4f;
                    line(pm, 32 + MathUtils.cos(a) * 13f, 32 + MathUtils.sin(a) * 13f,
                        32 + MathUtils.cos(a) * 24f, 32 + MathUtils.sin(a) * 24f);
                }
                pm.setColor(1f, 0.95f, 0.7f, 1f);
                pm.fillCircle(32, 32, 9);
            }
            case OVERDRIVE -> {
                // Lightning bolt
                pm.fillTriangle(38, 10, 22, 36, 34, 36);
                pm.fillTriangle(30, 54, 42, 28, 30, 28);
            }
            default -> pm.fillCircle(32, 32, 12);
        }

        Texture texture = new Texture(pm);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pm.dispose();
        return texture;
    }

    private static void line(Pixmap pm, float x1, float y1, float x2, float y2) {
        for (int dx = 0; dx <= 1; dx++) {
            for (int dy = 0; dy <= 1; dy++) {
                pm.drawLine(Math.round(x1) + dx, Math.round(y1) + dy, Math.round(x2) + dx, Math.round(y2) + dy);
            }
        }
    }
}
