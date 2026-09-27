package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Enemy extends GameObject {

    /** Different enemy flavors — same sprite, different stats and tint until you add new art. */
    public enum Type {
        SLIME(1f, 1.5f, 1, 1f, Color.WHITE),
        RUNNER(1f, 2.6f, 1, 0.85f, new Color(0.55f, 0.85f, 1f, 1f)),
        BRUTE(3f, 0.9f, 3, 1.6f, new Color(1f, 0.5f, 0.5f, 1f));

        final float maxHealth;
        final float speed;
        final int xpValue;
        final float sizeMultiplier;
        final Color tint;

        Type(float maxHealth, float speed, int xpValue, float sizeMultiplier, Color tint) {
            this.maxHealth = maxHealth;
            this.speed = speed;
            this.xpValue = xpValue;
            this.sizeMultiplier = sizeMultiplier;
            this.tint = tint;
        }
    }

    private static final float SCALE = 1 / 32f;
    private static final float HIT_FLASH_DURATION = 0.1f;

    private final Player player;
    private final Type type;
    private float health;
    private float hitFlashTimer;

    // Brief colored flash used by external effects (e.g. a Storm lightning strike) to show
    // something *happened* to this enemy, distinct from the plain white on-hit flash.
    private float statusTimer;
    private Color statusColor;

    public Enemy(float x, float y, Texture texture, Player player, Type type) {
        super(
            x, y,
            texture.getWidth() * SCALE * type.sizeMultiplier,
            texture.getHeight() * SCALE * type.sizeMultiplier,
            texture
        );
        this.player = player;
        this.type = type;
        this.health = type.maxHealth;
    }

    @Override
    void update(float deltaTime) {
        Vector2 direction = player.getCenter(TMP_VEC2)
            .sub(rect.x + rect.width * 0.5f, rect.y + rect.height * 0.5f)
            .nor()
            .scl(type.speed * deltaTime);

        rect.setPosition(rect.getX() + direction.x, rect.getY() + direction.y);

        if (hitFlashTimer > 0f) {
            hitFlashTimer -= deltaTime;
        }
        if (statusTimer > 0f) {
            statusTimer -= deltaTime;
        }
    }

    @Override
    public void draw(Batch batch) {
        if (texture == null) return;

        if (statusTimer > 0f) {
            // Flicker between the status color and white for a jolt/shock feel.
            boolean flicker = ((int) (statusTimer * 40f)) % 2 == 0;
            batch.setColor(flicker ? statusColor : Color.WHITE);
        } else {
            batch.setColor(hitFlashTimer > 0f ? Color.WHITE : type.tint);
        }
        batch.draw(texture, rect.x, rect.y, rect.width, rect.height);
        batch.setColor(Color.WHITE);
    }

    /** @return true if this hit killed the enemy. */
    public boolean takeDamage(float amount) {
        health -= amount;
        hitFlashTimer = HIT_FLASH_DURATION;
        return health <= 0f;
    }

    /** Flashes this enemy with a colored tint for {@code duration} seconds — used to show an external effect landing on it (e.g. a lightning strike). */
    public void applyStatusFlash(Color color, float duration) {
        this.statusColor = color;
        this.statusTimer = duration;
    }

    public int getXpValue() {
        return type.xpValue;
    }

    public Type getType() {
        return type;
    }

    public static Enemy spawn(Viewport gameViewport, Texture texture, Player player, Type type) {
        int edge = MathUtils.random(3); // 0: top, 1: right, 2: bottom, 3: left
        float x, y;

        switch (edge) {
            case 0 -> { // Top
                x = MathUtils.random(0, 1) * gameViewport.getWorldWidth();
                y = gameViewport.getWorldHeight();
            }
            case 1 -> { // Right
                x = gameViewport.getWorldWidth();
                y = MathUtils.random(0, 1) * gameViewport.getWorldHeight();
            }
            case 2 -> { // Bottom
                x = MathUtils.random(0, 1) * gameViewport.getWorldWidth();
                y = -texture.getHeight() * SCALE;
            }
            default -> { // Left
                x = -texture.getWidth() * SCALE;
                y = MathUtils.random(0, 1) * gameViewport.getWorldHeight();
            }
        }

        return new Enemy(x, y, texture, player, type);
    }

    /** Tougher/faster enemies gradually unlock as the score grows. */
    public static Type randomType(int score) {
        if (score > 40 && MathUtils.randomBoolean(0.2f)) return Type.BRUTE;
        if (score > 15 && MathUtils.randomBoolean(0.35f)) return Type.RUNNER;
        return Type.SLIME;
    }
}
