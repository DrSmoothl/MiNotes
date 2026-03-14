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

package net.micode.notes.tool;

import android.content.Context;

import androidx.preference.PreferenceManager;

import net.micode.notes.R;
import net.micode.notes.ui.NotesPreferenceActivity;

public class ResourceParser {

    public static final int YELLOW           = 0;
    public static final int BLUE             = 1;
    public static final int WHITE            = 2;
    public static final int GREEN            = 3;
    public static final int RED              = 4;

    public static final int BG_DEFAULT_COLOR = YELLOW;

    public static final int TEXT_SMALL       = 0;
    public static final int TEXT_MEDIUM      = 1;
    public static final int TEXT_LARGE       = 2;
    public static final int TEXT_SUPER       = 3;

    public static final int BG_DEFAULT_FONT_SIZE = TEXT_MEDIUM;

    public static final int BACKGROUND_COLOR_COUNT = 5;

    public static class NoteColorResources {
        private static final int[] CARD_BACKGROUND_COLORS = new int[] {
            R.color.notes_card_yellow,
            R.color.notes_card_blue,
            R.color.notes_card_white,
            R.color.notes_card_green,
            R.color.notes_card_red
        };

        private static final int[] EDIT_BACKGROUND_COLORS = new int[] {
                R.color.notes_editor_yellow,
                R.color.notes_editor_blue,
                R.color.notes_editor_white,
                R.color.notes_editor_green,
                R.color.notes_editor_red
        };

        private static final int[] EDIT_HEADER_COLORS = new int[] {
                R.color.notes_editor_header_yellow,
                R.color.notes_editor_header_blue,
                R.color.notes_editor_header_white,
                R.color.notes_editor_header_green,
                R.color.notes_editor_header_red
        };

        public static int getNoteEditorBackgroundColor(int id) {
            return EDIT_BACKGROUND_COLORS[id];
        }

        public static int getNoteEditorHeaderColor(int id) {
            return EDIT_HEADER_COLORS[id];
        }

        public static int getNoteCardBackgroundColor(int id) {
            return CARD_BACKGROUND_COLORS[id];
        }
    }

    public static int getDefaultBgId(Context context) {
        if (PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
                NotesPreferenceActivity.PREFERENCE_SET_BG_COLOR_KEY, false)) {
            return (int) (Math.random() * BACKGROUND_COLOR_COUNT);
        } else {
            return BG_DEFAULT_COLOR;
        }
    }

    public static class WidgetBgResources {
        private final static int [] BG_2X_RESOURCES = new int [] {
            R.drawable.widget_2x_yellow,
            R.drawable.widget_2x_blue,
            R.drawable.widget_2x_white,
            R.drawable.widget_2x_green,
            R.drawable.widget_2x_red,
        };

        public static int getWidget2xBgResource(int id) {
            return BG_2X_RESOURCES[id];
        }

        private final static int [] BG_4X_RESOURCES = new int [] {
            R.drawable.widget_4x_yellow,
            R.drawable.widget_4x_blue,
            R.drawable.widget_4x_white,
            R.drawable.widget_4x_green,
            R.drawable.widget_4x_red
        };

        public static int getWidget4xBgResource(int id) {
            return BG_4X_RESOURCES[id];
        }
    }

    public static class TextAppearanceResources {
        private final static int [] TEXTAPPEARANCE_RESOURCES = new int [] {
            R.style.TextAppearanceNormal,
            R.style.TextAppearanceMedium,
            R.style.TextAppearanceLarge,
            R.style.TextAppearanceSuper
        };

        public static int getTexAppearanceResource(int id) {
            /**
             * HACKME: Fix bug of store the resource id in shared preference.
             * The id may larger than the length of resources, in this case,
             * return the {@link ResourceParser#BG_DEFAULT_FONT_SIZE}
             */
            if (id >= TEXTAPPEARANCE_RESOURCES.length) {
                return BG_DEFAULT_FONT_SIZE;
            }
            return TEXTAPPEARANCE_RESOURCES[id];
        }

        public static int getResourcesSize() {
            return TEXTAPPEARANCE_RESOURCES.length;
        }
    }
}
