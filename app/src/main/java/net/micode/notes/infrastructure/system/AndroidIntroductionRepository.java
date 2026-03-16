package net.micode.notes.infrastructure.system;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.preference.PreferenceManager;

import net.micode.notes.R;
import net.micode.notes.domain.repository.IntroductionRepository;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public final class AndroidIntroductionRepository implements IntroductionRepository {
    private static final String PREFERENCE_ADD_INTRODUCTION = "net.micode.notes.introduction";

    private final Context context;

    public AndroidIntroductionRepository(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public boolean isIntroductionCreated() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getBoolean(PREFERENCE_ADD_INTRODUCTION, false);
    }

    @Override
    public String loadIntroductionText() {
        StringBuilder builder = new StringBuilder();
        InputStream inputStream = null;
        try {
            inputStream = context.getResources().openRawResource(R.raw.introduction);
            InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
            BufferedReader reader = new BufferedReader(inputStreamReader);
            char[] buffer = new char[1024];
            int readCount;
            while ((readCount = reader.read(buffer)) > 0) {
                builder.append(buffer, 0, readCount);
            }
        } catch (IOException e) {
            return "";
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException ignored) {
                }
            }
        }
        return builder.toString();
    }

    @Override
    public void markIntroductionCreated() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putBoolean(PREFERENCE_ADD_INTRODUCTION, true).apply();
    }
}