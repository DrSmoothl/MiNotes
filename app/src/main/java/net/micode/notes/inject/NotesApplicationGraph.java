package net.micode.notes.inject;

import android.content.Context;

import net.micode.notes.domain.repository.BackupRepository;
import net.micode.notes.domain.repository.IntroductionRepository;
import net.micode.notes.domain.repository.NoteEditorRepository;
import net.micode.notes.domain.repository.NoteRepository;
import net.micode.notes.domain.service.ContactNameResolver;
import net.micode.notes.domain.service.ReminderScheduler;
import net.micode.notes.domain.service.WidgetNotifier;
import net.micode.notes.domain.usecase.editor.DeleteNoteUseCase;
import net.micode.notes.domain.usecase.editor.GetAlarmNotePreviewUseCase;
import net.micode.notes.domain.usecase.editor.StartNoteEditorSessionUseCase;
import net.micode.notes.domain.usecase.list.DeleteNotesUseCase;
import net.micode.notes.domain.usecase.list.ExportNotesUseCase;
import net.micode.notes.domain.usecase.list.FolderManagementUseCase;
import net.micode.notes.domain.usecase.list.ClearWidgetBindingsUseCase;
import net.micode.notes.domain.usecase.list.GetWidgetNoteStateUseCase;
import net.micode.notes.domain.usecase.list.LoadNotesUseCase;
import net.micode.notes.domain.usecase.list.RestoreRemindersUseCase;
import net.micode.notes.domain.usecase.startup.InitializeIntroductionNoteUseCase;
import net.micode.notes.infrastructure.backup.AndroidBackupRepository;
import net.micode.notes.infrastructure.contentresolver.ContentResolverNoteRepository;
import net.micode.notes.infrastructure.editor.ContentResolverNoteEditorRepository;
import net.micode.notes.infrastructure.system.AndroidContactNameResolver;
import net.micode.notes.infrastructure.system.AndroidIntroductionRepository;
import net.micode.notes.infrastructure.system.AndroidReminderScheduler;
import net.micode.notes.infrastructure.system.AndroidWidgetNotifier;

public final class NotesApplicationGraph {
    private final NoteRepository noteRepository;
    private final BackupRepository backupRepository;
    private final NoteEditorRepository noteEditorRepository;
    private final IntroductionRepository introductionRepository;
    private final ContactNameResolver contactNameResolver;
    private final ReminderScheduler reminderScheduler;
    private final WidgetNotifier widgetNotifier;

    public NotesApplicationGraph(Context context) {
        Context appContext = context.getApplicationContext();
        this.contactNameResolver = new AndroidContactNameResolver(appContext);
        this.noteRepository = new ContentResolverNoteRepository(appContext,
                appContext.getContentResolver(), contactNameResolver);
        this.backupRepository = new AndroidBackupRepository(appContext);
        this.noteEditorRepository = new ContentResolverNoteEditorRepository(appContext);
        this.introductionRepository = new AndroidIntroductionRepository(appContext);
        this.reminderScheduler = new AndroidReminderScheduler(appContext);
        this.widgetNotifier = new AndroidWidgetNotifier(appContext);
    }

    public LoadNotesUseCase loadNotesUseCase() {
        return new LoadNotesUseCase(noteRepository);
    }

    public FolderManagementUseCase folderManagementUseCase() {
        return new FolderManagementUseCase(noteRepository);
    }

    public DeleteNotesUseCase deleteNotesUseCase() {
        return new DeleteNotesUseCase(noteRepository);
    }

    public ExportNotesUseCase exportNotesUseCase() {
        return new ExportNotesUseCase(backupRepository);
    }

    public ClearWidgetBindingsUseCase clearWidgetBindingsUseCase() {
        return new ClearWidgetBindingsUseCase(noteRepository);
    }

    public GetWidgetNoteStateUseCase getWidgetNoteStateUseCase() {
        return new GetWidgetNoteStateUseCase(noteRepository);
    }

    public RestoreRemindersUseCase restoreRemindersUseCase() {
        return new RestoreRemindersUseCase(noteRepository, reminderScheduler);
    }

    public StartNoteEditorSessionUseCase startNoteEditorSessionUseCase() {
        return new StartNoteEditorSessionUseCase(noteEditorRepository);
    }

    public DeleteNoteUseCase deleteNoteUseCase() {
        return new DeleteNoteUseCase(noteEditorRepository);
    }

    public GetAlarmNotePreviewUseCase getAlarmNotePreviewUseCase() {
        return new GetAlarmNotePreviewUseCase(noteEditorRepository);
    }

    public InitializeIntroductionNoteUseCase initializeIntroductionNoteUseCase() {
        return new InitializeIntroductionNoteUseCase(introductionRepository,
                startNoteEditorSessionUseCase());
    }

    public ReminderScheduler reminderScheduler() {
        return reminderScheduler;
    }

    public WidgetNotifier widgetNotifier() {
        return widgetNotifier;
    }
}