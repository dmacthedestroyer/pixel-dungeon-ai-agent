package com.shatteredpixel.shatteredpixeldungeon.ai;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import com.watabou.utils.Bundle;

public class RecordGameState {

    private java.util.Random actionGenerator = new java.util.Random();

    private BufferedWriter buffy;


    public void initiateWrite() {
        int randNum = actionGenerator.nextInt();
        System.out.println("file number: " + randNum);
        try {
            buffy = new BufferedWriter(new FileWriter("C:\\Users\\canne\\school_stuff\\5S2025\\pixel-dungeon-ai-agent\\test_data\\" + randNum + ".jsonl"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Bundle bundleGameData(Bundle previousState, Integer previousAction, Bundle nextState) {
        Bundle bundleOfBundles = new Bundle();
        bundleOfBundles.put("state", previousState);
        bundleOfBundles.put("action", previousAction);
        bundleOfBundles.put("next_state", nextState);

//        System.out.println(bundleOfBundles.toString());
        return bundleOfBundles;
    }

    public void saveGameData(Bundle gameData) {
        try {
            //maybe flushing everytime isn't the best idea...
            buffy.write(gameData.toString());
            buffy.newLine();
            buffy.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
