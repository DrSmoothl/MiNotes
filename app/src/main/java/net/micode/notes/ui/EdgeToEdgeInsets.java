package net.micode.notes.ui;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;

final class EdgeToEdgeInsets {
    private EdgeToEdgeInsets() {
    }

    static void applyTopInset(Activity activity, View view) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        final int paddingLeft = view.getPaddingLeft();
        final int paddingTop = view.getPaddingTop();
        final int paddingRight = view.getPaddingRight();
        final int paddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (target, windowInsets) -> {
            Insets statusBars = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars());
            target.setPadding(paddingLeft, paddingTop + statusBars.top, paddingRight, paddingBottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}