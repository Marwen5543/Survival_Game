package io.github.SurvivalGame;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;

/**
 * Everything the player can pick from the level-up menu: new weapons, weapon levels and passives.
 * The actual numbers live where they're used (WeaponSystem for weapons, Player for passives) —
 * this enum only owns the metadata shown on the cards and the "which 3 do I offer?" roll.
 */
public enum Upgrade {
    // --- Weapons (auto-fire, levelled up like in any good survivors-like) ---
    PISTOL("Flintlock Pistol", true, 5, new Color(1f, 0.82f, 0.3f, 1f), "P"),
    BLADES("Spirit Blades", true, 5, new Color(0.45f, 0.9f, 1f, 1f), "B"),
    NOVA("Shockwave", true, 5, new Color(1f, 0.5f, 0.3f, 1f), "N"),
    METEOR("Falling Stars", true, 5, new Color(0.95f, 0.4f, 1f, 1f), "T"),

    // --- Passives ---
    SWORD_MASTERY("Blade Mastery", false, 3, new Color(0.85f, 0.88f, 0.95f, 1f), "S"),
    DAMAGE("Sharpened Steel", false, 5, new Color(1f, 0.35f, 0.35f, 1f), "D"),
    ATTACK_SPEED("Quick Hands", false, 5, new Color(1f, 0.5f, 0.8f, 1f), "Q"),
    MOVE_SPEED("Swift Boots", false, 5, new Color(0.4f, 1f, 0.5f, 1f), "M"),
    MAX_HEALTH("Vitality", false, 5, new Color(1f, 0.4f, 0.5f, 1f), "V"),
    REGEN("Regeneration", false, 5, new Color(0.5f, 1f, 0.8f, 1f), "R"),
    SCAVENGER("Scavenger", false, 5, new Color(0.6f, 0.7f, 1f, 1f), "G"),
    DASH("Light Feet", false, 4, new Color(0.7f, 0.5f, 1f, 1f), "L"),
    GREED("Greed", false, 5, new Color(1f, 0.9f, 0.2f, 1f), "E"),
    ARMOR("Iron Skin", false, 5, new Color(0.6f, 0.68f, 0.78f, 1f), "A"),
    VAMPIRE("Bloodthirst", false, 3, new Color(0.85f, 0.1f, 0.3f, 1f), "Y"),
    PHOENIX("Phoenix Feather", false, 1, new Color(1f, 0.6f, 0.1f, 1f), "F"),

    // --- Fallback: only offered when the pool runs dry, can be taken forever ---
    HEAL("Second Wind", false, Integer.MAX_VALUE, new Color(1f, 0.6f, 0.6f, 1f), "+");

    final String title;
    final boolean weapon;
    final int maxLevel;
    final Color color;
    final String icon;

    Upgrade(String title, boolean weapon, int maxLevel, Color color, String icon) {
        this.title = title;
        this.weapon = weapon;
        this.maxLevel = maxLevel;
        this.color = color;
        this.icon = icon;
    }

    /** Pips on the card only make sense for upgrades with a small, fixed number of levels. */
    boolean hasPips() {
        return maxLevel <= 6;
    }

    String category() {
        if (this == HEAL) return "RECOVERY";
        return weapon ? "WEAPON" : "PASSIVE";
    }

    /** Card description for taking this upgrade to {@code level} (1 = first time). */
    String describe(int level) {
        return switch (this) {
            case PISTOL -> switch (level) {
                case 1 -> "Auto-fires at the nearest enemy. Reliable, accurate, always loaded.";
                case 2 -> "Faster fire rate and +25% bullet damage.";
                case 3 -> "Dual barrels: 2 bullets per volley, hitting harder.";
                case 4 -> "Faster fire rate. Bullets now pierce 1 enemy.";
                default -> "Triple barrels: 3 bullets per volley, piercing 2 enemies.";
            };
            case BLADES -> switch (level) {
                case 1 -> "Two blades orbit you and shred anything they touch.";
                case 2 -> "A third blade joins the orbit. More damage.";
                case 3 -> "Fourth blade and a faster spin.";
                case 4 -> "Fifth blade on a wider orbit.";
                default -> "Six blades: a whirling wall of steel.";
            };
            case NOVA -> switch (level) {
                case 1 -> "Unleash a shockwave every few seconds that damages and knocks enemies back.";
                case 2 -> "Bigger wave, faster recharge, more knockback.";
                case 3 -> "Bigger wave, faster recharge, +50% damage.";
                case 4 -> "The shockwave now reaches across a huge area.";
                default -> "Maximum radius, minimum cooldown, devastating damage.";
            };
            case METEOR -> switch (level) {
                case 1 -> "Stars rain on a random enemy after a short warning. Big area damage.";
                case 2 -> "Two stars per volley and bigger craters.";
                case 3 -> "Faster recharge and heavier impact.";
                case 4 -> "Wider blasts, harder hits.";
                default -> "Meteor storm: three stars per volley, devastating.";
            };
            case SWORD_MASTERY -> "Your sword swings at +1 extra target at once.";
            case DAMAGE -> "+20% damage with every weapon.";
            case ATTACK_SPEED -> "-10% cooldown on every weapon.";
            case MOVE_SPEED -> "+10% movement speed.";
            case MAX_HEALTH -> "+1 max health and heal 1 right now.";
            case REGEN -> "Recover +0.1 health per second.";
            case SCAVENGER -> "+0.5 pickup radius. Gems fly to you from farther away.";
            case DASH -> "-12% dash cooldown. Dash more, dodge more.";
            case GREED -> "+15% XP from every gem. Level faster.";
            case ARMOR -> "-10% damage taken from everything.";
            case VAMPIRE -> "+4% chance per kill to heal a little.";
            case PHOENIX -> "Once per run: cheat death, revive with 60% health and blast everything away.";
            case HEAL -> "Restore half of your maximum health.";
        };
    }

    /**
     * Picks up to {@code count} distinct upgrades the player can still level.
     * Guarantees a weapon option while any weapon is still locked, so the first level-ups
     * always offer something exciting. Falls back to {@link #HEAL} if the pool runs dry.
     */
    static Upgrade[] roll(Player player, int count) {
        Array<Upgrade> pool = new Array<>();
        for (Upgrade upgrade : values()) {
            if (upgrade != HEAL && player.getUpgradeLevel(upgrade) < upgrade.maxLevel) {
                pool.add(upgrade);
            }
        }
        pool.shuffle();

        Array<Upgrade> picks = new Array<>();
        for (int i = 0; i < pool.size && picks.size < count; i++) {
            picks.add(pool.get(i));
        }

        boolean hasWeapon = false;
        for (Upgrade upgrade : picks) {
            if (upgrade.weapon) {
                hasWeapon = true;
                break;
            }
        }
        if (!hasWeapon && picks.size > 0) {
            Array<Upgrade> locked = new Array<>();
            for (Upgrade upgrade : values()) {
                if (upgrade.weapon && player.getUpgradeLevel(upgrade) == 0) {
                    locked.add(upgrade);
                }
            }
            if (locked.size > 0) {
                picks.set(picks.size - 1, locked.random());
            }
        }

        if (picks.size < count) {
            picks.add(HEAL);
        }
        return picks.toArray(Upgrade.class);
    }
}
