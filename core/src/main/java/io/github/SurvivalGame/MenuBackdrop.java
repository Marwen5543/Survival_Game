package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Align;

/** Animated gradient + drifting motes shared by every menu screen. Follows the chosen field theme live. */
public class MenuBackdrop {
    private static final int COUNT = 90;

    private final float[] px = new float[COUNT];
    private final float[] py = new float[COUNT];
    private final float[] speed = new float[COUNT];
    private final float[] size = new float[COUNT];
    private final float[] phase = new float[COUNT];
    private final Color top = new Color(0, 0, 0, 1);
    private final Color bottom = new Color(0, 0, 0, 1);
    private final Color mote = new Color(1, 1, 1, 1);
    private final Color titleColor = new Color(1, 1, 1, 1);
    private final Color subColor = new Color(0.85f, 0.85f, 0.95f, 1f);
    private float time;

    public MenuBackdrop() {
        for (int i = 0; i < COUNT; i++) {
            px[i] = MathUtils.random();
            py[i] = MathUtils.random();
            speed[i] = MathUtils.random(0.4f, 1.4f);
            size[i] = MathUtils.random(1.5f, 5f);
            phase[i] = MathUtils.random(MathUtils.PI2);
        }
    }

    public void update(float dt) {
        time += dt;
        Settings.FieldTheme t = Settings.fieldTheme;
        for (int i = 0; i < COUNT; i++) {
            py[i] += t.ambientVy * 0.08f * speed[i] * dt;
            px[i] += MathUtils.sin(time * speed[i] + phase[i]) * 0.01f * dt;
            if (py[i] > 1.02f) py[i] = -0.02f;
            if (py[i] < -0.02f) py[i] = 1.02f;
            if (px[i] > 1f) px[i] -= 1f;
            if (px[i] < 0f) px[i] += 1f;
        }
    }

    /** Needs a ShapeRenderer already begun in Filled mode. */
    public void draw(ShapeRenderer sr, float w, float h) {
        Settings.FieldTheme t = Settings.fieldTheme;
        Color tint = t.bgTint;
        top.set(tint.r * 0.04f, tint.g * 0.04f, tint.b * 0.08f, 1f);
        bottom.set(tint.r * 0.20f, tint.g * 0.20f, tint.b * 0.22f, 1f);
        sr.rect(0, 0, w, h, bottom, bottom, top, top);

        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 0.8f);
        sr.setColor(t.rune.r, t.rune.g, t.rune.b, 0.06f + 0.05f * pulse);
        sr.circle(w / 2f, -h * 0.25f, h * (0.95f + 0.05f * pulse), 64);

        for (int i = 0; i < COUNT; i++) {
            float a = 0.2f + 0.4f * (0.5f + 0.5f * MathUtils.sin(time * 2f * speed[i] + phase[i]));
            mote.set(t.ambient.r, t.ambient.g, t.ambient.b, a);
            sr.setColor(mote);
            sr.circle(px[i] * w, py[i] * h, size[i], 8);
        }
    }

    public void drawTitle(Batch batch, BitmapFont font, GlyphLayout layout, float w, float topY) {
        float pulse = 0.5f + 0.5f * MathUtils.sin(time * 2f);
        titleColor.set(1f, 0.78f + 0.17f * pulse, 0.2f + 0.2f * pulse, 1f);

        font.getData().setScale(3.2f);
        layout.setText(font, "SURVIVAL", titleColor, 0f, Align.left, false);
        font.draw(batch, layout, w / 2f - layout.width / 2f, topY);
        float titleHeight = layout.height;

        font.getData().setScale(1.1f);
        layout.setText(font, "E N D L E S S   N I G H T", subColor, 0f, Align.left, false);
        font.draw(batch, layout, w / 2f - layout.width / 2f, topY - titleHeight - 24f);

        font.getData().setScale(1f);
    }
}
