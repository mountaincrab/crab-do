package com.mountaincrab.crabdo.ui.util

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.material3.Text as M3Text

// Web links are stored inline in the existing plain-string fields (task
// title/description, subtask title) as Markdown — `[label](url)` — and bare
// `https://…` URLs are autolinked. No schema/migration change: the field stays a
// String everywhere (Room, Firestore, the cloud function). This file provides
// the read-only renderer plus the authoring helper. Kept link-only (no other
// Markdown) so a stray `#`/`*`/`_` in normal text never renders as formatting.

// A Markdown link `[label](url)` OR a bare http(s) URL.
private val TOKEN = Regex("""\[([^\]\n]+)]\((https?://[^\s)]+)\)|(https?://[^\s<]+)""")

// Trailing prose punctuation that shouldn't be swallowed into a bare URL.
private val TRAILING = Regex("""[.,;:!?)]+$""")

/** Build an [AnnotatedString] where Markdown links and bare URLs are clickable. */
fun buildLinkedString(text: String, linkColor: Color): AnnotatedString {
    val linkStyles = TextLinkStyles(
        style = SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline),
    )
    return buildAnnotatedString {
        var last = 0
        for (m in TOKEN.findAll(text)) {
            if (m.range.first > last) append(text.substring(last, m.range.first))
            val bare = m.groupValues[3]
            val label: String
            val url: String
            var trailing = ""
            if (bare.isNotEmpty()) {
                val stripped = bare.replace(TRAILING, "")
                trailing = bare.substring(stripped.length)
                url = stripped
                label = stripped
            } else {
                label = m.groupValues[1]
                url = m.groupValues[2]
            }
            withLink(LinkAnnotation.Url(url, linkStyles)) { append(label) }
            if (trailing.isNotEmpty()) append(trailing)
            last = m.range.last + 1
        }
        if (last < text.length) append(text.substring(last))
    }
}

/**
 * Text that renders Markdown links and bare URLs as clickable links. Drop-in
 * replacement for a plain [M3Text] on read-only surfaces (cards, checklist rows).
 */
@Composable
fun LinkedText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    linkColor: Color = MaterialTheme.colorScheme.primary,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
) {
    M3Text(
        text = buildLinkedString(text, linkColor),
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        overflow = overflow,
    )
}

/**
 * Insert a Markdown link at the current selection, using the selected text as the
 * label (falling back to the URL when nothing is selected). Returns the updated
 * [TextFieldValue] with the caret placed after the inserted link.
 */
fun insertMarkdownLink(value: TextFieldValue, label: String, url: String): TextFieldValue {
    val start = value.selection.min
    val end = value.selection.max
    val trimmedUrl = url.trim()
    val labelText = label.trim().ifBlank { trimmedUrl }
    val md = "[$labelText]($trimmedUrl)"
    val newText = value.text.substring(0, start) + md + value.text.substring(end)
    val caret = start + md.length
    return TextFieldValue(newText, androidx.compose.ui.text.TextRange(caret))
}
