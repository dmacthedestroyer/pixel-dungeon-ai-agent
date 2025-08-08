package com.shatteredpixel.shatteredpixeldungeon.ai;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.watabou.input.InputHandler;

public class NumpadInputHandler extends InputHandler {
    private int pressedKey = Input.Keys.NUMPAD_5;

    public NumpadInputHandler() {
        super(Gdx.input);  // or the proper constructor
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isNumpadKey(keycode)) {
            pressedKey = keycode;
            return true;
        }
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        if (isNumpadKey(keycode) && keycode == pressedKey) {
            pressedKey = Input.Keys.NUMPAD_5;
            return true;
        }
        return false;
    }

    private int mapNumpadKeyToActionIndex(int keycode) {
        switch (keycode) {
            case Input.Keys.NUMPAD_7: return 0; // up-left
            case Input.Keys.NUMPAD_8: return 1; // up
            case Input.Keys.NUMPAD_9: return 2; // up-right
            case Input.Keys.NUMPAD_4: return 3; // left
            case Input.Keys.NUMPAD_6: return 4; // right
            case Input.Keys.NUMPAD_1: return 5; // down-left
            case Input.Keys.NUMPAD_2: return 6; // down
            case Input.Keys.NUMPAD_3: return 7; // down-right
            default: return -1; // invalid or no mapping
        }
    }

    public int getPressedKey() {
        return mapNumpadKeyToActionIndex(pressedKey);
    }

    private boolean isNumpadKey(int keycode) {
        return keycode >= Input.Keys.NUMPAD_1 && keycode <= Input.Keys.NUMPAD_9;
    }
}
