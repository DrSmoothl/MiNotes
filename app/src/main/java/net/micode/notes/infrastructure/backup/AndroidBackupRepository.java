package net.micode.notes.infrastructure.backup;

import android.content.Context;

import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.repository.BackupRepository;
import net.micode.notes.tool.BackupUtils;

public final class AndroidBackupRepository implements BackupRepository {
    private final Context appContext;

    public AndroidBackupRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    @Override
    public ExportedTextFile exportToText() {
        BackupUtils backupUtils = BackupUtils.getInstance(appContext);
        int result = backupUtils.exportToText();
        if (result == BackupUtils.STATE_SUCCESS) {
            return new ExportedTextFile(ExportedTextFile.ExportState.SUCCESS,
                    backupUtils.getExportedTextFileName(), backupUtils.getExportedTextFileDir());
        }
        if (result == BackupUtils.STATE_SD_CARD_UNMOUONTED) {
            return new ExportedTextFile(ExportedTextFile.ExportState.STORAGE_UNAVAILABLE, "", "");
        }
        return new ExportedTextFile(ExportedTextFile.ExportState.FAILURE, "", "");
    }
}