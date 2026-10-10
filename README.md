
# SurvivalGame — Endless Night

A fast, top-down **survivors-like** built with **Java + libGDX**. Fight an endless horde with auto-firing weapons,
level up, bank and combine powerful abilities, dodge telegraphed boss attacks and survive as long as you can.

> **Status note:** the code was written and structured to drop straight into a libGDX project, but it has not been
> compiled by its author's tooling. If your first build reports a small error, fix or report it — everything is plain
> libGDX API (no custom engine code).

---

## Table of contents

1. [Features at a glance](#features-at-a-glance)
2. [Requirements & setup](#requirements--setup)
3. [Controls](#controls)
4. [How to play](#how-to-play)
5. [Weapons & upgrades](#weapons--upgrades)
6. [Abilities (hotkeys 1–7)](#abilities-hotkeys-17)
7. [Enemies](#enemies)
8. [Bosses](#bosses)
9. [Hazards (telegraphed attacks)](#hazards-telegraphed-attacks)
10. [Waves, events & combo](#waves-events--combo)
11. [Menus & options](#menus--options)
12. [Visual effects](#visual-effects)
13. [Assets](#assets)
14. [Project structure](#project-structure)
15. [Architecture notes](#architecture-notes)
16. [Tuning & customization](#tuning--customization)
17. [Troubleshooting](#troubleshooting)
18. [Ideas for future work](#ideas-for-future-work)

---

## Features at a glance

- **Main menu** with animated backdrop, **Options**, **How to Play** and persistent best-run records.
- **Options** (saved between sessions): music & SFX volume, 4 field themes, 5 resolutions + fullscreen,
  4 difficulties, enemy speed, spawn rate, enemy colour style, ability transparency, and toggles for new enemy types,
  boss fights and random events.
- **4 weapons** that auto-fire and level up (sword, pistol, spirit blades, shockwave, falling stars) and
  **12 passive upgrades**.
- **7 banked abilities** with real animations: Shield, Haste, Magnet, Storm, Freeze, Supernova, Overdrive.
- **12 enemy types**, each with its own painted sprite and its own trick, plus **elite affixes**.
- **4 bosses** with unique sprites, full movesets, intro drops, 3 phases and stun windows.
- **Telegraphed hazards**: blasts, acid pools, shockwave rings, lasers and gravity wells that warn (and flash)
  before they hurt. Dashing dodges all of them.
- **Random events**: Blood Moon, Swarm, Treasure Goblin and enemy waves.
- **Combo system**, **Phoenix revive**, depth-sorted sprites, screen shake, hit-stop and slow-mo on boss kills.
- **No art required**: every enemy, boss and new ability icon is generated in code. Drop in PNGs to override any of them.

---

## Requirements & setup

| Requirement | Notes |
|---|---|
| **Java 17+** | The code uses switch expressions / arrow-switch and `var`. |
| **libGDX** | Core module plus **gdx-freetype** (the font is generated from a `.ttf`). |
| **LWJGL3 desktop backend** | Needed for runtime resolution / fullscreen changes (`setWindowedMode`, `setFullscreenMode`). |
| **Gradle** (via the standard `gdx-liftoff` template) | Any libGDX project layout works. |

### Quick start

1. Create (or open) a libGDX project with the package `io.github.SurvivalGame`
   (e.g. generated with **gdx-liftoff**, enabling the **FreeType** extension and the **LWJGL3** platform).
2. Copy every `*.java` file from this project into `core/src/main/java/io/github/SurvivalGame/`
   (overwrite existing files).
3. Put the required [assets](#assets) into the `assets/` folder.
4. Make sure the launcher creates `new GdxGame()`.
5. Run the desktop launcher (`./gradlew lwjgl3:run`).

> Settings and records are stored with libGDX `Preferences` under the name `survival-game-settings`.

---

## Controls

### In game

| Key | Action |
|---|---|
| `W` `A` `S` `D` | Move |
| `Shift` | Dash — brief invincibility, **dodges every hazard and projectile** |
| `1`–`7` | Use a banked ability (Shield, Haste, Magnet, Storm, Freeze, Supernova, Overdrive) |
| `Esc` | Pause / resume |
| `M` | Back to the main menu (while paused or dead) |
| `R` | Restart (when dead) |
| `1` `2` `3` / click | Pick an upgrade on the level-up screen |

### Menus

| Key | Action |
|---|---|
| `W` / `S` (or arrows) | Move selection |
| `A` / `D` (or arrows) | Change an option value |
| `Enter` / `Space` | Select / toggle |
| Mouse | Hover and click (left half of a value = previous, right half = next) |
| `Esc` | Back (Options saves on exit) |

---

## How to play

- Your **sword**, plus any weapons you pick, **fire automatically**. You only move, dash and choose upgrades.
- Kill enemies to drop **XP gems**. Walk near them to collect. Levelling up pauses the game and offers **3 upgrades**.
- Glowing **ability orbs** spawn on the field. Touching one **banks a charge** (max 5 per ability).
  You decide when to spend each charge with keys `1`–`7`. Using the same ability again **stacks its duration**
  (up to 3× the base time), and different abilities can be layered.
- **Watch the ground.** Almost every enemy attack is telegraphed with a flashing zone. Move out of it or dash through it.
- Every ~75 seconds a **boss** drops in. Beating one gives a large XP haul, healing, ability orbs and a **free upgrade**.
- Your best kill count and longest survival time are saved and shown in the main menu.

---

## Weapons & upgrades

### Weapons (auto-fire, up to level 5)

| Weapon | Behaviour |
|---|---|
| **Sword** (starting) | Swings at the nearest enemies, up to 3 targets (more with Blade Mastery). |
| **Flintlock Pistol** | Fires at the nearest enemy; more barrels and piercing at higher levels. |
| **Spirit Blades** | Blades orbit you and shred anything they touch (2 → 6 blades). |
| **Shockwave** | Periodic expanding ring that damages and knocks enemies back. |
| **Falling Stars** | Meteors rain on random enemies after a short warning ring (area damage). |

### Passive upgrades

| Upgrade | Max level | Effect per level |
|---|---|---|
| Blade Mastery | 3 | Sword hits +1 extra target |
| Sharpened Steel | 5 | +20% damage (all weapons) |
| Quick Hands | 5 | −10% cooldowns |
| Swift Boots | 5 | +10% movement speed |
| Vitality | 5 | +1 max health (and heal 1) |
| Regeneration | 5 | +0.1 health / second |
| Scavenger | 5 | +0.5 pickup radius |
| Light Feet | 4 | −12% dash cooldown |
| Greed | 5 | +15% XP from gems |
| Iron Skin | 5 | −10% damage taken |
| Bloodthirst | 3 | +4% chance to heal on kill |
| **Phoenix Feather** | 1 | Once per run: cheat death, revive at 60% health and blast the screen |
| Second Wind | ∞ | Fallback when the pool runs dry: restore half your max health |

The level-up roll always offers a weapon while any weapon is still locked, so early levels feel exciting.

---

## Abilities (hotkeys 1–7)

| Key | Ability | Duration | Effect | Animation |
|---|---|---|---|---|
| `1` | **Shield** | 4 s | Immune to damage; absorbs projectiles | Rotating hex barrier, orbiting shards, ripples on block, blinks before expiring |
| `2` | **Haste** | 5 s | ×1.6 movement speed | Afterimages and wind streaks |
| `3` | **Magnet** | 6 s | Larger pickup radius | Rings contracting inward, motes spiralling to you |
| `4` | **Storm** | 5 s | Lightning strikes clusters of enemies | A thundercloud follows you; bolts fall from it with rain underneath |
| `5` | **Freeze** | 4 s | Enemies move at 12% speed; **frozen enemies take +50% damage** | Frost-nova shockwave with ice spikes, orbiting crystals, snow, ice on enemies, frost along screen edges |
| `6` | **Supernova** | instant | Damages **every** enemy on screen; boss takes heavy damage; clears hazards and projectiles | ~0.45 s charge-up spiral, then multi-ring blast, rays, scorch mark, hit-stop |
| `7` | **Overdrive** | 6 s | ×1.5 damage and ~2× attack rate | Flame aura, orbiting sparks, RPM gauge ring |

Activating any ability also plays an animated rune glyph on the ground. Icons are generated in code
unless you provide `*_icon.png` files.

---

## Enemies

Every enemy has its own painted sprite, squash/stretch animation, a ground shadow and its own ability.
Types unlock as your score rises (the *New Enemy Types* option can disable the special ones).

| Enemy | Unlocks at score | Trick |
|---|---|---|
| **Slime** | start | Plain chaser. |
| **Runner** | 15 | Quivers (wind-up), then **lunges** at you. |
| **Splitter** | 10 | Bursts into two Minis on death. |
| **Mini** | — | Fast little swarmer (from Splitters, Summoners, Hive Queen). |
| **Spitter** | 25 | Keeps its distance and fires 3-orb volleys. |
| **Bomber** | 30 | Lights a fuse beside you and explodes (hurts other enemies too). |
| **Shade** | 35 | Fades out and **blinks next to you**, leaving a marker, then stabs. |
| **Brute** | 40 | Raises its fists, then **slams** (blast + shockwave). |
| **Warden** | 45 | Projects an aura that **shields all nearby enemies** (−60% damage). Kill it first. |
| **Sniper** | 50 | Floating eye that tracks you with a long **laser**, then fires. |
| **Summoner** | 60 | Draws a magic circle and conjures Minis. |
| **Treasure Goblin** | event only | Runs away for 9 s; drops big XP and loot if you catch it. |

### Elites

After about a minute, some enemies spawn as **golden Elites**: bigger, 3× health, 3× XP, extra drops, plus a random affix.

| Affix | Effect |
|---|---|
| **Swift** | +40% speed |
| **Volatile** | Leaves a delayed blast where it dies |
| **Armored** | −40% damage taken |

Enemy health also scales with time and with the chosen difficulty.

---

## Bosses

Bosses **drop in from above** (invulnerable during the intro, with a landing shockwave), show a cast-name under the
boss bar, and escalate at **60%** and **30%** health. Order rotates through all four; each later boss has more health.

| Boss | Moveset |
|---|---|
| **The Juggernaut** (iron golem) | **Iron Charge** (chains up to 3×, red flashing lane warning) · **Seismic Slam** (blast, shockwave, falling rocks) · **Rockfall**. After a charge or slam it is **stunned and takes +50% damage**. |
| **The Void Warlock** | **Void Spiral** bullet-hell · **Gravity Wells** that drag you in · rotating **Rift Lasers** · **Phase Shift** teleports with bullet rings. |
| **The Hive Queen** | **Brood Call** (minions) · **Acid Rain** (slowing pools) · **Spore Bloom** (bullet rings) · **Royal Guard** (summons real enemies, including Spitters/Bombers/Summoners later). |
| **The Storm Wraith** | **Lightning Rain** · sweeping **Thunder Beam** · **Arc Volley** (fast aimed orbs) · **Storm Step** teleports (phase 2+). |

**Rewards:** 18 big XP gems, 2 hearts, 3 ability orbs and one free level-up, with an explosion sequence and slow-motion.

| Setting | Default |
|---|---|
| First boss | 55 s |
| Gap after a boss dies | 75 s |
| Base health | 110 (+55% per boss beaten, × difficulty) |

---

## Hazards (telegraphed attacks)

All hazards **warn first** (flashing, with a crisp outline), then hurt. They are semi-transparent so you can still
see what is happening beneath them.

| Hazard | Behaviour |
|---|---|
| **Blast** | Circle that fills and flashes faster, then detonates. (Lightning strikes are a skinned Blast.) |
| **Pool** | Lingering acid puddle; damages every ~0.7 s and **slows** you. |
| **Ring** | Expanding shockwave; damages once as it passes — dash through it. |
| **Beam** | Fixed, sweeping or player-tracking laser with a telegraph and a lock-on window. |
| **Well** | Gravity well that drags you toward its centre. |

Limits stop late-game screens from being buried: there are caps on simultaneous hazards and projectiles.

---

## Waves, events & combo

- **Waves** (every 30 s, first at 20 s): a pack rushes in from one screen edge.
- **Random events** (every ~45 s): **Blood Moon** (20 s: enemies ×1.3 speed and spawn faster, **XP doubled**, red screen-edge glow),
  **Swarm** (14 runners ring you), **Treasure Goblin**.
- **Combo**: chain kills within 2.5 s. Up to ×2 XP at a 50-kill combo.
- Events are paused while a boss is active, and can be disabled in Options.

---

## Menus & options

### Options screen

| Option | Values |
|---|---|
| Music Volume / Sound Effects | 0–100% in 10% steps (music applies live) |
| Field Theme | Meadow, Ember Wastes, Frozen Tundra, The Void |
| Resolution | 1280×720, 1366×768, 1600×900, 1920×1080, 2560×1440 |
| Fullscreen | On / Off |
| Difficulty | Easy (0.75× health, 0.7× damage) · Normal · Hard (1.4×, 1.35×) · Nightmare (2×, 1.8×) |
| Enemy Speed | 50%–200% |
| Spawn Rate | 50%–300% |
| Enemy Style | Classic, Neon, Shadow, Rainbow |
| Ability Transparency | Off, Balanced, Max (how see-through enemy ability zones are) |
| New Enemy Types | On / Off |
| Boss Fights | On / Off |
| Random Events | On / Off |

Field themes change the background tint, ground decorations (grass, rocks, cracks, runes) and ambient particles
(pollen, rising embers, falling snow, void motes).

---

## Visual effects

- Animated, **never flat full-screen tints**: edge vignettes for Blood Moon, low health, boss phases, hits and acid.
- Banner system with sliding bars and a punch-in title for boss warnings, events and loot.
- Depth-sorted enemy sprites, ground shadows, health bars for tough/damaged enemies.
- Hit-stop on big impacts, slow-motion on boss defeat, camera shake with magnitude by event.
- Particle budget (max 450) that scales down in crowded fights.

---

## Assets

Put these in the `assets/` folder.

### Required

| File | Purpose |
|---|---|
| `bgd.png` | Tiling background |
| `player.png` | Player sprite |
| `slime.png` | Only used to **measure the base enemy size** (so enemies keep your original scale) |
| `CherryCreamSoda-Regular.ttf` | UI font (generated by FreeType at size 24) |
| `nightsplitter.mp3` | Music (shared between menus and game) |
| `slash.wav` | Sword swing sound |
| `slash_00.png` … `slash_13.png` | Sword swing animation frames |
| `buff_burst.png` | 8-column white burst sheet (used on level-up) |

### Optional overrides (all auto-generated if missing)

| File pattern | Overrides |
|---|---|
| `shield_icon.png`, `haste_icon.png`, `magnet_icon.png`, `storm_icon.png`, `freeze_icon.png`, `nuke_icon.png`, `overdrive_icon.png` | Ability icons |
| `enemy_slime.png`, `enemy_runner.png`, `enemy_brute.png`, `enemy_splitter.png`, `enemy_mini.png`, `enemy_spitter.png`, `enemy_bomber.png`, `enemy_goblin.png`, `enemy_shade.png`, `enemy_warden.png`, `enemy_summoner.png`, `enemy_sniper.png` | Enemy sprites (square images work best, facing right) |
| `boss_juggernaut.png`, `boss_warlock.png`, `boss_hive_queen.png`, `boss_storm_wraith.png` | Boss sprites (square, facing right) |

---

## Project structure

All files live in package `io.github.SurvivalGame`.

### Core & screens

| File | Role |
|---|---|
| `GdxGame` | Entry point. Owns the batch, shape renderer, font and the **shared music**; switches (and disposes) screens. |
| `MainMenuScreen` | Title menu with animated backdrop and saved records. |
| `OptionsScreen` | Keyboard + mouse options menu with live preview and saving. |
| `ControlsScreen` | "How to Play" screen. |
| `GameScreen` | The game loop: spawning, bosses, events, hazards, collisions, rendering and HUD. |
| `MenuBackdrop` | Animated gradient + drifting motes shared by all menus. |
| `LevelUpMenu` | "Choose 1 of 3" upgrade cards. |

### Gameplay objects

| File | Role |
|---|---|
| `Player` | Stats, XP, upgrades, dash, buffs, banked ability charges, slow/pull, Phoenix revive. |
| `Enemy` | All 12 enemy behaviours, elites/affixes, squash-and-stretch posing. |
| `Boss` | The four bosses (extends `Enemy`, so every weapon hits them). |
| `Hazard` | Telegraphed blasts, pools, rings, beams and wells. |
| `EnemyProjectile` | Hostile orbs from Spitters and bosses. |
| `Attack`, `Bullet`, `WeaponSystem` | Sword swings and the auto-weapons. |
| `AbilityPickup`, `HealthPickup`, `XpGem` | Collectibles. |
| `Upgrade` | Upgrade definitions, descriptions and the level-up roll. |
| `GameObject` | Base class (rectangle, texture, centre helper). |

### Visuals & config

| File | Role |
|---|---|
| `AbilityFx` | Every player-ability animation (barrier, trails, cloud, frost nova, supernova, flames …). |
| `SpriteFactory` | Paints enemy and boss sprites procedurally (or loads override PNGs). |
| `IconFactory` | Generates ability icons without art. |
| `GroundDecoration` | Theme-aware scenery. |
| `Particle`, `BuffEffect`, `LightningBolt` | Particles, level-up burst, lightning bolts. |
| `Settings` | Persistent options, records, themes, difficulties, resolutions. |
| `GameConfig` | **Every tunable number and asset path.** |

---

## Architecture notes

- **Enemies are plain `GameObject`s** with a state machine (`phase`, `phaseTimer`, `cooldown`). Their attacks do not hurt directly:
  they push `Hazard`s and `EnemyProjectile`s into shared lists via *sinks* injected by `GameScreen`.
- **`Boss extends Enemy`**, so weapons, abilities and the Storm all damage bosses with no special-casing.
- **Kill rewards live in one place** (`GameScreen.onEnemyKilled` / `onBossKilled`). Weapons only call `takeDamage()`;
  dead enemies are reaped after the weapon update.
- **Never nest libGDX `Array` iterators.** The code iterates enemies with backward index loops wherever
  rewards or spawns can happen mid-loop.
- **Layered rendering:** background → ground (scenery, scorch marks, hazards, shadows) → depth-sorted sprites →
  effects (projectiles, auras, particles, hazard outlines) → player on top → HUD.
- **Time scaling:** enemy simulation runs on `enemyDt` (slowed by Freeze, sped up by Blood Moon) while the player stays at real time;
  hit-stop and slow-mo scale the global delta.
- **Procedural art:** `SpriteFactory` paints with `Pixmap` and caches textures; a PNG with the matching name always wins.

---

## Tuning & customization

Almost everything is in **`GameConfig.java`**.

| Want to… | Change |
|---|---|
| Make the game harder / easier | `ENEMY_SPAWN_INTERVAL_START/MIN`, `DIFFICULTY_RAMP_PER_SECOND`, `MAX_ENEMIES`, or the in-game Difficulty option |
| Make contact damage hurt more | `DAMAGE_PER_SECOND` (note: invincibility frames limit how often it applies) |
| Boss timing / strength | `BOSS_FIRST_AT`, `BOSS_INTERVAL`, `BOSS_BASE_HEALTH`, `BOSS_HEALTH_PER_TIER` |
| Ability power | `*_DURATION`, `NUKE_DAMAGE`, `FREEZE_TIME_SCALE`, `FREEZE_SHATTER_MULT`, `MAX_ABILITY_STACK` |
| Event frequency | `WAVE_INTERVAL`, `EVENT_INTERVAL`, `BLOOD_MOON_DURATION` |
| Screen shake / particles | `SCREEN_SHAKE_*`, `PARTICLES_PER_DEATH` |
| Add an enemy | Add a constant to `Enemy.Type`, a painter in `SpriteFactory`, a behaviour in `Enemy.update`, and an unlock rule in `Enemy.randomType`. |
| Add a boss | Add a `Boss.Kind`, an attack list and its attack code, and a sprite in `SpriteFactory`. |
| Add an upgrade | Add a constant to `Upgrade` (title, max level, colour, icon letter, description) and read its level where it applies. |
| Add a field theme | Add a constant to `Settings.FieldTheme`. |

---

## Troubleshooting

| Problem | Likely cause / fix |
|---|---|
| Crash at launch about a missing file | An asset from the [required list](#required) is missing or misnamed. |
| Font looks blurry in menus | The font is generated at size 24 and scaled; raise `parameter.size` in `GdxGame.create()`. |
| Resolution / fullscreen options do nothing | Needs the LWJGL3 backend; other backends ignore runtime mode changes. |
| Enemies look too big / small | Sizes come from `slime.png`'s width ÷ 32. Resize that image or change `Enemy.baseSize`. |
| Too much on screen in the late game | Lower `MAX_ENEMIES`, or set *Ability Transparency* to **Max**. |
| Ability icons look wrong | Provide your own `*_icon.png`, or delete the file to use the generated icon. |
| Reset all saved settings & records | Delete the `survival-game-settings` preferences file (in your OS user data folder for libGDX). |

---

## Ideas for future work

- Gamepad support and remappable keys.
- Meta-progression (unlockable characters, permanent upgrades, gold).
- More bosses and a boss-rush mode.
- Real sprite sheets and animation frames in place of the procedural art.
- Sound effects per ability, enemy and boss attack; adaptive music for boss fights.
- Damage numbers and a post-run stats screen.
- Screen-reader-friendly menus and colour-blind-safe telegraph colours.
