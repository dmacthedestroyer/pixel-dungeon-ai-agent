package com.shatteredpixel.shatteredpixeldungeon.ai;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.utils.IntMap;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.actors.mobs.Mob;
import com.shatteredpixel.shatteredpixeldungeon.levels.Terrain;
import com.shatteredpixel.shatteredpixeldungeon.levels.features.HighGrass;
import com.shatteredpixel.shatteredpixeldungeon.levels.traps.Trap;
import com.shatteredpixel.shatteredpixeldungeon.plants.Plant;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Random;

import java.awt.*;
import java.util.ArrayList;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.shatteredpixel.shatteredpixeldungeon.scenes.PixelScene.uiCamera;
import static com.watabou.noosa.Camera.main;

import com.badlogic.gdx.graphics.Color;

/**
 * Inject programmable hero actions into the standard ShatteredPixelDungeon Game class. Future updates will make the
 * Hero agent parameterized so we can plug in different agents.
 */
public class AIPixelDungeon extends ShatteredPixelDungeon {

    ShapeRenderer shape;
    TextureRegion region;

    public AIPixelDungeon(PlatformSupport platform) {
        super(platform);
    }

    @Override
    public void create() {
        super.create();
        startGame();

        shape = new ShapeRenderer();
    }

    // Send to the hero select scene when Game is ready to switch scenes on startup
    public void startGame(){
        // Logic pulled from code called in onclick event for HeroSelectScene start button
        GamesInProgress.selectedClass = HeroClass.WARRIOR;
        sceneClass = HeroSelectScene.class;

//        Dungeon.hero = null;
//        Dungeon.daily = Dungeon.dailyReplay = false;
////        Dungeon.initSeed();
//        ActionIndicator.clearAction();
//
//        InterlevelScene.mode = InterlevelScene.Mode.DESCEND;
//        sceneClass = InterlevelScene.class;
    }

    // Once hero dies, simulate a left mouse click at the location where the "restart game" button would be
    public void restartGame() {
        mouseClick(
                (int) main.screenWidth() / 2,
                (int) (main.screenHeight() / 1.7),
                Input.Buttons.LEFT
        );
    }

    // Once HeroSelectScene is entered, simulate a left mouse click where the "start" button would be
    public void handleHeroSelectScene(){
        mouseClick(
                (int) main.screenWidth() / 8,
                (int) (main.screenHeight() / 1.1),
                Input.Buttons.LEFT
        );
    }

    @Override
    public void update() {
        // Check if we just died
        if (Dungeon.hero != null && !Dungeon.hero.isAlive()) {
            restartGame();
        }
        // Check if we entered the hero select scene
        if (scene.getClass().equals(HeroSelectScene.class)) {
            handleHeroSelectScene();
        }

        super.update();
//        if (Dungeon.hero != null && Dungeon.hero.isAlive() && Dungeon.hero.ready && scene.active && scene.alive && scene.getClass().equals(GameScene.class) && Dungeon.hero.curAction == null && Dungeon.level != null) {
//            HeroAction action = act();
//            if (action != null) {
//                Dungeon.hero.curAction = action;
//                // stole this from CellSelector.moveFromActions function
//                // I don't understand yet how calling next() progresses the game logic, but it seems to do the trick ¯\_(ツ)_/¯
//                Dungeon.hero.next();
//            }
//        }
        if (Dungeon.level != null) {
            String[] levelContext = createLevelContext();
            drawRectangles(levelContext);
        }
    }

    /**
     * Modify this code to make the AI hero do whatever you want. If it returns any subclass of HeroAction, it'll
     * perform that action. Otherwise does nothing. I don't know how robust this is, so give it a try and report any
     * errors found
     */
    protected HeroAction act() {
        ArrayList<HeroAction> actions = listActions(Dungeon.hero.pos);
        return actions.get(Random.Int(actions.size()));
     }

    public ArrayList<HeroAction> listActions(int heroPos) {
        ArrayList<HeroAction> actions = new ArrayList<>();
        for (int delta : PathFinder.NEIGHBOURS8){
            int cell = heroPos + delta;
            actions.add(new HeroAction.Move(cell));
        }

        return actions;
    }

    // Overrides input listening to simulate a mouse click. Taken from InputHandler class
    // Give an x, y, and button id (from Input.Buttons for mouse buttons)
    // pointer id is assumed 0 for mouse id (works for now?)
    public void mouseClick(int x, int y, int button) {
        // Must click down and then click up in sequence for a valid mouse input
        inputHandler.touchDown(x, y, 0, button);
        inputHandler.touchUp(x, y, 0, button);
    }

    public String[] createLevelContext() {
        int length = Dungeon.level.map.length;
        String[] mobs = new String[length];
        for (Mob mob : Dungeon.level.mobs) {
            int pos = mob.pos;
            mobs[pos] = String.format("mob.%s.%s", mob.alignment, mob.name());
        }

        String[] traps = new String[length];
        for (IntMap.Entry<Trap> trap : Dungeon.level.traps) {
            int pos = trap.value.pos;
            traps[pos] = String.format("trap.%s", trap.value.name());
        }

        String[] plants = new String[length];
        for (IntMap.Entry<Plant> plant : Dungeon.level.plants) {
            int pos = plant.value.pos;
            plants[pos] = String.format("plant.%s", plant.value.name());
        }

        String[] levelContext = new String[length];
        for (int i = 0; i < length; i++) {
            // Handle Terrain objects not in an explicit list in Level, taken from Level class
            String isDoor = "";
            switch(Dungeon.level.map[i]) {
                case Terrain.HIGH_GRASS:
                case Terrain.FURROWED_GRASS:
                    plants[i] = "plant.grass";
                    break;

                case Terrain.DOOR:
                    isDoor = "door.normal";
                    break;

                case Terrain.CRYSTAL_DOOR:
                    isDoor = "door.crystal";
                    break;
            }

            if (Dungeon.level.visited[i]) { levelContext[i] = "visited"; }
            if (Dungeon.level.mapped[i]) { levelContext[i] = "mapped"; }
            if (Dungeon.level.visited[i] && Dungeon.level.passable[i]) { levelContext[i] = "passable"; }
            if (Dungeon.level.heroFOV[i]) { levelContext[i] = "fov"; }
            if (Dungeon.level.visited[i] && Dungeon.level.pit[i]) { levelContext[i] = "pit"; }
            if (Dungeon.level.visited[i] && plants[i] != null) { levelContext[i] = plants[i]; }
            if (Dungeon.level.visited[i] && traps[i] != null) { levelContext[i] = traps[i]; }
            if (Dungeon.level.visited[i] && !isDoor.isEmpty()) {levelContext[i] = isDoor; }
            if (Dungeon.level.heroFOV[i] && mobs[i] != null) { levelContext[i] = mobs[i]; }
        }

        if (Dungeon.hero != null) { levelContext[Dungeon.hero.pos] = "hero"; }

        return levelContext;
    }

    public void drawRectangles(String[] rectangles) {
        shape.begin(ShapeRenderer.ShapeType.Line);
        for (int i = 0; i < rectangles.length; i++) {
            if (Objects.equals(rectangles[i], "passable")) {
                draw(i,Color.BLUE);
//            } else if (Objects.equals(rectangles[i], "mapped")) {
//                draw(i, Color.RED);
            } else if (Objects.equals(rectangles[i], "visited")) {
                draw(i, Color.GREEN);
            } else if (Objects.equals(rectangles[i], "fov")) {
                draw(i, Color.CYAN);
            } else if (Objects.equals(rectangles[i], "pit")) {
                draw(i, Color.BROWN);
            } else if (rectangles[i] != null && Pattern.compile("plant").matcher(rectangles[i]).find()) {
                draw(i, Color.FOREST);
            } else if (rectangles[i] != null && Pattern.compile("trap").matcher(rectangles[i]).find()) {
                draw(i, Color.ORANGE);
            } else if (rectangles[i] != null && Pattern.compile("door").matcher(rectangles[i]).find()) {
                draw(i, Color.PURPLE);
            }else if (rectangles[i] != null && Pattern.compile("mob.ENEMY").matcher(rectangles[i]).find()) {
                draw(i, Color.RED);
            } else if (rectangles[i] != null && Pattern.compile("hero").matcher(rectangles[i]).find()) {
                draw(i, Color.WHITE);
            }
        }
        shape.end();
    }

    public void draw(int pos, Color color) {
        int size = 5;
        shape.setProjectionMatrix(new Matrix4(uiCamera.matrix));
        shape.setColor(color);
        float[] coords = getCoords(pos);
        shape.rect(coords[0], coords[1], size, size);
    }

    public float[] getCoords(int index) {
        return new float[] {
                (float) (index % Dungeon.level.width()) / Dungeon.level.map.length * main.screenWidth() * 3,
                (float) (index / Dungeon.level.width()) / Dungeon.level.map.length * main.screenHeight() * 3
        };
    }
}
