package io.github.SurvivalGame;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

public class Player extends GameObject {
    private static final float SCALE = 1 / 32f;
    private static final float SPEED = 2f;
    private static final float LIFE = 5f;
    private static final float ATTACK_COOLDOWN = 1.6f;

    private int level = 1;
    private int experience = 0;
    private int experienceToNextLevel = 10;
    private final Vector2 moveDirection = new Vector2(0, 0);
    private final Vector2 lastDirection = new Vector2(1, 0); // used for attacks; default is right
    private float life = LIFE;
    private float attackTimer;
    private final Array<Attack> attacks = new Array<>();
    private final Animation<Texture> attackAnimation;
    private final Viewport gameViewport;
    private final Sound attackSfx;

    // Damage feedback
    private float invincibleTimer;

    // Dash
    private float dashTimer;
    private float dashCooldownTimer;

    // Buffs granted by AbilityPickups
    private float shieldTimer;
    private float hasteTimer;
    private float magnetTimer;
    private float stormTimer;

    public Player(float x, float y,
                  Viewport gameViewport,
                  Texture texture,
                  Animation<Texture> attackAnimation,
                  Sound attackSfx) {
        super(x, y, texture.getWidth() * SCALE, texture.getHeight() * SCALE, texture);
        reset(x, y);
        this.gameViewport = gameViewport;
        this.attackAnimation = attackAnimation;
        this.attackSfx = attackSfx;
    }

    public void reset(float x, float y) {
        rect.setPosition(x, y);

        life = LIFE;
        attackTimer = ATTACK_COOLDOWN;
        invincibleTimer = 0f;
        dashTimer = 0f;
        dashCooldownTimer = 0f;
        shieldTimer = 0f;
        hasteTimer = 0f;
        magnetTimer = 0f;
        stormTimer = 0f;

        level = 1;
        experience = 0;
        experienceToNextLevel = 10;

        attacks.clear();
    }

    public boolean addExperience(int amount) {
        experience += amount;

        if (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            level++;
            experienceToNextLevel = Math.round(experienceToNextLevel * 1.35f);
            return true;
        }

        return false;
    }

    public int getLevel() {
        return level;
    }

    public int getExperience() {
        return experience;
    }

    public int getExperienceToNextLevel() {
        return experienceToNextLevel;
    }

    @Override
    void update(float deltaTime) {
        if (invincibleTimer > 0f) invincibleTimer -= deltaTime;
        if (dashTimer > 0f) dashTimer -= deltaTime;
        if (dashCooldownTimer > 0f) dashCooldownTimer -= deltaTime;
        if (shieldTimer > 0f) shieldTimer -= deltaTime;
        if (hasteTimer > 0f) hasteTimer -= deltaTime;
        if (magnetTimer > 0f) magnetTimer -= deltaTime;
        if (stormTimer > 0f) stormTimer -= deltaTime;

        var iterator = attacks.iterator();
        while (iterator.hasNext()) {
            Attack attack = iterator.next();
            attack.update(deltaTime);
            if (attack.isDone()) {
                iterator.remove();
            }
        }

        move(deltaTime);
    }

    /**
     * Called separately from update() so it can see the current enemy list.
     * Swings toward the nearest threats — from every side at once if the player is surrounded.
     */
    public void updateCombat(float deltaTime, Array<Enemy> nearbyEnemies) {
        if (canAttack(deltaTime)) {
            spawnAttacks(nearbyEnemies);
        }
    }

    private void spawnAttacks(Array<Enemy> nearbyEnemies) {
        Vector2 playerCenter = getCenter(new Vector2());
        attackSfx.play();

        Array<Enemy> targets = findNearestEnemies(playerCenter, nearbyEnemies);

        if (targets.isEmpty()) {
            attacks.add(new Attack(playerCenter, lastDirection, attackAnimation));
            return;
        }

        for (Enemy enemy : targets) {
            Vector2 direction = enemy.getCenter(new Vector2()).sub(playerCenter);
            if (direction.isZero()) {
                direction.set(lastDirection);
            } else {
                direction.nor();
            }
            attacks.add(new Attack(playerCenter, direction, attackAnimation));
        }
    }

    private Array<Enemy> findNearestEnemies(Vector2 playerCenter, Array<Enemy> allEnemies) {
        Array<Enemy> inRange = new Array<>();

        for (Enemy enemy : allEnemies) {
            if (playerCenter.dst(enemy.getCenter(new Vector2())) <= GameConfig.SWORD_DETECTION_RADIUS) {
                inRange.add(enemy);
            }
        }

        inRange.sort((a, b) -> Float.compare(
            playerCenter.dst2(a.getCenter(new Vector2())),
            playerCenter.dst2(b.getCenter(new Vector2()))
        ));

        if (inRange.size > GameConfig.SWORD_MAX_TARGETS) {
            inRange.truncate(GameConfig.SWORD_MAX_TARGETS);
        }

        return inRange;
    }

    private void move(float deltaTime) {
        if (moveDirection.isZero()) return;

        float speed = SPEED;
        if (isDashing()) {
            speed *= GameConfig.DASH_SPEED_MULTIPLIER;
        } else if (isHasted()) {
            speed *= GameConfig.HASTE_MULTIPLIER;
        }

        float newX = rect.getX() + moveDirection.x * speed * deltaTime;
        float newY = rect.getY() + moveDirection.y * speed * deltaTime;

        newX = Math.max(0f, Math.min(newX, gameViewport.getWorldWidth() - rect.getWidth()));
        newY = Math.max(0f, Math.min(newY, gameViewport.getWorldHeight() - rect.getHeight()));

        rect.setPosition(newX, newY);
    }

    public void changeDirection(Vector2 direction) {
        if (!direction.isZero()) {
            lastDirection.set(direction);
        }
        moveDirection.set(direction);
    }

    public boolean tryDash() {
        if (dashCooldownTimer > 0f || moveDirection.isZero()) {
            return false;
        }

        dashTimer = GameConfig.DASH_DURATION;
        dashCooldownTimer = GameConfig.DASH_COOLDOWN;
        invincibleTimer = Math.max(invincibleTimer, GameConfig.DASH_DURATION);
        return true;
    }

    public boolean isDashing() {
        return dashTimer > 0f;
    }

    public float getDashCooldownPercent() {
        return dashCooldownTimer <= 0f ? 1f : 1f - (dashCooldownTimer / GameConfig.DASH_COOLDOWN);
    }

    public void applyAbility(AbilityPickup.Type type) {
        switch (type) {
            case SHIELD -> shieldTimer = GameConfig.SHIELD_DURATION;
            case HASTE -> hasteTimer = GameConfig.HASTE_DURATION;
            case MAGNET -> magnetTimer = GameConfig.MAGNET_DURATION;
            case STORM -> stormTimer = GameConfig.STORM_DURATION;
        }
    }

    public boolean isShielded() {
        return shieldTimer > 0f;
    }

    public boolean isHasted() {
        return hasteTimer > 0f;
    }

    public boolean isMagnetActive() {
        return magnetTimer > 0f;
    }

    public boolean isStormActive() {
        return stormTimer > 0f;
    }

    public float getShieldPercent() {
        return shieldTimer / GameConfig.SHIELD_DURATION;
    }

    public float getHastePercent() {
        return hasteTimer / GameConfig.HASTE_DURATION;
    }

    public float getMagnetPercent() {
        return magnetTimer / GameConfig.MAGNET_DURATION;
    }

    public float getStormPercent() {
        return stormTimer / GameConfig.STORM_DURATION;
    }

    public float getPickupRadius() {
        return GameConfig.BASE_PICKUP_RADIUS + (isMagnetActive() ? GameConfig.MAGNET_RADIUS_BONUS : 0f);
    }

    public float getLife() {
        return life;
    }

    public float getMaxLife() {
        return LIFE;
    }

    public void heal(float amount) {
        life = Math.min(LIFE, life + amount);
    }

    /** Damage is ignored while shielded, dashing/dash-invincible, or mid post-hit i-frames. */
    public void subLife(float amount) {
        if (invincibleTimer > 0f || shieldTimer > 0f) {
            return;
        }
        life -= amount;
        invincibleTimer = GameConfig.PLAYER_INVINCIBILITY_DURATION;
    }

    public boolean isInvincible() {
        return invincibleTimer > 0f || shieldTimer > 0f;
    }

    public boolean isDead() {
        return life <= 0f;
    }

    public boolean canAttack(float delta) {
        attackTimer -= delta;
        if (attackTimer <= 0f) {
            attackTimer = ATTACK_COOLDOWN;
            return true;
        }

        return false;
    }

    public Array<Attack> getAttacks() {
        return attacks;
    }

    @Override
    public void draw(Batch batch) {
        if (texture == null) return;

        // Flicker while briefly invincible from a hit — but not while shielded or dashing,
        // since those already have their own visual treatment.
        if (invincibleTimer > 0f && shieldTimer <= 0f && !isDashing() && ((int) (invincibleTimer * 20)) % 2 == 0) {
            return;
        }

        super.draw(batch);
    }
}
