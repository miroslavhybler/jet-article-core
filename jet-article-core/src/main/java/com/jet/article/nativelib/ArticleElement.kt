package com.jet.article.nativelib

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.IntSize


/**
 * @author Miroslav Hýbler <br>
 * created on 07.02.2026
 */
sealed class ArticleElement constructor(
    val sourceIndex: Int,
    val ids: List<String>,
) {

    val compositionKey: String = "$sourceIndex-${ids.joinToString(separator = "-")}"


    class Text constructor(
        sourceIndex: Int,
        val text: AnnotatedString,
        val style: TextStyle,
        val isTitle: Boolean = false,
        val tag: String? = null,
        val isCode: Boolean = false,
        ids: List<String>,
    ) : ArticleElement(
        sourceIndex = sourceIndex,
        ids = ids
    )


    class Image constructor(
        sourceIndex: Int,
        val url: String,
        val contentDescription: String?,
        val defaultSize: IntSize,
        ids: List<String>,
    ) : ArticleElement(
        sourceIndex = sourceIndex,
        ids = ids,
    )


    class Quote(
        sourceIndex: Int,
        val text: AnnotatedString,
        val style: TextStyle,
        ids: List<String>,
    ) : ArticleElement(
        sourceIndex = sourceIndex,
        ids = ids,
    )

    /**
     * Represents a list (ordered or unordered).
     * @param items The list of items, where each item is a list of ContentElements.
     *              This allows for nested content within a list item.
     * @param isOrdered True for an ordered list (<ol>), false for an unordered list (<ul>).
     */
    class ContentList(
        sourceIndex: Int,
        val items: List<List<ArticleElement>>,
        val isOrdered: Boolean,
        ids: List<String>,
    ) : ArticleElement(
        sourceIndex = sourceIndex,
        ids = ids
    )

    /**
     * Represents a table.
     * @param rows The list of rows in the table.
     */
    class ContentTable(
        sourceIndex: Int,
        val rows: List<TableRow>,
        ids: List<String>,
    ) : ArticleElement(
        sourceIndex = sourceIndex,
        ids = ids
    ) {
        /**
         * Represents a row in a table.
         * @param cells The list of cells in the row.
         */
        data class TableRow(val cells: List<TableCell>)

        /**
         * Represents a cell in a table.
         * @param content The list of ContentElements within the cell.
         */
        data class TableCell(val content: List<ArticleElement>)
    }

}
