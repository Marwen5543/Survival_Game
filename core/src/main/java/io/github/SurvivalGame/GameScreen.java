package io.github.SurvivalGame;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.audio.Music;
import com.badlogic.gdx.audio.Sound;
import com.badlogic.gdx.graphics.Camera;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.ExtendViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.utils.viewport.Viewport;

public class GameScreen extends ScreenAdapter {
    private static final boolean DRAW_DEBUG = false;

    // General
    private final Batch batch;
    private final ShapeRenderer shapeRenderer;
    private final Viewport gameViewport = new ExtendViewport(GameConfig.WORLD_WIDTH, GameConfig.WORLD_HEIGHT);
    private final Viewport uiViewport = new ScreenViewport();
    private final BitmapFont font;
    private final GlyphLayout layout = new GlyphLayout();
    private final Array<XpGem> xpGems = new Array<>();
    private final Array<HealthPickup> healthPickups = new Array<>();
    private final Array<AbilityPickup> abilityPickups = new Array<>();
    private final Array<Particle> particles = new Array<>();

    // Field dressing — no new art needed, just makes the arena feel designed instead of flat
    private final Array<GroundDecoration> groundDecorations = new Array<>();
    private final Array<Vector2> raindrops = new Array<>();
    private final Array<LightningBolt> lightningBolts = new Array<>();

    // Assets
    private final Texture bgdTexture = new Texture(Gdx.files.internal(GameConfig.BACKGROUND_TEXTURE));
    private final Texture playerTexture = new Texture(Gdx.files.internal(GameConfig.PLAYER_TEXTURE));
    private final Texture enemyTexture = new Texture(Gdx.files.internal(GameConfig.ENEMY_TEXTURE));
    private final Array<Texture> attackTextures = loadAttackTextures();
    private final Animation<Texture> attackAnimation = new Animation<>(1 / 12f, attackTextures);
    private final Music music = Gdx.audio.newMusic(Gdx.files.internal(GameConfig.MUSIC_FILE));
    private final Sound slashSfx = Gdx.audio.newSound(Gdx.files.internal(GameConfig.SLASH_SFX));

    // Player
    private final Player player = new Player(
        GameConfig.WORLD_WIDTH / 2f, GameConfig.WORLD_HEIGHT / 2f,
        gameViewport,
        playerTexture,
        attackAnimation,
        slashSfx
    );
    private final Vector2 inputMovement = new Vector2();

    // Enemies
    private final Array<Enemy> enemies = new Array<>();
    private float enemySpawnTimer;
    private float elapsedTime;

    // Ability pickups & storm effect
    private float abilitySpawnTimer;
    private float stormStrikeTimer;

    // Game State
    private int score;
    private boolean paused;
    private float shakeTimer;

    public GameScreen(GdxGame game) {
        this.batch = game.getBatch();
        this.shapeRenderer = game.getShapeRenderer();
        this.font = game.getFont();

        bgdTexture.setWrap(Texture.TextureWrap.Repeat, Texture.TextureWrap.Repeat);
    }

    private Array<Texture> loadAttackTextures() {
        Array<Texture> textures = new Array<>(14);
        for (int i = 0; i <= 13; i++) {
            textures.add(new Texture(Gdx.files.internal(String.format("slash_%02d.png", i))));
        }
        return textures;
    }

    @Override
    public void resize(int width, int height) {
        gameViewport.update(width, height, true);
        uiViewport.update(width, height, true);
    }

    @Override
    public void show() {
        resetGame();
    }

    private void resetGame() {
        player.reset(GameConfig.WORLD_WIDTH / 2f, GameConfig.WORLD_HEIGHT / 2f);

        enemies.clear();
        xpGems.clear();
        healthPickups.clear();
        abilityPickups.clear();
        particles.clear();
        lightningBolts.clear();

        enemySpawnTimer = 0f;
        abilitySpawnTimer = 0f;
        stormStrikeTimer = 0f;
        elapsedTime = 0f;

        score = 0;
        paused = false;
        shakeTimer = 0f;

        groundDecorations.clear();
        for (int i = 0; i < GameConfig.GROUND_DECORATION_COUNT; i++) {
            float x = MathUtils.random(0f, GameConfig.WORLD_WIDTH);
            float y = MathUtils.random(0f, GameConfig.WORLD_HEIGHT);
            groundDecorations.add(new GroundDecoration(x, y, GroundDecoration.randomType()));
        }

        raindrops.clear();
        for (int i = 0; i < GameConfig.RAIN_DROP_COUNT; i++) {
            raindrops.add(new Vector2(
                MathUtils.random(0f, GameConfig.WORLD_WIDTH),
                MathUtils.random(0f, GameConfig.WORLD_HEIGHT)
            ));
        }

        music.stop();
        music.setLooping(true);
        music.play();
    }

    @Override
    public void render(float deltaTime) {
        if (!player.isDead() && Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            paused = !paused;
        }

        if (!player.isDead() && !paused) {
            processInput();
            updateLogic(deltaTime);
            checkCollisions(deltaTime);
            updateEffects(deltaTime);
        } else if (player.isDead() && Gdx.input.isKeyJustPressed(Input.Keys.R)) {
            resetGame();
        }

        draw();
    }

    private void processInput() {
        inputMovement.setZero();
        if (Gdx.input.isKeyPressed(Input.Keys.W)) {
            inputMovement.y += 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.S)) {
            inputMovement.y -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.A)) {
            inputMovement.x -= 1;
        }
        if (Gdx.input.isKeyPressed(Input.Keys.D)) {
            inputMovement.x += 1;
        }

        inputMovement.nor();
        player.changeDirection(inputMovement);

        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) || Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_RIGHT)) {
            player.tryDash();
        }
    }

    private void updateLogic(float deltaTime) {
        elapsedTime += deltaTime;

        player.update(deltaTime);
        player.updateCombat(deltaTime, enemies);

        // Enemy spawning — ramps up the longer you survive.
        float spawnInterval = Math.max(
            GameConfig.ENEMY_SPAWN_INTERVAL_MIN,
            GameConfig.ENEMY_SPAWN_INTERVAL_START - elapsedTime * GameConfig.DIFFICULTY_RAMP_PER_SECOND
        );

        enemySpawnTimer += deltaTime;
        if (enemySpawnTimer >= spawnInterval) {
            enemySpawnTimer = 0f;
            Enemy.Type type = Enemy.randomType(score);
            enemies.add(Enemy.spawn(gameViewport, enemyTexture, player, type));
        }

        for (Enemy enemy : enemies) {
            enemy.update(deltaTime);
        }

        // Ability orb spawning — appear at a random spot on the field.
        abilitySpawnTimer += deltaTime;
        if (abilitySpawnTimer >= GameConfig.ABILITY_SPAWN_INTERVAL) {
            abilitySpawnTimer = 0f;
            float x = MathUtils.random(1f, GameConfig.WORLD_WIDTH - 1f);
            float y = MathUtils.random(1f, GameConfig.WORLD_HEIGHT - 1f);
            abilityPickups.add(new AbilityPickup(x, y, AbilityPickup.randomType()));
        }
        for (AbilityPickup pickup : abilityPickups) {
            pickup.update(deltaTime);
        }

        updateStorm(deltaTime);
    }

    /** While the Storm buff is active, periodically zaps every enemy near a random target with its own lightning bolt. */
    private void updateStorm(float deltaTime) {
        if (!player.isStormActive()) {
            stormStrikeTimer = 0f;
        } else {
            stormStrikeTimer -= deltaTime;

            if (stormStrikeTimer <= 0f && enemies.size > 0) {
                stormStrikeTimer = GameConfig.STORM_STRIKE_INTERVAL;

                Enemy target = enemies.get(MathUtils.random(enemies.size - 1));
                Vector2 strikeCenter = target.getCenter(new Vector2());

                var iterator = enemies.iterator();
                while (iterator.hasNext()) {
                    Enemy enemy = iterator.next();
                    Vector2 enemyCenter = enemy.getCenter(new Vector2());

                    if (enemyCenter.dst(strikeCenter) <= GameConfig.STORM_STRIKE_RADIUS) {
                        boolean died = enemy.takeDamage(GameConfig.STORM_STRIKE_DAMAGE);
                        enemy.applyStatusFlash(new Color(0.85f, 0.7f, 1f, 1f), 0.25f);
                        lightningBolts.add(new LightningBolt(enemyCenter.x, enemyCenter.y, GameConfig.WORLD_HEIGHT));
                        Particle.burst(particles, enemyCenter.x, enemyCenter.y, new Color(0.72f, 0.42f, 1f, 1f), GameConfig.PARTICLES_PER_DEATH);

                        if (died) {
                            xpGems.add(new XpGem(enemyCenter.x, enemyCenter.y, enemy.getXpValue()));
                            if (MathUtils.randomBoolean(GameConfig.HEALTH_PICKUP_CHANCE)) {
                                healthPickups.add(new HealthPickup(enemyCenter.x, enemyCenter.y));
                            }
                            iterator.remove();
                            score++;
                        }
                    }
                }
            }
        }

        if (player.isStormActive()) {
            for (Vector2 drop : raindrops) {
                drop.y -= GameConfig.RAIN_FALL_SPEED * deltaTime;
                if (drop.y < 0f) {
                    drop.y = GameConfig.WORLD_HEIGHT;
                    drop.x = MathUtils.random(0f, GameConfig.WORLD_WIDTH);
                }
            }
        }
    }

    private void updateEffects(float deltaTime) {
        if (shakeTimer > 0f) {
            shakeTimer -= deltaTime;
        }

        var particleIterator = particles.iterator();
        while (particleIterator.hasNext()) {
            Particle particle = particleIterator.next();
            particle.update(deltaTime);
            if (particle.isDone()) {
                particleIterator.remove();
            }
        }

        var boltIterator = lightningBolts.iterator();
        while (boltIterator.hasNext()) {
            LightningBolt bolt = boltIterator.next();
            bolt.update(deltaTime);
            if (bolt.isDone()) {
                boltIterator.remove();
            }
        }
    }

    private void checkCollisions(float deltaTime) {
        // Attacks vs enemies
        for (Attack attack : player.getAttacks()) {
            var enemyIterator = enemies.iterator();

            while (enemyIterator.hasNext()) {
                Enemy enemy = enemyIterator.next();

                if (attack.hasHit(enemy)) {
                    continue;
                }

                if (attack.overlaps(enemy)) {
                    attack.markHit(enemy);

                    boolean died = enemy.takeDamage(1f);
                    if (died) {
                        Vector2 enemyCenter = enemy.getCenter(new Vector2());

                        xpGems.add(new XpGem(enemyCenter.x, enemyCenter.y, enemy.getXpValue()));
                        Particle.burst(particles, enemyCenter.x, enemyCenter.y, Color.YELLOW, GameConfig.PARTICLES_PER_DEATH);

                        if (MathUtils.randomBoolean(GameConfig.HEALTH_PICKUP_CHANCE)) {
                            healthPickups.add(new HealthPickup(enemyCenter.x, enemyCenter.y));
                        }

                        enemyIterator.remove();
                        score++;
                    }
                }
            }
        }

        // XP gems vs player
        var xpIterator = xpGems.iterator();
        while (xpIterator.hasNext()) {
            XpGem gem = xpIterator.next();

            if (gem.collect(player, deltaTime, player.getPickupRadius())) {
                boolean leveledUp = player.addExperience(gem.getAmount());
                xpIterator.remove();

                if (leveledUp) {
                    System.out.println("LEVEL UP! Level = " + player.getLevel());
                }
            }
        }

        // Health pickups vs player
        var healthIterator = healthPickups.iterator();
        while (healthIterator.hasNext()) {
            HealthPickup pickup = healthIterator.next();
            if (pickup.overlaps(player)) {
                player.heal(GameConfig.HEALTH_PICKUP_AMOUNT);
                healthIterator.remove();
            }
        }

        // Ability pickups vs player
        var abilityIterator = abilityPickups.iterator();
        while (abilityIterator.hasNext()) {
            AbilityPickup pickup = abilityIterator.next();
            if (pickup.overlaps(player)) {
                player.applyAbility(pickup.getType());
                Vector2 center = pickup.getCenter(new Vector2());
                Particle.burst(particles, center.x, center.y, pickup.getType().color, GameConfig.PARTICLES_PER_DEATH);
                abilityIterator.remove();
            }
        }

        // Enemies vs player
        int numHits = 0;
        for (Enemy enemy : enemies) {
            if (player.overlaps(enemy)) {
                ++numHits;
            }
        }

        if (numHits > 0) {
            float lifeBefore = player.getLife();
            player.subLife(GameConfig.DAMAGE_PER_SECOND * numHits * deltaTime);
            if (player.getLife() < lifeBefore) {
                shakeTimer = GameConfig.SCREEN_SHAKE_DURATION;
            }
        }
    }

    private void draw() {
        ScreenUtils.clear(Color.BLACK);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);

        gameViewport.apply();

        Camera camera = gameViewport.getCamera();
        Vector3 originalCameraPos = new Vector3(camera.position);
        if (shakeTimer > 0f) {
            float magnitude = GameConfig.SCREEN_SHAKE_MAGNITUDE * (shakeTimer / GameConfig.SCREEN_SHAKE_DURATION);
            camera.position.add(MathUtils.random(-magnitude, magnitude), MathUtils.random(-magnitude, magnitude), 0f);
            camera.update();
        }

        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        drawBackground();
        batch.end();

        // Ground dressing — grass, rocks, cracks and pulsing runes, drawn beneath everything
        // else so the field reads as a designed place instead of flat ground.
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (GroundDecoration decoration : groundDecorations) {
            decoration.draw(shapeRenderer, elapsedTime);
        }
        shapeRenderer.end();

        batch.begin();
        for (Enemy enemy : enemies) {
            enemy.draw(batch);
        }
        for (Attack attack : player.getAttacks()) {
            attack.draw(batch);
        }
        player.draw(batch);
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (XpGem gem : xpGems) {
            gem.drawDebug(shapeRenderer);
        }
        for (HealthPickup pickup : healthPickups) {
            pickup.drawDebug(shapeRenderer);
        }
        for (AbilityPickup pickup : abilityPickups) {
            pickup.drawDebug(shapeRenderer);
        }
        for (Particle particle : particles) {
            particle.draw(shapeRenderer);
        }
        if (player.isShielded()) {
            Vector2 center = player.getCenter(new Vector2());
            shapeRenderer.setColor(0.4f, 0.7f, 1f, 0.35f);
            shapeRenderer.circle(center.x, center.y, 0.6f, 20);
        }
        shapeRenderer.end();

        // Storm weather — rain while the buff is active, plus a lightning bolt landing on
        // every enemy actually struck (added in updateStorm()).
        if (player.isStormActive()) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
            shapeRenderer.setColor(0.75f, 0.85f, 1f, 0.55f);
            for (Vector2 drop : raindrops) {
                shapeRenderer.line(drop.x, drop.y, drop.x - 0.06f, drop.y - 0.22f);
            }
            shapeRenderer.end();
        }

        if (!lightningBolts.isEmpty()) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (LightningBolt bolt : lightningBolts) {
                bolt.draw(shapeRenderer);
            }
            shapeRenderer.end();
        }

        if (shakeTimer > 0f) {
            camera.position.set(originalCameraPos);
            camera.update();
        }

        drawDebug();
        drawUi();
    }

    private void drawUi() {
        uiViewport.apply();

        if (player.isStormActive()) {
            shapeRenderer.setProjectionMatrix(uiViewport.getCamera().combined);
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            shapeRenderer.setColor(0.1f, 0.05f, 0.2f, 0.25f);
            shapeRenderer.rect(0, 0, uiViewport.getWorldWidth(), uiViewport.getWorldHeight());
            shapeRenderer.end();
        }

        float barX = 20;
        float barWidth = 200;

        shapeRenderer.setProjectionMatrix(uiViewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        float healthY = uiViewport.getWorldHeight() - 40;
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, healthY, barWidth, 16);
        shapeRenderer.setColor(Color.RED);
        float healthPerc = Math.max(0f, player.getLife() / player.getMaxLife());
        shapeRenderer.rect(barX, healthY, barWidth * healthPerc, 16);

        float xpY = healthY - 20;
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, xpY, barWidth, 10);
        shapeRenderer.setColor(Color.CYAN);
        float xpPerc = (float) player.getExperience() / player.getExperienceToNextLevel();
        shapeRenderer.rect(barX, xpY, barWidth * xpPerc, 10);

        float dashY = xpY - 16;
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(barX, dashY, 60, 6);
        shapeRenderer.setColor(player.getDashCooldownPercent() >= 1f ? Color.LIME : Color.GRAY);
        shapeRenderer.rect(barX, dashY, 60 * player.getDashCooldownPercent(), 6);

        float buffY = dashY - 14;
        if (player.isShielded()) {
            drawBuffBar(barX, buffY, 60, 6, new Color(0.35f, 0.65f, 1f, 1f), player.getShieldPercent());
            buffY -= 12;
        }
        if (player.isHasted()) {
            drawBuffBar(barX, buffY, 60, 6, new Color(0.4f, 1f, 0.45f, 1f), player.getHastePercent());
            buffY -= 12;
        }
        if (player.isMagnetActive()) {
            drawBuffBar(barX, buffY, 60, 6, new Color(1f, 0.65f, 0.2f, 1f), player.getMagnetPercent());
            buffY -= 12;
        }
        if (player.isStormActive()) {
            drawBuffBar(barX, buffY, 60, 6, new Color(0.72f, 0.42f, 1f, 1f), player.getStormPercent());
        }

        shapeRenderer.end();

        batch.setProjectionMatrix(uiViewport.getCamera().combined);
        batch.begin();
        font.draw(batch, "Score: " + score, barX, uiViewport.getWorldHeight() - 60);
        font.draw(batch, "Level: " + player.getLevel(), barX, uiViewport.getWorldHeight() - 96);

        StringBuilder activeBuffs = new StringBuilder();
        if (player.isShielded()) activeBuffs.append("Shield ");
        if (player.isHasted()) activeBuffs.append("Haste ");
        if (player.isMagnetActive()) activeBuffs.append("Magnet ");
        if (player.isStormActive()) activeBuffs.append("Storm ");
        if (activeBuffs.length() > 0) {
            font.draw(batch, activeBuffs.toString().trim(), barX, uiViewport.getWorldHeight() - 130);
        }

        if (player.isDead()) {
            layout.setText(font, "GAME OVER");
            font.draw(batch, layout, uiViewport.getWorldWidth() / 2 - layout.width / 2, uiViewport.getWorldHeight() / 2 + 40);
            layout.setText(font, "Score: " + score);
            font.draw(batch, layout, uiViewport.getWorldWidth() / 2 - layout.width / 2, uiViewport.getWorldHeight() / 2);
            layout.setText(font, "Press R to Restart");
            font.draw(batch, layout, uiViewport.getWorldWidth() / 2 - layout.width / 2, uiViewport.getWorldHeight() / 2 - 30);
        } else if (paused) {
            layout.setText(font, "PAUSED");
            font.draw(batch, layout, uiViewport.getWorldWidth() / 2 - layout.width / 2, uiViewport.getWorldHeight() / 2);
            layout.setText(font, "Press ESC to Resume");
            font.draw(batch, layout, uiViewport.getWorldWidth() / 2 - layout.width / 2, uiViewport.getWorldHeight() / 2 - 30);
        }
        batch.end();
    }

    private void drawBuffBar(float x, float y, float width, float height, Color color, float percent) {
        shapeRenderer.setColor(Color.DARK_GRAY);
        shapeRenderer.rect(x, y, width, height);
        shapeRenderer.setColor(color);
        shapeRenderer.rect(x, y, width * Math.max(0f, Math.min(1f, percent)), height);
    }

    private void drawBackground() {
        float u2 = gameViewport.getWorldWidth() / GameConfig.WORLD_WIDTH;
        float v2 = gameViewport.getWorldHeight() / GameConfig.WORLD_HEIGHT;

        batch.draw(bgdTexture,
            0, 0,
            gameViewport.getWorldWidth(),
            gameViewport.getWorldHeight(),
            0, 0,
            u2, v2
        );
    }

    private void drawDebug() {
        if (!DRAW_DEBUG) {
            return;
        }

        shapeRenderer.setProjectionMatrix(gameViewport.getCamera().combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);

        player.drawDebug(shapeRenderer, Color.GREEN);

        for (Enemy enemy : enemies) {
            enemy.drawDebug(shapeRenderer, Color.RED);
        }

        for (Attack attack : player.getAttacks()) {
            attack.drawDebug(shapeRenderer, Color.YELLOW);
        }
        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        bgdTexture.dispose();
        playerTexture.dispose();
        enemyTexture.dispose();
        attackTextures.forEach(Texture::dispose);
        music.dispose();
        slashSfx.dispose();
    }
}
