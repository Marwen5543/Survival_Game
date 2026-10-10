package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;

/**
 * A glowing icon that appears on the map. Collecting it doesn't apply its buff immediately —
 * it banks a charge (see {@link Player#addAbilityCharge}) that the player spends later, on
 * their own timing, via the 1-7 hotkeys.
 */
public class AbilityPickup extends GameObject {

    public enum Type {
        SHIELD(new Color(0.35f, 0.65f, 1f, 1f)),
        HASTE(new Color(0.4f, 1f, 0.45f, 1f)),
        MAGNET(new Color(1f, 0.65f, 0.2f, 1f)),
        STORM(new Color(0.72f, 0.42f, 1f, 1f)),
        FREEZE(new Color(0.55f, 0.9f, 1f, 1f)),      // time almost stops for every enemy
        NUKE(new Color(1f, 0.4f, 0.15f, 1f)),        // supernova: hits every enemy on screen
        OVERDRIVE(new Color(1f, 0.35f, 0.75f, 1f));  // x1.5 damage, ~2x attack rate

        final Color color;

        Type(Color color) {
            this.color = color;
        }
    }

    private static final float SIZE = 0.42f;

    private final Type type;
    private float bobTimer;

    public AbilityPickup(float x, float y, Type type, Texture icon) {
        super(x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE, icon);
        this.type = type;
    }

    @Override
    void update(float deltaTime) {
        bobTimer += deltaTime;
    }

    public Type getType() {
        return type;
    }

    /** Soft pulsing glow drawn behind the icon — call this in a ShapeRenderer Filled pass, before the batch pass. */
    public void drawGlow(ShapeRenderer shapeRenderer) {
        float pulse = 0.5f + 0.5f * MathUtils.sin(bobTimer * 3f);
        float centerX = rect.x + rect.width / 2f;
        float centerY = rect.y + rect.height / 2f + bob();
        shapeRenderer.setColor(type.color.r, type.color.g, type.color.b, 0.18f + 0.15f * pulse);
        shapeRenderer.circle(centerX, centerY, rect.width * 0.95f, 20);
    }

    @Override
    public void draw(Batch batch) {
        if (texture == null) return;

        float pulse = 1f + 0.08f * MathUtils.sin(bobTimer * 3f);
        float width = rect.width * pulse;
        float height = rect.height * pulse;
        float centerX = rect.x + rect.width / 2f;
        float centerY = rect.y + rect.height / 2f + bob();

        batch.draw(texture, centerX - width / 2f, centerY - height / 2f, width, height);
    }

    private float bob() {
        return MathUtils.sin(bobTimer * 4f) * 0.06f;
    }

    public static Type randomType() {
        Type[] values = Type.values();
        return values[MathUtils.random(values.length - 1)];
    }
}
