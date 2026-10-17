package org.dazai.osmium.ui.reader

import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import org.readium.r2.navigator.Decoration

/**
 * This Decorator Style will display a tinted icon in the page margin to show that a highlight
 * has an associated note.
 */
@Parcelize
data class DecorationStyleAnnotationMark(@ColorInt val tint: Int, val isActive: Boolean = false) : Decoration.Style

