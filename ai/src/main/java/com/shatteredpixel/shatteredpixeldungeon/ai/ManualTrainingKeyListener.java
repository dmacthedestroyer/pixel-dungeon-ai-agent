package com.shatteredpixel.shatteredpixeldungeon.ai;

import com.shatteredpixel.shatteredpixeldungeon.SPDAction;
import com.watabou.input.GameAction;
import com.watabou.input.KeyBindings;
import com.watabou.input.KeyEvent;
import com.watabou.utils.Signal;

import static com.shatteredpixel.shatteredpixeldungeon.SPDSettings.zoom;

public class ManualTrainingKeyListener implements Signal.Listener<KeyEvent> {
	GameAction curAction;
	@Override
	public boolean onSignal(KeyEvent event) {
		curAction = KeyBindings.getActionForKey(event);
		return false;
	}
}