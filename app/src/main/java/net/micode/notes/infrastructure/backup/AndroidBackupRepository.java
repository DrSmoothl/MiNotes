package net.micode.notes.infrastructure.backup;

import android.content.Context;
import android.database.Cursor;
import android.os.Environment;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.util.Log;

import net.micode.notes.domain.model.ExportedTextFile;
import net.micode.notes.domain.repository.BackupRepository;
import net.micode.notes.R;
import net.micode.notes.data.Notes;
import net.micode.notes.data.Notes.DataColumns;
import net.micode.notes.data.Notes.DataConstants;
import net.micode.notes.data.Notes.NoteColumns;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

public final class AndroidBackupRepository implements BackupRepository {
    private static final String TAG = "AndroidBackupRepository";
    private static final String[] NOTE_PROJECTION = {
            NoteColumns.ID,
            NoteColumns.MODIFIED_DATE,
            NoteColumns.SNIPPET,
            NoteColumns.TYPE,
    };
    private static final String[] DATA_PROJECTION = {
            DataColumns.CONTENT,
            DataColumns.MIME_TYPE,
            DataColumns.DATA1,
            DataColumns.DATA2,
            DataColumns.DATA3,
            DataColumns.DATA4,
    };

    private final Context appContext;
    private final String[] textFormat;

    public AndroidBackupRepository(Context context) {
        this.appContext = context.getApplicationContext();
        this.textFormat = appContext.getResources().getStringArray(R.array.format_for_exported_note);
    }

    @Override
    public ExportedTextFile exportToText() {
        if (!externalStorageAvailable()) {
            return new ExportedTextFile(ExportedTextFile.ExportState.STORAGE_UNAVAILABLE, "", "");
        }

        File exportFile = generateExportFile();
        if (exportFile == null) {
            return new ExportedTextFile(ExportedTextFile.ExportState.FAILURE, "", "");
        }

        PrintStream printStream = createPrintStream(exportFile);
        if (printStream == null) {
            return new ExportedTextFile(ExportedTextFile.ExportState.FAILURE, "", "");
        }

        try {
            exportFolders(printStream);
            exportRootNotes(printStream);
        } finally {
            printStream.close();
        }

        File parent = exportFile.getParentFile();
        return new ExportedTextFile(ExportedTextFile.ExportState.SUCCESS,
                exportFile.getName(),
                parent == null ? "" : parent.getAbsolutePath());
    }

    private void exportFolders(PrintStream printStream) {
        Cursor folderCursor = appContext.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                NOTE_PROJECTION,
                "(" + NoteColumns.TYPE + "=" + Notes.TYPE_FOLDER + " AND "
                        + NoteColumns.PARENT_ID + "<>" + Notes.ID_TRASH_FOLER + ") OR "
                        + NoteColumns.ID + "=" + Notes.ID_CALL_RECORD_FOLDER,
                null,
                null);
        if (folderCursor == null) {
            return;
        }
        try {
            while (folderCursor.moveToNext()) {
                String folderName;
                if (folderCursor.getLong(0) == Notes.ID_CALL_RECORD_FOLDER) {
                    folderName = appContext.getString(R.string.call_record_folder_name);
                } else {
                    folderName = folderCursor.getString(2);
                }
                if (!TextUtils.isEmpty(folderName)) {
                    printStream.println(String.format(textFormat[0], folderName));
                }
                exportFolderNotes(String.valueOf(folderCursor.getLong(0)), printStream);
            }
        } finally {
            folderCursor.close();
        }
    }

    private void exportRootNotes(PrintStream printStream) {
        Cursor noteCursor = appContext.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                NOTE_PROJECTION,
                NoteColumns.TYPE + "=" + Notes.TYPE_NOTE + " AND " + NoteColumns.PARENT_ID + "=0",
                null,
                null);
        if (noteCursor == null) {
            return;
        }
        try {
            while (noteCursor.moveToNext()) {
                printStream.println(String.format(textFormat[1], DateFormat.format(
                        appContext.getString(R.string.format_datetime_mdhm),
                        noteCursor.getLong(1))));
                exportNote(String.valueOf(noteCursor.getLong(0)), printStream);
            }
        } finally {
            noteCursor.close();
        }
    }

    private void exportFolderNotes(String folderId, PrintStream printStream) {
        Cursor notesCursor = appContext.getContentResolver().query(
                Notes.CONTENT_NOTE_URI,
                NOTE_PROJECTION,
                NoteColumns.PARENT_ID + "=?",
                new String[] { folderId },
                null);
        if (notesCursor == null) {
            return;
        }
        try {
            while (notesCursor.moveToNext()) {
                printStream.println(String.format(textFormat[1], DateFormat.format(
                        appContext.getString(R.string.format_datetime_mdhm),
                        notesCursor.getLong(1))));
                exportNote(String.valueOf(notesCursor.getLong(0)), printStream);
            }
        } finally {
            notesCursor.close();
        }
    }

    private void exportNote(String noteId, PrintStream printStream) {
        Cursor dataCursor = appContext.getContentResolver().query(
                Notes.CONTENT_DATA_URI,
                DATA_PROJECTION,
                DataColumns.NOTE_ID + "=?",
                new String[] { noteId },
                null);
        if (dataCursor == null) {
            return;
        }
        try {
            while (dataCursor.moveToNext()) {
                String mimeType = dataCursor.getString(1);
                if (DataConstants.CALL_NOTE.equals(mimeType)) {
                    String phoneNumber = dataCursor.getString(4);
                    long callDate = dataCursor.getLong(2);
                    String location = dataCursor.getString(0);
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        printStream.println(String.format(textFormat[2], phoneNumber));
                    }
                    printStream.println(String.format(textFormat[2], DateFormat.format(
                            appContext.getString(R.string.format_datetime_mdhm),
                            callDate)));
                    if (!TextUtils.isEmpty(location)) {
                        printStream.println(String.format(textFormat[2], location));
                    }
                } else if (DataConstants.NOTE.equals(mimeType)) {
                    String content = dataCursor.getString(0);
                    if (!TextUtils.isEmpty(content)) {
                        printStream.println(String.format(textFormat[2], content));
                    }
                }
            }
            printStream.println();
        } finally {
            dataCursor.close();
        }
    }

    private static boolean externalStorageAvailable() {
        return Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState());
    }

    private PrintStream createPrintStream(File exportFile) {
        try {
            return new PrintStream(new FileOutputStream(exportFile));
        } catch (FileNotFoundException exception) {
            Log.e(TAG, "Failed to create print stream", exception);
            return null;
        }
    }

    private File generateExportFile() {
        File baseDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS);
        if (baseDir == null) {
            return null;
        }
        File fileDir = new File(baseDir, "notes");
        File file = new File(fileDir, appContext.getString(
                R.string.file_name_txt_format,
                DateFormat.format(appContext.getString(R.string.format_date_ymd),
                        System.currentTimeMillis())));
        try {
            if (!fileDir.exists() && !fileDir.mkdirs()) {
                return null;
            }
            if (!file.exists() && !file.createNewFile()) {
                return null;
            }
            return file;
        } catch (SecurityException exception) {
            Log.e(TAG, "Failed to create export file", exception);
            return null;
        } catch (IOException exception) {
            Log.e(TAG, "Failed to create export file", exception);
            return null;
        }
    }
}