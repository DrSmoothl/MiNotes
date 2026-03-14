package net.micode.notes.domain.usecase.list;

import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.repository.BackupRepository;

public final class ExportNotesUseCase {
    private final BackupRepository backupRepository;

    public ExportNotesUseCase(BackupRepository backupRepository) {
        this.backupRepository = backupRepository;
    }

    public ExportedTextFile exportToText() {
        return backupRepository.exportToText();
    }
}