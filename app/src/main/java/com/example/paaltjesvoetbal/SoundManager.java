package com.example.paaltjesvoetbal;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

public class SoundManager {
    private static SoundManager instance;
    private SoundPool soundPool;
    private final int shootSound;
    private final int goalSound;

    private SoundManager(Context context) {
        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();

        soundPool = new SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build();

        // Load sounds
        shootSound = soundPool.load(context, R.raw.hit_ball, 1);
        goalSound = soundPool.load(context, R.raw.goal, 1);
    }

    // Get the singleton instance
    public static SoundManager getInstance(Context context) {
        if (instance == null) {
            instance = new SoundManager(context);
        }
        return instance;
    }

    public void playShootSound() {
        soundPool.play(shootSound, 1, 1, 1, 0, 1);
    }

    public void playGoalSound() {
        soundPool.play(goalSound, 1, 1, 1, 0, 1);
    }

    public void release() {
        soundPool.release();
        soundPool = null;
        instance = null;
    }
}