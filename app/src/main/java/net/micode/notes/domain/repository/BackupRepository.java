package net.micode.notes.domain.repository;

import net.micode.notes.domain.model.ExportedTextFile;

public interface BackupRepository {
    ExportedTextFile exportToText();
}