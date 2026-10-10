package io.github.SurvivalGame;

import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.viewport.Viewport;

import java.util.EnumMap;
import java.util.Map;

public class Player extends GameObject {
    private static final float SCALE = 1 / 32f;
    private static final float SPEED = 2f;
    private static final float LIFE = 5f;
    private static final float ATTACK_COOLDOWN = 1.6f;

    // Per-level strength of the passive upgrades
    private static final float DAMAGE_PER_LEVEL = 0.20f;
    private static final float COOLDOWN_REDUCTION_PER_LEVEL = 0.10f;
    private static final float MIN_COOLDOWN_MULTIPLIER = 0.4f;
    private static final float SPEED_PER_LEVEL = 0.10f;
    private static final float REGEN_PER_LEVEL = 0.10f;
    private static final float PICKUP_RADIUS_PER_LEVEL = 0.5f;
    private static final float DASH_COOLDOWN_REDUCTION_PER_LEVEL = 0.12f;
    private static final float XP_BONUS_PER_LEVEL = 0.15f;
    private static final float ARMOR_PER_LEVEL = 0.10f;

    private int level = 1;
    private int experience = 0;
    private int experienceToNextLevel = 10;
    private float xpBuffer;
    private final Vector2 moveDirection = new Vector2(0, 0);
    private final Vector2 lastDirection = new Vector2(1, 0); // used for attacks; default is right
    private float life = LIFE;
    private float maxLife = LIFE;
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
    private float freezeTimer;
    private float overdriveTimer;

    // Slow (acid pools) — set by hazards
    private float slowTimer;

    // Phoenix Feather: one free revive per run
    private boolean phoenixUsed;
    private boolean revivePending;

    // Banked ability charges — picking up an orb stores a charge that the player spends on their own timing.
    private final Map<AbilityPickup.Type, Integer> abilityCharges = new EnumMap<>(AbilityPickup.Type.class);

    // Level-up choices made so far (weapons and passives). Permanent for the whole run.
    private final Map<Upgrade, Integer> upgradeLevels = new EnumMap<>(Upgrade.class);

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

        maxLife = LIFE;
        life = maxLife;
        attackTimer = ATTACK_COOLDOWN;
        invincibleTimer = 0f;
        dashTimer = 0f;
        dashCooldownTimer = 0f;
        shieldTimer = 0f;
        hasteTimer = 0f;
        magnetTimer = 0f;
        stormTimer = 0f;
        freezeTimer = 0f;
        overdriveTimer = 0f;
        slowTimer = 0f;
        phoenixUsed = false;
        revivePending = false;
        abilityCharges.clear();
        upgradeLevels.clear();

        level = 1;
        experience = 0;
        experienceToNextLevel = 10;
        xpBuffer = 0f;

        attacks.clear();
    }

    // ------------------------------------------------------------ Upgrades

    public int getUpgradeLevel(Upgrade upgrade) {
        return upgradeLevels.getOrDefault(upgrade, 0);
    }

    /** Applies a level-up pick. Instant effects (heals) happen here; the rest is read via the getters below. */
    public void applyUpgrade(Upgrade upgrade) {
        switch (upgrade) {
            case HEAL -> {
                heal(maxLife * 0.5f);
                return; // repeatable fallback, not tracked as a level
            }
            case MAX_HEALTH -> {
                maxLife += 1f;
                life += 1f;
            }
            default -> {
            }
        }

        int current = getUpgradeLevel(upgrade);
        if (current < upgrade.maxLevel) {
            upgradeLevels.put(upgrade, current + 1);
        }
    }

    /** Scales a weapon's base damage by Sharpened Steel and Overdrive. */
    public float getDamage(float base) {
        float multiplier = 1f + DAMAGE_PER_LEVEL * getUpgradeLevel(Upgrade.DAMAGE);
        if (isOverdriven()) multiplier *= 1.5f;
        return base * multiplier;
    }

    /** Multiply any weapon cooldown by this (Quick Hands passive, Overdrive buff). */
    public float getCooldownMultiplier() {
        float multiplier = 1f - COOLDOWN_REDUCTION_PER_LEVEL * getUpgradeLevel(Upgrade.ATTACK_SPEED);
        multiplier = Math.max(MIN_COOLDOWN_MULTIPLIER, multiplier);
        if (isOverdriven()) multiplier *= 0.55f;
        return multiplier;
    }

    public float getMoveSpeed() {
        return SPEED * (1f + SPEED_PER_LEVEL * getUpgradeLevel(Upgrade.MOVE_SPEED));
    }

    public int getSwordTargets() {
        return GameConfig.SWORD_MAX_TARGETS + getUpgradeLevel(Upgrade.SWORD_MASTERY);
    }

    public float getDashCooldown() {
        float multiplier = 1f - DASH_COOLDOWN_REDUCTION_PER_LEVEL * getUpgradeLevel(Upgrade.DASH);
        return GameConfig.DASH_COOLDOWN * multiplier;
    }

    // ------------------------------------------------------------ XP

    /** @return how many levels this XP granted (usually 0 or 1, more for big boss piles). */
    public int addExperience(int amount) {
        xpBuffer += amount * (1f + XP_BONUS_PER_LEVEL * getUpgradeLevel(Upgrade.GREED));
        int gained = (int) xpBuffer;
        xpBuffer -= gained;
        experience += gained;

        int levels = 0;
        while (experience >= experienceToNextLevel) {
            experience -= experienceToNextLevel;
            level++;
            levels++;
            experienceToNextLevel = Math.round(experienceToNextLevel * 1.35f);
        }
        return levels;
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
        if (freezeTimer > 0f) freezeTimer -= deltaTime;
        if (overdriveTimer > 0f) overdriveTimer -= deltaTime;
        if (slowTimer > 0f) slowTimer -= deltaTime;

        int regenLevel = getUpgradeLevel(Upgrade.REGEN);
        if (regenLevel > 0 && life > 0f && life < maxLife) {
            life = Math.min(maxLife, life + REGEN_PER_LEVEL * regenLevel * deltaTime);
        }

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
        attackSfx.play(Settings.sfxVolume);

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

        int maxTargets = getSwordTargets();
        if (inRange.size > maxTargets) {
            inRange.truncate(maxTargets);
        }

        return inRange;
    }

    private void move(float deltaTime) {
        if (moveDirection.isZero()) return;

        float speed = getMoveSpeed();
        if (isDashing()) {
            speed *= GameConfig.DASH_SPEED_MULTIPLIER;
        } else if (isHasted()) {
            speed *= GameConfig.HASTE_MULTIPLIER;
        }

        if (slowTimer > 0f) {
            speed *= GameConfig.PLAYER_SLOW_FACTOR;
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
        dashCooldownTimer = getDashCooldown();
        invincibleTimer = Math.max(invincibleTimer, GameConfig.DASH_DURATION);
        return true;
    }

    public boolean isDashing() {
        return dashTimer > 0f;
    }

    public float getDashCooldownPercent() {
        if (dashCooldownTimer <= 0f) return 1f;
        return Math.max(0f, Math.min(1f, 1f - (dashCooldownTimer / getDashCooldown())));
    }

    /** Banks one charge of this ability from a picked-up orb, capped so the hotbar can't overflow. */
    public void addAbilityCharge(AbilityPickup.Type type) {
        int current = abilityCharges.getOrDefault(type, 0);
        abilityCharges.put(type, Math.min(current + 1, GameConfig.MAX_ABILITY_STACK));
    }

    public int getAbilityCharge(AbilityPickup.Type type) {
        return abilityCharges.getOrDefault(type, 0);
    }

    /**
     * Spends one banked charge of this ability. If it's already active, the new duration stacks
     * on top of what's left (capped). NUKE is instant — GameScreen performs the blast.
     *
     * @return true if a charge was available and the ability activated.
     */
    public boolean tryUseAbility(AbilityPickup.Type type) {
        int current = abilityCharges.getOrDefault(type, 0);
        if (current <= 0) {
            return false;
        }

        abilityCharges.put(type, current - 1);
        switch (type) {
            case SHIELD -> shieldTimer = stack(shieldTimer, GameConfig.SHIELD_DURATION);
            case HASTE -> hasteTimer = stack(hasteTimer, GameConfig.HASTE_DURATION);
            case MAGNET -> magnetTimer = stack(magnetTimer, GameConfig.MAGNET_DURATION);
            case STORM -> stormTimer = stack(stormTimer, GameConfig.STORM_DURATION);
            case FREEZE -> freezeTimer = stack(freezeTimer, GameConfig.FREEZE_DURATION);
            case OVERDRIVE -> overdriveTimer = stack(overdriveTimer, GameConfig.OVERDRIVE_DURATION);
            case NUKE -> {
                // instant effect, handled by GameScreen
            }
        }
        return true;
    }

    /** Adds a fresh duration on top of what's left, capped at a multiple of the base duration. */
    private float stack(float currentTimer, float duration) {
        float cap = duration * GameConfig.ABILITY_STACK_DURATION_CAP_MULTIPLIER;
        return Math.min(currentTimer + duration, cap);
    }

    /** Called by acid pools: slows movement for a moment. */
    public void applySlow(float seconds) {
        slowTimer = Math.max(slowTimer, seconds);
    }

    public boolean isSlowed() {
        return slowTimer > 0f;
    }

    /** Called by gravity wells: drags the player, kept inside the arena. */
    public void applyPull(float dx, float dy) {
        float nx = Math.max(0f, Math.min(rect.x + dx, gameViewport.getWorldWidth() - rect.width));
        float ny = Math.max(0f, Math.min(rect.y + dy, gameViewport.getWorldHeight() - rect.height));
        rect.setPosition(nx, ny);
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

    public boolean isFreezeActive() {
        return freezeTimer > 0f;
    }

    public boolean isOverdriven() {
        return overdriveTimer > 0f;
    }

    /** Percentages can exceed 1 while a buff is stacked beyond its base duration. */
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

    public float getFreezePercent() {
        return freezeTimer / GameConfig.FREEZE_DURATION;
    }

    public float getOverdrivePercent() {
        return overdriveTimer / GameConfig.OVERDRIVE_DURATION;
    }

    public float getPickupRadius() {
        return GameConfig.BASE_PICKUP_RADIUS
            + PICKUP_RADIUS_PER_LEVEL * getUpgradeLevel(Upgrade.SCAVENGER)
            + (isMagnetActive() ? GameConfig.MAGNET_RADIUS_BONUS : 0f);
    }

    public float getLife() {
        return life;
    }

    public float getMaxLife() {
        return maxLife;
    }

    public void heal(float amount) {
        life = Math.min(maxLife, life + amount);
    }

    /**
     * Damage is ignored while shielded, dashing/dash-invincible, or mid post-hit i-frames.
     * Iron Skin reduces it; the Phoenix Feather turns the first lethal hit into a revive.
     */
    public void subLife(float amount) {
        if (invincibleTimer > 0f || shieldTimer > 0f) {
            return;
        }
        float armor = Math.max(0.3f, 1f - ARMOR_PER_LEVEL * getUpgradeLevel(Upgrade.ARMOR));
        life -= amount * armor;
        invincibleTimer = GameConfig.PLAYER_INVINCIBILITY_DURATION;

        if (life <= 0f && getUpgradeLevel(Upgrade.PHOENIX) > 0 && !phoenixUsed) {
            phoenixUsed = true;
            revivePending = true;
            life = maxLife * 0.6f;
            invincibleTimer = 3f;
        }
    }

    /** True exactly once after the Phoenix Feather saved the player — GameScreen plays the blast. */
    public boolean consumeRevive() {
        if (revivePending) {
            revivePending = false;
            return true;
        }
        return false;
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
            attackTimer = ATTACK_COOLDOWN * getCooldownMultiplier();
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
