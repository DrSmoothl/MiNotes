/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.notes.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.materialswitch.MaterialSwitch;

import net.micode.notes.R;


public class NotesPreferenceActivity extends AppCompatActivity {
    public static final String PREFERENCE_SET_BG_COLOR_KEY = "pref_key_bg_random_appear";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        MaterialToolbar toolbar = findViewById(R.id.preferences_toolbar);
        toolbar.setNavigationOnClickListener(view -> finish());

        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        MaterialSwitch randomBackgroundSwitch = findViewById(
                R.id.preferences_random_background_switch);
        randomBackgroundSwitch.setChecked(
                preferences.getBoolean(PREFERENCE_SET_BG_COLOR_KEY, false));
        randomBackgroundSwitch.setOnCheckedChangeListener(
                new CompoundButton.OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        preferences.edit().putBoolean(PREFERENCE_SET_BG_COLOR_KEY, isChecked)
                                .apply();
                    }
                });
    }
}
