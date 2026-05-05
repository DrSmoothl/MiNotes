package net.micode.notes.ui;

import android.app.SearchManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.format.DateUtils;
import android.text.style.BackgroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.textfield.TextInputEditText;

import net.micode.notes.R;
import net.micode.notes.data.Notes;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearchNotesActivity extends AppCompatActivity {
    private static final String PREF_RECENT_SEARCHES = "pref_recent_searches";
    private static final String HISTORY_SEPARATOR = "\\n";
    private static final int MAX_HISTORY_SIZE = 8;

    private MaterialToolbar mToolbar;
    private TextInputEditText mSearchInput;
    private TextView mResultsSummary;
    private RecyclerView mResultsList;
    private View mEmptyState;
    private TextView mEmptyTitle;
    private TextView mEmptyMessage;
    private View mHistorySection;
    private TextView mClearHistoryButton;
    private ChipGroup mRecentSearchesGroup;
    private SearchResultsAdapter mAdapter;
    private SharedPreferences mPreferences;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mSearchExecutor = Executors.newSingleThreadExecutor();

    private Runnable mPendingSearchRunnable;
    private int mSearchRequestVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_notes);

        initViews();
        bindInitialQuery();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPendingSearchRunnable != null) {
            mMainHandler.removeCallbacks(mPendingSearchRunnable);
        }
        mSearchExecutor.shutdownNow();
    }

    private void initViews() {
        mToolbar = findViewById(R.id.search_toolbar);
        mSearchInput = findViewById(R.id.search_input);
        mResultsSummary = findViewById(R.id.search_results_summary);
        mResultsList = findViewById(R.id.search_results_list);
        mEmptyState = findViewById(R.id.search_empty_state);
        mEmptyTitle = findViewById(R.id.search_empty_title);
        mEmptyMessage = findViewById(R.id.search_empty_message);
        mHistorySection = findViewById(R.id.search_history_section);
        mClearHistoryButton = findViewById(R.id.search_clear_history);
        mRecentSearchesGroup = findViewById(R.id.search_history_group);
        mPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        EdgeToEdgeInsets.applyTopInset(this, mToolbar);
        EdgeToEdgeInsets.applyBottomInsetToPadding(this, mResultsList);
        EdgeToEdgeInsets.applyBottomInsetToPadding(this, mEmptyState);

        mToolbar.setNavigationOnClickListener(view -> finish());
        mClearHistoryButton.setOnClickListener(view -> clearRecentSearches());

        mAdapter = new SearchResultsAdapter(new SearchResultClickListener() {
            @Override
            public void onClick(SearchResultItem item) {
                openResult(item);
            }
        });
        mResultsList.setLayoutManager(new LinearLayoutManager(this));
        mResultsList.setAdapter(mAdapter);

        mSearchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            @Override
            public void afterTextChanged(Editable editable) {
                scheduleSearch(editable == null ? "" : editable.toString());
            }
        });
    }

    private void bindInitialQuery() {
        CharSequence initialQuery = getIntent().getStringExtra(SearchManager.QUERY);
        if (TextUtils.isEmpty(initialQuery)) {
            renderRecentSearches();
            renderIdleState();
            mSearchInput.requestFocus();
            InputMethodManager inputMethodManager = getSystemService(InputMethodManager.class);
            if (inputMethodManager != null) {
                inputMethodManager.showSoftInput(mSearchInput, InputMethodManager.SHOW_IMPLICIT);
            }
            return;
        }

        mSearchInput.setText(initialQuery);
        mSearchInput.setSelection(initialQuery.length());
    }

    private void scheduleSearch(String rawQuery) {
        if (mPendingSearchRunnable != null) {
            mMainHandler.removeCallbacks(mPendingSearchRunnable);
        }

        final String query = rawQuery == null ? "" : rawQuery.trim();
        mPendingSearchRunnable = new Runnable() {
            @Override
            public void run() {
                performSearch(query);
            }
        };
        mMainHandler.postDelayed(mPendingSearchRunnable, 180L);
    }

    private void performSearch(String query) {
        if (TextUtils.isEmpty(query)) {
            mSearchRequestVersion++;
            mAdapter.submit(query, new ArrayList<SearchResultItem>());
            renderRecentSearches();
            renderIdleState();
            return;
        }

        final int requestVersion = ++mSearchRequestVersion;
        mSearchExecutor.execute(new Runnable() {
            @Override
            public void run() {
                List<SearchResultItem> results = queryResults(query);
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (isFinishing() || requestVersion != mSearchRequestVersion) {
                            return;
                        }
                        renderSearchResults(query, results);
                    }
                });
            }
        });
    }

    private List<SearchResultItem> queryResults(String query) {
        ArrayList<SearchResultItem> results = new ArrayList<SearchResultItem>();
        Cursor cursor = getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                new String[] {
                        Notes.NoteColumns.ID,
                        Notes.NoteColumns.SNIPPET,
                        Notes.NoteColumns.MODIFIED_DATE
                },
                Notes.NoteColumns.SNIPPET + " LIKE ? AND "
                        + Notes.NoteColumns.PARENT_ID + " <> ? AND "
                        + Notes.NoteColumns.TYPE + " = ?",
                new String[] {
                        "%" + query + "%",
                        String.valueOf(Notes.ID_TRASH_FOLER),
                        String.valueOf(Notes.TYPE_NOTE)
                },
                Notes.NoteColumns.MODIFIED_DATE + " DESC");

        if (cursor == null) {
            return results;
        }

        try {
            int idIndex = cursor.getColumnIndexOrThrow(Notes.NoteColumns.ID);
            int snippetIndex = cursor.getColumnIndexOrThrow(Notes.NoteColumns.SNIPPET);
            int modifiedIndex = cursor.getColumnIndexOrThrow(Notes.NoteColumns.MODIFIED_DATE);
            while (cursor.moveToNext()) {
                results.add(new SearchResultItem(
                        cursor.getLong(idIndex),
                        formatSnippet(cursor.getString(snippetIndex)),
                        cursor.getLong(modifiedIndex)));
            }
        } finally {
            cursor.close();
        }
        return results;
    }

    private void renderIdleState() {
        mResultsSummary.setText(R.string.notes_search_results_hint);
        mResultsList.setVisibility(View.GONE);
        mEmptyState.setVisibility(View.VISIBLE);
        mEmptyTitle.setText(R.string.notes_search_idle_title);
        mEmptyMessage.setText(R.string.notes_search_idle_message);
    }

    private void renderSearchResults(String query, List<SearchResultItem> results) {
        renderRecentSearches();
        mAdapter.submit(query, results);
        mResultsSummary.setText(getResources().getQuantityString(
                R.plurals.search_results_title,
                results.size(),
                String.valueOf(results.size()),
                query));

        if (results.isEmpty()) {
            mResultsList.setVisibility(View.GONE);
            mEmptyState.setVisibility(View.VISIBLE);
            mEmptyTitle.setText(R.string.notes_search_empty_title);
            mEmptyMessage.setText(R.string.notes_search_empty_message);
            return;
        }

        mEmptyState.setVisibility(View.GONE);
        mResultsList.setVisibility(View.VISIBLE);
        saveRecentSearch(query);
    }

    private void openResult(SearchResultItem item) {
        String query = mSearchInput.getText() == null ? "" : mSearchInput.getText().toString().trim();
        saveRecentSearch(query);
        Intent intent = new Intent(this, NoteEditActivity.class);
        intent.setAction(Intent.ACTION_VIEW);
        intent.putExtra(Intent.EXTRA_UID, item.noteId);
        intent.putExtra(SearchManager.EXTRA_DATA_KEY, String.valueOf(item.noteId));
        intent.putExtra(SearchManager.USER_QUERY, query);
        startActivity(intent);
    }

    private void renderRecentSearches() {
        List<String> searches = getRecentSearches();
        mRecentSearchesGroup.removeAllViews();
        boolean hasHistory = !searches.isEmpty();
        mHistorySection.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
        mClearHistoryButton.setVisibility(hasHistory ? View.VISIBLE : View.GONE);
        if (!hasHistory) {
            return;
        }
        for (String search : searches) {
            Chip chip = new Chip(this);
            chip.setText(search);
            chip.setCheckable(false);
            chip.setClickable(true);
            chip.setEnsureMinTouchTargetSize(true);
            chip.setOnClickListener(view -> applyRecentSearch(search));
            mRecentSearchesGroup.addView(chip);
        }
    }

    private void applyRecentSearch(String search) {
        mSearchInput.setText(search);
        mSearchInput.setSelection(search.length());
    }

    private void clearRecentSearches() {
        mPreferences.edit().remove(PREF_RECENT_SEARCHES).apply();
        renderRecentSearches();
    }

    private void saveRecentSearch(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (TextUtils.isEmpty(normalizedQuery)) {
            return;
        }
        ArrayList<String> updatedSearches = new ArrayList<String>(getRecentSearches());
        updatedSearches.remove(normalizedQuery);
        updatedSearches.add(0, normalizedQuery);
        if (updatedSearches.size() > MAX_HISTORY_SIZE) {
            updatedSearches = new ArrayList<String>(updatedSearches.subList(0, MAX_HISTORY_SIZE));
        }
        StringBuilder serialized = new StringBuilder();
        for (int index = 0; index < updatedSearches.size(); index++) {
            if (index > 0) {
                serialized.append(HISTORY_SEPARATOR);
            }
            serialized.append(updatedSearches.get(index));
        }
        mPreferences.edit().putString(PREF_RECENT_SEARCHES, serialized.toString()).apply();
    }

    private List<String> getRecentSearches() {
        String raw = mPreferences.getString(PREF_RECENT_SEARCHES, "");
        ArrayList<String> searches = new ArrayList<String>();
        if (TextUtils.isEmpty(raw)) {
            return searches;
        }
        String[] parts = raw.split(HISTORY_SEPARATOR);
        for (String part : parts) {
            if (!TextUtils.isEmpty(part)) {
                searches.add(part);
            }
        }
        return searches;
    }

    private static String formatSnippet(String rawSnippet) {
        if (TextUtils.isEmpty(rawSnippet)) {
            return "";
        }
        return rawSnippet.trim().replaceAll("\\s+", " ");
    }

    private interface SearchResultClickListener {
        void onClick(SearchResultItem item);
    }

    private static final class SearchResultItem {
        private final long noteId;
        private final String snippet;
        private final long modifiedDate;

        private SearchResultItem(long noteId, String snippet, long modifiedDate) {
            this.noteId = noteId;
            this.snippet = snippet;
            this.modifiedDate = modifiedDate;
        }
    }

    private static final class SearchResultViewHolder extends RecyclerView.ViewHolder {
        private final TextView title;
        private final TextView subtitle;

        private SearchResultViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.search_result_title);
            subtitle = itemView.findViewById(R.id.search_result_subtitle);
        }
    }

    private final class SearchResultsAdapter
            extends RecyclerView.Adapter<SearchResultViewHolder> {
        private final List<SearchResultItem> items = new ArrayList<SearchResultItem>();
        private final SearchResultClickListener listener;
        private String query = "";

        private SearchResultsAdapter(SearchResultClickListener listener) {
            this.listener = listener;
        }

        private void submit(String query, List<SearchResultItem> results) {
            String oldQuery = this.query;
            String newQuery = query == null ? "" : query.trim();
            List<SearchResultItem> oldItems = new ArrayList<SearchResultItem>(items);
            List<SearchResultItem> newItems = new ArrayList<SearchResultItem>(results);
            DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                @Override
                public int getOldListSize() {
                    return oldItems.size();
                }

                @Override
                public int getNewListSize() {
                    return newItems.size();
                }

                @Override
                public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                    return oldItems.get(oldItemPosition).noteId
                            == newItems.get(newItemPosition).noteId;
                }

                @Override
                public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                    SearchResultItem oldItem = oldItems.get(oldItemPosition);
                    SearchResultItem newItem = newItems.get(newItemPosition);
                    return oldItem.modifiedDate == newItem.modifiedDate
                            && TextUtils.equals(oldItem.snippet, newItem.snippet)
                            && TextUtils.equals(oldQuery, newQuery);
                }
            });

            this.query = newQuery;
            items.clear();
            items.addAll(newItems);
            diffResult.dispatchUpdatesTo(this);
        }

        @NonNull
        @Override
        public SearchResultViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.search_result_item, parent, false);
            return new SearchResultViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull SearchResultViewHolder holder, int position) {
            SearchResultItem item = items.get(position);
            String displaySnippet = TextUtils.isEmpty(item.snippet)
                    ? getString(R.string.notes_search_untitled_note)
                    : item.snippet;
            holder.title.setText(highlightQuery(displaySnippet, query));
            holder.subtitle.setText(DateUtils.getRelativeTimeSpanString(
                    item.modifiedDate,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS));
            holder.itemView.setOnClickListener(view -> listener.onClick(item));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        private Spannable highlightQuery(String text, String query) {
            SpannableString spannable = new SpannableString(text);
            if (TextUtils.isEmpty(query) || TextUtils.isEmpty(text)) {
                return spannable;
            }
            Pattern pattern = Pattern.compile(Pattern.quote(query), Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                spannable.setSpan(new BackgroundColorSpan(ContextCompat.getColor(
                                SearchNotesActivity.this, R.color.user_query_highlight)),
                        matcher.start(), matcher.end(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            return spannable;
        }
    }
}
