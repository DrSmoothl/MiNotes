package net.micode.notes.ui;

import android.app.Activity;
import android.view.View;
import android.view.ViewGroup;

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

    static void applyBottomInsetToPadding(Activity activity, View view) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        final int paddingLeft = view.getPaddingLeft();
        final int paddingTop = view.getPaddingTop();
        final int paddingRight = view.getPaddingRight();
        final int paddingBottom = view.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(view, (target, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            target.setPadding(paddingLeft, paddingTop, paddingRight,
                    paddingBottom + navigationBars.bottom);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }

    static void applyBottomInsetToMargin(Activity activity, View view) {
        WindowCompat.setDecorFitsSystemWindows(activity.getWindow(), false);

        ViewGroup.LayoutParams rawLayoutParams = view.getLayoutParams();
        if (!(rawLayoutParams instanceof ViewGroup.MarginLayoutParams)) {
            return;
        }
        ViewGroup.MarginLayoutParams layoutParams = (ViewGroup.MarginLayoutParams) rawLayoutParams;
        final int leftMargin = layoutParams.leftMargin;
        final int topMargin = layoutParams.topMargin;
        final int rightMargin = layoutParams.rightMargin;
        final int bottomMargin = layoutParams.bottomMargin;

        ViewCompat.setOnApplyWindowInsetsListener(view, (target, windowInsets) -> {
            Insets navigationBars = windowInsets.getInsets(WindowInsetsCompat.Type.navigationBars());
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) target.getLayoutParams();
            params.setMargins(leftMargin, topMargin, rightMargin, bottomMargin + navigationBars.bottom);
            target.setLayoutParams(params);
            return windowInsets;
        });
        ViewCompat.requestApplyInsets(view);
    }
}