package com.shatteredpixel.shatteredpixeldungeon.ai;

import com.badlogic.gdx.Input;
import com.shatteredpixel.shatteredpixeldungeon.Dungeon;
import com.shatteredpixel.shatteredpixeldungeon.GamesInProgress;
import com.shatteredpixel.shatteredpixeldungeon.ShatteredPixelDungeon;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroAction;
import com.shatteredpixel.shatteredpixeldungeon.actors.hero.HeroClass;
import com.shatteredpixel.shatteredpixeldungeon.scenes.GameScene;
import com.shatteredpixel.shatteredpixeldungeon.scenes.HeroSelectScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.PlatformSupport;
import com.watabou.utils.Random;

import java.util.ArrayList;

import static com.watabou.noosa.Camera.main;

/**
 * Inject programmable hero actions into the standard ShatteredPixelDungeon Game class. Future updates will make the
 * Hero agent parameterized so we can plug in different agents.
 */
public class AIPixelDungeon extends ShatteredPixelDungeon {
    public AIPixelDungeon(PlatformSupport platform) {
        super(platform);
    }

    private Bundle previousState;
    private Integer previousAction;

    private RecordGameState recorder;

    @Override
    public void create() {
        super.create();
        int randNum = actionGenerator.nextInt();
        try {
            buffy = new BufferedWriter(new FileWriter("C:\\Users\\canne\\school_stuff\\5S2025\\pixel-dungeon-ai-agent\\test_data\\" + randNum + ".jsonl"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        recorder = new RecordGameState();
        recorder.initiateWrite();
        startGame();
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
        if (Dungeon.hero != null && Dungeon.hero.isAlive() && Dungeon.hero.ready && scene.active && scene.alive && scene.getClass().equals(GameScene.class) && Dungeon.hero.curAction == null && Dungeon.level != null) {

            System.out.println("Attempt to print bundle:");
//            System.out.println(Dungeon.bundleAll().toString());

            //save bundle as jsonl file
            //each line should be {state, action, next_state}
            Bundle nextState = Dungeon.bundleAll();

            if (previousState != null && previousAction != null) {
                recorder.saveGameData(recorder.bundleGameData(previousState, previousAction, nextState));
            }


            int action = actionGenerator.nextInt(8);
            if (Dungeon.hero.handle(Dungeon.hero.pos + PathFinder.NEIGHBOURS8[action])) {
                Dungeon.hero.next();
                previousAction = action;
                previousState = nextState;
            }
            else {
                System.out.println("hello darkness my old friend...");
            }

//            HeroAction action = act();
//            if (action != null) {
//                Dungeon.hero.curAction = action;
//                // stole this from CellSelector.moveFromActions function
//                // I don't understand yet how calling next() progresses the game logic, but it seems to do the trick ¯\_(ツ)_/¯
//                Dungeon.hero.next();
//            }
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

}
