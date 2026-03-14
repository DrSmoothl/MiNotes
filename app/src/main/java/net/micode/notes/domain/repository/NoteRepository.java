package net.micode.notes.domain.repository;

import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.NoteListItem;
import net.micode.notes.domain.model.ScheduledReminder;
import net.micode.notes.domain.model.WidgetBinding;
import net.micode.notes.domain.model.WidgetNoteState;

import java.util.List;
import java.util.Set;

public interface NoteRepository {
    List<NoteListItem> getNotes(long folderId);

    List<FolderDestination> getFolderDestinations(long currentFolderId, boolean includeRoot);

    int getUserFolderCount();

    boolean isVisibleFolderName(String name);

    long createFolder(String name);

    boolean renameFolder(long folderId, String name);

    boolean deleteNotes(Set<Long> ids);

    boolean moveNotes(Set<Long> ids, long folderId);

    Set<WidgetBinding> getWidgetsForFolder(long folderId);

    List<ScheduledReminder> getUpcomingReminders(long currentTimeMillis);

    WidgetNoteState getWidgetNoteState(int widgetId);

    void clearWidgetBindings(int[] widgetIds);
}