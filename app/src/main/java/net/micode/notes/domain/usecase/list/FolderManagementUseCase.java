package net.micode.notes.domain.usecase.list;

import net.micode.notes.domain.model.FolderDestination;
import net.micode.notes.domain.model.WidgetBinding;
import net.micode.notes.domain.repository.NoteRepository;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class FolderManagementUseCase {
    private final NoteRepository noteRepository;

    public FolderManagementUseCase(NoteRepository noteRepository) {
        this.noteRepository = noteRepository;
    }

    public int getUserFolderCount() {
        return noteRepository.getUserFolderCount();
    }

    public List<FolderDestination> getFolderDestinations(long currentFolderId,
            boolean includeRoot) {
        return noteRepository.getFolderDestinations(currentFolderId, includeRoot);
    }

    public boolean folderNameExists(String name) {
        return noteRepository.isVisibleFolderName(name);
    }

    public long createFolder(String name) {
        return noteRepository.createFolder(name);
    }

    public boolean renameFolder(long folderId, String name) {
        return noteRepository.renameFolder(folderId, name);
    }

    public boolean moveNotes(Set<Long> ids, long folderId) {
        return noteRepository.moveNotes(ids, folderId);
    }

    public Set<WidgetBinding> deleteFolder(long folderId) {
        if (folderId <= 0) {
            return Collections.emptySet();
        }
        Set<WidgetBinding> widgets = noteRepository.getWidgetsForFolder(folderId);
        HashSet<Long> ids = new HashSet<Long>();
        ids.add(folderId);
        if (!noteRepository.deleteNotes(ids)) {
            return Collections.emptySet();
        }
        return widgets;
    }
}