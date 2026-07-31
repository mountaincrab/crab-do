package com.mountaincrab.crabdo.ui.boards.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector2D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.getSelectedText
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.round
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import com.mountaincrab.crabdo.data.local.entity.ColumnEntity
import com.mountaincrab.crabdo.data.local.entity.SubtaskEntity
import com.mountaincrab.crabdo.data.local.entity.TaskEntity
import com.mountaincrab.crabdo.ui.theme.Eyebrow
import com.mountaincrab.crabdo.ui.theme.PillButton
import com.mountaincrab.crabdo.ui.theme.PillGroup
import com.mountaincrab.crabdo.ui.util.AddLinkDialog
import com.mountaincrab.crabdo.ui.util.insertMarkdownLink
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun AddCardDialog(
    columns: List<ColumnEntity>,
    currentColumnId: String,
    onAdd: (title: String, description: String, reminderTimeMillis: Long?, reminderStyle: TaskEntity.ReminderStyle, columnId: String) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(TextFieldValue("")) }
    var description by remember { mutableStateOf(TextFieldValue("")) }
    var selectedColumnId by remember { mutableStateOf(currentColumnId) }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderStyle by remember { mutableStateOf(TaskEntity.ReminderStyle.NOTIFICATION) }
    var reminderMillis by remember { mutableStateOf(defaultReminderTime()) }

    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    val submit = {
        if (title.text.isNotBlank()) {
            onAdd(
                title.text.trim(),
                description.text.trim(),
                if (reminderEnabled) reminderMillis else null,
                reminderStyle,
                selectedColumnId
            )
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.fillMaxSize()) {
                DialogHeader(title = "New Task", onClose = onDismiss) {
                    TextButton(onClick = submit, enabled = title.text.isNotBlank()) {
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TaskFormFields(
                        title = title,
                        onTitleChange = { title = it },
                        description = description,
                        onDescriptionChange = { description = it },
                        columns = columns,
                        selectedColumnId = selectedColumnId,
                        onColumnSelected = { selectedColumnId = it },
                        reminderEnabled = reminderEnabled,
                        onReminderEnabledChange = { reminderEnabled = it },
                        reminderStyle = reminderStyle,
                        onReminderStyleChange = { reminderStyle = it },
                        reminderMillis = reminderMillis,
                        onReminderMillisChange = { reminderMillis = it },
                        titleFocusRequester = focusRequester,
                        // On the New Task dialog, the title field's return key
                        // submits (adds the task + closes) instead of advancing
                        // to Notes. The submit lambda no-ops on a blank title and
                        // onAdd dismisses the dialog.
                        titleImeAction = ImeAction.Done,
                        onTitleImeAction = submit,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun EditCardDialog(
    task: TaskEntity,
    columns: List<ColumnEntity>,
    subtasks: List<SubtaskEntity>,
    onSave: (title: String, description: String, reminderTimeMillis: Long?, reminderStyle: TaskEntity.ReminderStyle, columnId: String) -> Unit,
    onDelete: () -> Unit,
    onAddSubtask: (String) -> Unit,
    onToggleSubtask: (String, Boolean) -> Unit,
    onDeleteSubtask: (String) -> Unit,
    onRenameSubtask: (String, String) -> Unit,
    onReorderSubtask: (String, Double, Double) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(TextFieldValue(task.title)) }
    var description by remember { mutableStateOf(TextFieldValue(task.description)) }
    var selectedColumnId by remember { mutableStateOf(task.columnId) }
    var reminderEnabled by remember { mutableStateOf(task.reminderTimeMillis != null) }
    var reminderStyle by remember { mutableStateOf(task.reminderStyle) }
    var reminderMillis by remember { mutableStateOf(task.reminderTimeMillis ?: defaultReminderTime()) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var newSubtask by remember { mutableStateOf("") }
    // The subtask composer is hidden behind an "+ Add subtask" button until
    // tapped (mirrors the board column's "+ Add task"). Back collapses it, and
    // so does moving focus away (e.g. tapping the title field) — see
    // subtaskFieldHadFocus below.
    var composerVisible by remember { mutableStateOf(false) }
    // Tracks whether the composer's text field has actually held focus, so the
    // initial unfocused onFocusChanged event (fired before requestFocus) isn't
    // mistaken for the user moving focus away and doesn't collapse the composer.
    var subtaskFieldHadFocus by remember { mutableStateOf(false) }
    val subtaskFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val scrollState = rememberScrollState()
    // Set when the user adds a subtask so the next list emission scrolls the
    // newest row into view; gated so the initial load (empty → populated) does
    // not yank an existing list to the bottom.
    var scrollToNewSubtask by remember { mutableStateOf(false) }

    // Add the subtask but keep the field focused and the keyboard up so the
    // next one can be typed straight away (the IME "Done" tick / icon tap would
    // otherwise dismiss the keyboard).
    val addSubtaskAndContinue = {
        if (newSubtask.isNotBlank()) {
            onAddSubtask(newSubtask.trim())
            newSubtask = ""
            scrollToNewSubtask = true
            subtaskFocusRequester.requestFocus()
            keyboardController?.show()
        }
    }

    // Local copy of the subtask list so long-press drag reorder can shuffle items
    // without waiting for the DB round-trip; resynced from the flow when not dragging.
    val displaySubtasks = remember { mutableStateOf(subtasks) }
    val currentSubtasksRef = rememberUpdatedState(subtasks)
    var draggingSubtaskId by remember { mutableStateOf<String?>(null) }
    // When a subtask is checked off we briefly hold the list in its current order
    // (showing the tick in place) before letting it re-sort, so the completion is
    // visible before the row slides into the completed group.
    var holdReorderId by remember { mutableStateOf<String?>(null) }
    var dragOffsetY by remember { mutableFloatStateOf(0f) }
    var itemHeightPx by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val subtaskSpacingPx = with(density) { SubtaskRowSpacing.toPx() }
    val haptic = LocalHapticFeedback.current

    // Don't pull the freshly re-sorted list from the flow while dragging or while
    // holding a just-completed row in place — those windows manage the order
    // locally (the hold is released by the timer effect below).
    LaunchedEffect(subtasks) {
        if (draggingSubtaskId == null && holdReorderId == null) displaySubtasks.value = subtasks
    }

    // Releases the post-completion hold: keep the ticked row in place for a beat,
    // then resync to the sorted list so it slides into the completed group.
    LaunchedEffect(holdReorderId) {
        if (holdReorderId != null) {
            kotlinx.coroutines.delay(167)
            holdReorderId = null
            if (draggingSubtaskId == null) displaySubtasks.value = currentSubtasksRef.value
        }
    }

    // Persist the toggle immediately, but on completion optimistically flip the
    // tick in place and start the hold so the slide is deferred ~167ms.
    val handleToggleSubtask: (String, Boolean) -> Unit = { id, completed ->
        onToggleSubtask(id, completed)
        if (completed && holdReorderId == null && draggingSubtaskId == null) {
            displaySubtasks.value = displaySubtasks.value.map {
                if (it.id == id) it.copy(isCompleted = true) else it
            }
            holdReorderId = id
        }
    }

    // After a user adds a subtask, scroll to the bottom so the new row and the
    // composer are visible.
    LaunchedEffect(displaySubtasks.value.size) {
        if (scrollToNewSubtask) {
            scrollState.animateScrollTo(scrollState.maxValue)
            scrollToNewSubtask = false
        }
    }

    // Persist the current field values. There is no Save button — edits autosave
    // when the editor is closed (cross icon or back press). A blank title is left
    // unsaved so closing never wipes the task's title to empty.
    val submit = {
        if (title.text.isNotBlank()) {
            onSave(
                title.text.trim(),
                description.text.trim(),
                if (reminderEnabled) reminderMillis else null,
                reminderStyle,
                selectedColumnId
            )
        }
    }

    // Both close affordances (cross icon, Android back button) save first, then
    // dismiss — so there is no way to lose edits by exiting.
    val saveAndDismiss = {
        submit()
        onDismiss()
    }

    // Render the editor as a full-screen overlay in the host (activity) window
    // rather than a Compose Dialog. A Dialog gets its own sub-window, which on
    // Android 15/16 is inset below the status bar yet sized to the full display, so
    // a bottom-anchored composer ends up off the bottom edge. The activity window
    // bounds the height and dispatches system-bar/ime insets correctly, so
    // weight(1f) distributes real space and the scrolling form lifts above the
    // keyboard via imePadding(). Back collapses the subtask composer if open,
    // otherwise saves + dismisses (autosave).
    BackHandler {
        if (composerVisible) {
            composerVisible = false
            newSubtask = ""
            keyboardController?.hide()
        } else {
            saveAndDismiss()
        }
    }
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            DialogHeader(title = "Edit Task", onClose = saveAndDismiss) {
                IconButton(onClick = { showDeleteConfirm = true }) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete task",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
            // Form + checklist scroll in this region, which takes the space left
            // below the header and above the pinned subtask composer. imePadding()
            // lifts it above the keyboard while the title/notes fields are being
            // edited, but is dropped while the subtask composer is open — the
            // composer is then a bar pinned below this region carrying its own
            // imePadding(), so padding here too would double-count the keyboard.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .then(if (composerVisible) Modifier else Modifier.imePadding())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TaskFormFields(
                    title = title,
                    onTitleChange = { title = it },
                    description = description,
                    onDescriptionChange = { description = it },
                    columns = columns,
                    selectedColumnId = selectedColumnId,
                    onColumnSelected = { selectedColumnId = it },
                    reminderEnabled = reminderEnabled,
                    onReminderEnabledChange = { reminderEnabled = it },
                    reminderStyle = reminderStyle,
                    onReminderStyleChange = { reminderStyle = it },
                    reminderMillis = reminderMillis,
                    onReminderMillisChange = { reminderMillis = it },
                    titleFocusRequester = null,
                )
                Eyebrow("Checklist")

                // The checklist gets its own column so its rows sit tight
                // against each other (SubtaskRowSpacing) instead of inheriting
                // the form's 12dp gap between fields.
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(SubtaskRowSpacing)
                ) {
                displaySubtasks.value.forEach { subtask ->
                    key(subtask.id) {
                    val isDragging = subtask.id == draggingSubtaskId
                    SubtaskItem(
                        subtask = subtask,
                        onToggle = { handleToggleSubtask(subtask.id, it) },
                        onDelete = { onDeleteSubtask(subtask.id) },
                        onRename = { onRenameSubtask(subtask.id, it) },
                        modifier = Modifier
                            .zIndex(if (isDragging) 1f else 0f)
                            // Slide the row to its new slot when the list
                            // re-sorts (e.g. checking it off moves it into the
                            // completed group) so the toggle gives visible
                            // feedback instead of snapping. Omitted for the row
                            // being dragged — the drag's own translationY drives
                            // it, and a placement offset would fight the
                            // per-slot translationY compensation.
                            .then(if (isDragging) Modifier else Modifier.animatePlacement())
                            .graphicsLayer {
                                translationY = if (isDragging) dragOffsetY else 0f
                                alpha = if (isDragging) 0.85f else 1f
                            }
                            .onSizeChanged { size ->
                                if (size.height > 0) itemHeightPx = size.height.toFloat()
                            }
                            .pointerInput(subtask.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        draggingSubtaskId = subtask.id
                                        dragOffsetY = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffsetY += dragAmount.y
                                        val slotH = if (itemHeightPx > 0f)
                                            itemHeightPx + subtaskSpacingPx
                                        else with(density) { 56.dp.toPx() }
                                        var curIdx = displaySubtasks.value
                                            .indexOfFirst { it.id == subtask.id }
                                        if (curIdx < 0) return@detectDragGesturesAfterLongPress
                                        while (curIdx < displaySubtasks.value.size - 1 &&
                                            dragOffsetY > slotH / 2f) {
                                            val m = displaySubtasks.value.toMutableList()
                                            m.add(curIdx + 1, m.removeAt(curIdx))
                                            displaySubtasks.value = m
                                            dragOffsetY -= slotH
                                            curIdx++
                                        }
                                        while (curIdx > 0 && dragOffsetY < -slotH / 2f) {
                                            val m = displaySubtasks.value.toMutableList()
                                            m.add(curIdx - 1, m.removeAt(curIdx))
                                            displaySubtasks.value = m
                                            dragOffsetY += slotH
                                            curIdx--
                                        }
                                    },
                                    onDragEnd = {
                                        val current = displaySubtasks.value
                                        val idx = current.indexOfFirst { it.id == subtask.id }
                                        if (idx >= 0) {
                                            val prevOrder = current.getOrNull(idx - 1)?.order
                                            val nextOrder = current.getOrNull(idx + 1)?.order
                                            onReorderSubtask(
                                                subtask.id,
                                                prevOrder ?: ((nextOrder ?: 1.0) - 2.0),
                                                nextOrder ?: ((prevOrder ?: 0.0) + 2.0)
                                            )
                                        }
                                        draggingSubtaskId = null
                                        dragOffsetY = 0f
                                    },
                                    onDragCancel = {
                                        draggingSubtaskId = null
                                        dragOffsetY = 0f
                                        displaySubtasks.value = currentSubtasksRef.value
                                    }
                                )
                            }
                    )
                    }
                }
                }

                // Collapsed add-subtask affordance at the end of the checklist
                // (like the board's "+ Add task"). Tapping it opens the composer,
                // which is rendered as a bar pinned above the keyboard *below* this
                // scroll region (see below) rather than inline here: an inline
                // composer at the tail of a long checklist lands behind the keyboard
                // and has to be scrolled to, whereas a pinned bar is always in view.
                if (!composerVisible) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { subtaskFieldHadFocus = false; composerVisible = true }
                            .padding(horizontal = 8.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "+ Add subtask",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 0.sp,
                                fontSize = 13.sp,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // Subtask composer, pinned as a bar directly above the keyboard (and
            // above the system nav bar when the keyboard is down) via imePadding() +
            // navigationBarsPadding(), outside the scroll region. Because it is
            // pinned it is visible the instant the keyboard opens — no scroll-into-
            // view race, which is what made the inline version disappear behind the
            // keyboard on a long checklist. Done adds the subtask and keeps the
            // field open + cleared so the next one can be typed straight away; back
            // collapses it (handled by BackHandler above).
            if (composerVisible) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .imePadding()
                        .navigationBarsPadding()
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = newSubtask,
                            onValueChange = { newSubtask = it },
                            placeholder = { Text("Add a subtask…") },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(subtaskFocusRequester)
                                // Collapse the composer back to the "+ Add
                                // subtask" button when focus leaves the field
                                // (e.g. the user taps the title/notes field).
                                // Guarded by subtaskFieldHadFocus so the initial
                                // unfocused state — emitted before requestFocus
                                // runs — doesn't immediately collapse it.
                                .onFocusChanged { focusState ->
                                    if (focusState.isFocused) {
                                        subtaskFieldHadFocus = true
                                    } else if (subtaskFieldHadFocus) {
                                        composerVisible = false
                                        newSubtask = ""
                                    }
                                },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            keyboardOptions = KeyboardOptions(
                                imeAction = ImeAction.Done,
                                capitalization = KeyboardCapitalization.Sentences
                            ),
                            keyboardActions = KeyboardActions(onDone = { addSubtaskAndContinue() })
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = addSubtaskAndContinue,
                            enabled = newSubtask.isNotBlank()
                        ) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = "Add subtask",
                                tint = if (newSubtask.isNotBlank()) MaterialTheme.colorScheme.primary
                                       else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            )
                        }
                    }
                }
            }
        }
    }

    // The composer is pinned above the keyboard, so it is always visible. Focus it
    // and raise the keyboard, then keep the checklist scrolled to its bottom so the
    // most recent rows sit right above the composer as the keyboard/layout settle
    // (maxValue grows over several frames as imePadding takes effect, so follow it
    // rather than reading it once).
    LaunchedEffect(composerVisible) {
        if (composerVisible) {
            subtaskFocusRequester.requestFocus()
            keyboardController?.show()
            snapshotFlow { scrollState.maxValue }
                .collect { max -> scrollState.animateScrollTo(max) }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete task?") },
            text = { Text("\"${task.title}\" will be permanently deleted.") },
            confirmButton = {
                TextButton(
                    onClick = { showDeleteConfirm = false; onDelete() },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun DialogHeader(
    title: String,
    onClose: () -> Unit,
    actions: @Composable RowScope.() -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = "Close")
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .weight(1f)
                .padding(start = 4.dp)
        )
        actions()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskFormFields(
    title: TextFieldValue,
    onTitleChange: (TextFieldValue) -> Unit,
    description: TextFieldValue,
    onDescriptionChange: (TextFieldValue) -> Unit,
    columns: List<ColumnEntity>,
    selectedColumnId: String,
    onColumnSelected: (String) -> Unit,
    reminderEnabled: Boolean,
    onReminderEnabledChange: (Boolean) -> Unit,
    reminderStyle: TaskEntity.ReminderStyle,
    onReminderStyleChange: (TaskEntity.ReminderStyle) -> Unit,
    reminderMillis: Long,
    onReminderMillisChange: (Long) -> Unit,
    titleFocusRequester: FocusRequester?,
    titleImeAction: ImeAction = ImeAction.Next,
    onTitleImeAction: () -> Unit = {},
) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showTitleLinkDialog by remember { mutableStateOf(false) }
    var showNotesLinkDialog by remember { mutableStateOf(false) }

    Eyebrow("Title")
    OutlinedTextField(
        value = title,
        onValueChange = onTitleChange,
        placeholder = { Text("E.g., Review Q3 Metrics…") },
        modifier = Modifier
            .fillMaxWidth()
            .then(if (titleFocusRequester != null) Modifier.focusRequester(titleFocusRequester) else Modifier),
        singleLine = true,
        trailingIcon = {
            IconButton(onClick = { showTitleLinkDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "Add link",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        keyboardOptions = KeyboardOptions(
            imeAction = titleImeAction,
            capitalization = KeyboardCapitalization.Sentences
        ),
        keyboardActions = KeyboardActions(
            onDone = { onTitleImeAction() }
        ),
        shape = RoundedCornerShape(12.dp)
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Eyebrow("Notes")
        TextButton(onClick = { showNotesLinkDialog = true }) {
            Icon(
                imageVector = Icons.Default.Link,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text("Add link", style = MaterialTheme.typography.labelMedium)
        }
    }
    OutlinedTextField(
        value = description,
        onValueChange = onDescriptionChange,
        placeholder = { Text("Add details or notes…") },
        modifier = Modifier.fillMaxWidth(),
        minLines = 2,
        maxLines = 4,
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
    )

    if (showTitleLinkDialog) {
        AddLinkDialog(
            initialLabel = title.getSelectedText().text,
            onDismiss = { showTitleLinkDialog = false },
            onConfirm = { label, url ->
                onTitleChange(insertMarkdownLink(title, label, url))
                showTitleLinkDialog = false
            },
        )
    }
    if (showNotesLinkDialog) {
        AddLinkDialog(
            initialLabel = description.getSelectedText().text,
            onDismiss = { showNotesLinkDialog = false },
            onConfirm = { label, url ->
                onDescriptionChange(insertMarkdownLink(description, label, url))
                showNotesLinkDialog = false
            },
        )
    }

    Eyebrow("Column")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        columns.forEach { col ->
            val isSelected = col.id == selectedColumnId
            Button(
                onClick = { onColumnSelected(col.id) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(10.dp),
                contentPadding = PaddingValues(vertical = 10.dp, horizontal = 8.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isSelected)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = if (isSelected)
                        MaterialTheme.colorScheme.onPrimary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                ),
                border = if (!isSelected)
                    BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                else null,
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
            ) {
                Text(
                    text = col.title,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium
                )
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Task Reminder", fontWeight = FontWeight.Bold)
                    Text(
                        "Alert me about this task",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = reminderEnabled,
                    onCheckedChange = onReminderEnabledChange
                )
            }

            if (reminderEnabled) {
                Spacer(Modifier.height(12.dp))
                PillGroup {
                    PillButton(
                        selected = reminderStyle == TaskEntity.ReminderStyle.NOTIFICATION,
                        onClick = { onReminderStyleChange(TaskEntity.ReminderStyle.NOTIFICATION) },
                        icon = Icons.Default.Notifications,
                        text = "Notification",
                    )
                    PillButton(
                        selected = reminderStyle == TaskEntity.ReminderStyle.ALARM,
                        onClick = { onReminderStyleChange(TaskEntity.ReminderStyle.ALARM) },
                        icon = Icons.Default.AccessAlarm,
                        text = "Alarm",
                    )
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickChip("Later") { onReminderMillisChange(quickPreset(QuickPreset.LATER_TODAY)) }
                    QuickChip("Tomorrow") { onReminderMillisChange(quickPreset(QuickPreset.TOMORROW)) }
                    QuickChip("Next Wk") { onReminderMillisChange(quickPreset(QuickPreset.NEXT_WEEK)) }
                }

                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedCard(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                "Date",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = SimpleDateFormat("EEE, d MMM", Locale.getDefault())
                                    .format(Date(reminderMillis)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    OutlinedCard(
                        onClick = { showTimePicker = true },
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(
                                "Time",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = SimpleDateFormat("HH:mm", Locale.getDefault())
                                    .format(Date(reminderMillis)),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = localDateToUtcMidnight(reminderMillis)
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { dateMillis ->
                        val cal = Calendar.getInstance().apply {
                            timeInMillis = dateMillis
                            val existing = Calendar.getInstance().also { it.timeInMillis = reminderMillis }
                            set(Calendar.HOUR_OF_DAY, existing.get(Calendar.HOUR_OF_DAY))
                            set(Calendar.MINUTE, existing.get(Calendar.MINUTE))
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        onReminderMillisChange(cal.timeInMillis)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val initialCal = remember { Calendar.getInstance().apply { timeInMillis = reminderMillis } }
        val timePickerState = rememberTimePickerState(
            initialHour = initialCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = initialCal.get(Calendar.MINUTE),
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            title = { Text("Set time") },
            text = { TimePicker(state = timePickerState) },
            confirmButton = {
                TextButton(onClick = {
                    val cal = Calendar.getInstance().apply {
                        timeInMillis = reminderMillis
                        set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                        set(Calendar.MINUTE, timePickerState.minute)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    onReminderMillisChange(cal.timeInMillis)
                    showTimePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) { Text("Cancel") }
            }
        )
    }
}

/**
 * Animates this composable's placement within its parent: when the layout moves
 * the row to a new slot (e.g. the checklist re-sorts a just-completed subtask
 * into the completed group), the row slides there instead of snapping, giving
 * the user visible confirmation the toggle registered.
 *
 * The first real placement is adopted without animation (the [Animatable] is
 * seeded from it), so rows don't fly in from the origin on first composition.
 */
private fun Modifier.animatePlacement(): Modifier = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf<IntOffset?>(null) }
    var animatable by remember {
        mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null)
    }
    this
        .onPlaced { coordinates ->
            targetOffset = coordinates.positionInParent().round()
        }
        .offset {
            val target = targetOffset ?: return@offset IntOffset.Zero
            val anim = animatable
                ?: Animatable(target, IntOffset.VectorConverter).also { animatable = it }
            if (anim.targetValue != target) {
                scope.launch {
                    anim.animateTo(
                        target,
                        spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    )
                }
            }
            anim.value - target
        }
}

@Composable
private fun QuickChip(label: String, onClick: () -> Unit) {
    AssistChip(
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) }
    )
}

private enum class QuickPreset { LATER_TODAY, TOMORROW, NEXT_WEEK }

private fun defaultReminderTime(): Long {
    val cal = Calendar.getInstance()
    cal.add(Calendar.HOUR_OF_DAY, 1)
    cal.set(Calendar.MINUTE, 0)
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun quickPreset(preset: QuickPreset): Long {
    val cal = Calendar.getInstance()
    when (preset) {
        QuickPreset.LATER_TODAY -> {
            cal.add(Calendar.HOUR_OF_DAY, 3)
            cal.set(Calendar.MINUTE, 0)
        }
        QuickPreset.TOMORROW -> {
            cal.add(Calendar.DAY_OF_YEAR, 1)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }
        QuickPreset.NEXT_WEEK -> {
            cal.add(Calendar.DAY_OF_YEAR, 7)
            cal.set(Calendar.HOUR_OF_DAY, 9)
            cal.set(Calendar.MINUTE, 0)
        }
    }
    cal.set(Calendar.SECOND, 0)
    cal.set(Calendar.MILLISECOND, 0)
    return cal.timeInMillis
}

private fun localDateToUtcMidnight(localMillis: Long): Long {
    val local = Calendar.getInstance()
    local.timeInMillis = localMillis
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
        set(
            local.get(Calendar.YEAR),
            local.get(Calendar.MONTH),
            local.get(Calendar.DAY_OF_MONTH),
            0, 0, 0
        )
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis
}
