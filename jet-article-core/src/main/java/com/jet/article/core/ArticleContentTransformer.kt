package com.jet.article.core

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import java.util.Stack


private interface ContentBuilder {
    fun addElement(element: ArticleElement)
    fun build(): List<ArticleElement>
}

private class RootBuilder : ContentBuilder {
    private val elements = mutableListOf<ArticleElement>()
    override fun addElement(element: ArticleElement) {
        elements.add(element)
    }

    override fun build(): List<ArticleElement> = elements
}

private class ListBuilder(val isOrdered: Boolean, val ids: List<String>, val sourceIndex: Int) : ContentBuilder {
    private val items = mutableListOf<List<ArticleElement>>()
    override fun addElement(element: ArticleElement) {
        // This builder should only receive lists of elements representing an 'li'
        if (element is ArticleElement.Text || element is ArticleElement.Image || element is ArticleElement.ContentList) {
            items.add(listOf(element))
        }
    }

    fun addItemList(item: List<ArticleElement>) {
        items.add(item)
    }

    override fun build(): List<ArticleElement> {
        val allIds = ids.toMutableList()
        items.forEach { row ->
            row.forEach { cell ->
                allIds.addAll(cell.ids)
            }
        }
        return listOf(
            ArticleElement.ContentList(
                sourceIndex = sourceIndex,
                items = items,
                isOrdered = isOrdered,
                ids = allIds.distinct()
            )
        )
    }
}


private class ListItemBuilder constructor(
    val ids: List<String>
) : ContentBuilder {
    private val content = mutableListOf<ArticleElement>()
    override fun addElement(element: ArticleElement) {
        content.add(element = element)
    }

    override fun build(): List<ArticleElement> = content
}


private class TableBuilder(
    val ids: List<String>,
    val sourceIndex: Int,
) : ContentBuilder {
    private val rows = mutableListOf<ArticleElement.ContentTable.TableRow>()
    private var currentRow = mutableListOf<ArticleElement.ContentTable.TableCell>()
    private var currentCell = mutableListOf<ArticleElement>()

    fun startNewRow() {
        if (currentCell.isNotEmpty()) {
            currentRow.add(element = ArticleElement.ContentTable.TableCell(content = currentCell))
        }
        if (currentRow.isNotEmpty()) {
            rows.add(element = ArticleElement.ContentTable.TableRow(cells = currentRow))
        }
        currentRow = mutableListOf()
        currentCell = mutableListOf()
    }

    fun startNewCell() {
        if (currentCell.isNotEmpty()) {
            currentRow.add(element = ArticleElement.ContentTable.TableCell(content = currentCell))
        }
        currentCell = mutableListOf()
    }

    override fun addElement(element: ArticleElement) {
        currentCell.add(element = element)
    }

    override fun build(): List<ArticleElement> {
        if (currentCell.isNotEmpty()) {
            currentRow.add(element = ArticleElement.ContentTable.TableCell(content = currentCell))
        }
        if (currentRow.isNotEmpty()) {
            rows.add(element = ArticleElement.ContentTable.TableRow(cells = currentRow))
        }
        val allIds = ids.toMutableList()
        rows.forEach { row ->
            row.cells.forEach { cell ->
                cell.content.forEach {
                    allIds.addAll(elements = it.ids)
                }
            }
        }
        return listOf(
            ArticleElement.ContentTable(
                rows = rows,
                ids = allIds.distinct(),
                sourceIndex = sourceIndex,
            )
        )
    }
}


// --- ContentTransformer ---

class ArticleContentTransformer() : TagCallback {

    companion object {
        fun getClasses(attributes: Map<String, String>): List<String> {
            return attributes["class"]?.split(" ")?.filter { it.isNotBlank() } ?: emptyList()
        }
    }

    private val mJetArticleNativeLib: JetArticleNativeLib = JetArticleNativeLib()
    private val mBuilderStack = Stack<ContentBuilder>()
    private var mAnnotatedStringBuilder = AnnotatedString.Builder()
    private val mStyleStack = Stack<SpanStyle>()
    private val mIdStack = Stack<String>()
    private val mTagStack = Stack<String>()
    private var mSourceIndexCounter = 0

    private var mIsInsidePreTag = false
    private var mIgnoredTagDepth = 0
    private var mIsInsideBody = false
    private var mIsInsideTitle = false
    private var mTitle: String? = null
    private var mIsInsideBlockquote = false
    private var mCustomTagFilter: ((tagName: String, attributes: Map<String, String>) -> Boolean)? =
        null

    private val mIgnoredTags = setOf(
        "noscript", "script", "svg", "button", "input", "form", "style"
    )


    fun transform(
        html: String,
        url: String,
        customTagFilter: ((tagName: String, attributes: Map<String, String>) -> Boolean)? = null
    ): ArticleData {
        // Reset all state
        mBuilderStack.clear()
        mBuilderStack.push(RootBuilder())
        mAnnotatedStringBuilder = AnnotatedString.Builder()
        mStyleStack.clear()
        mIdStack.clear()
        mTagStack.clear()
        mSourceIndexCounter = 0
        mIsInsidePreTag = false
        mIgnoredTagDepth = 0
        mIsInsideBody = false
        mIsInsideTitle = false
        mTitle = null
        mIsInsideBlockquote = false
        mCustomTagFilter = customTagFilter

        mJetArticleNativeLib.parse(html = html, callback = this)
        flushText()

        val rootElements = mBuilderStack.pop().build()
        return ArticleData(
            url = url,
            headData = ArticleHeadData(title = mTitle),
            elements = rootElements,
        )
    }

    override fun onStartTag(tagName: String, attributes: Map<String, String>) {
        val tag = tagName.lowercase()

        if (mIgnoredTagDepth > 0) {
            mIgnoredTagDepth++
            mIdStack.push("")
            mTagStack.push("")
            return
        }

        if (mIgnoredTags.contains(tag) || (mCustomTagFilter != null && !mCustomTagFilter!!(
                tag,
                attributes
            ))
        ) {
            mIgnoredTagDepth++
            mIdStack.push("")
            mTagStack.push("")
            return
        }


        if (tag == "body") mIsInsideBody = true
        if (tag == "title") mIsInsideTitle = true
        if (!mIsInsideBody && !mIsInsideTitle) return

        if (isBlockTag(tagName = tag)) {
            flushText()
        }

        mIdStack.push(attributes["id"] ?: "")
        mTagStack.push(tag)

        when (tag) {
            "ul", "ol" -> mBuilderStack.push(
                ListBuilder(
                    isOrdered = tag == "ol",
                    ids = mIdStack.filter(predicate = String::isNotEmpty).toList(),
                    sourceIndex = mSourceIndexCounter++,
                )
            )

            "li" -> mBuilderStack.push(
                ListItemBuilder(
                    ids = mIdStack.filter(predicate = String::isNotEmpty).toList()
                )
            )

            "table" -> mBuilderStack.push(
                TableBuilder(
                    ids = mIdStack.filter(predicate = String::isNotEmpty).toList(),
                    sourceIndex = mSourceIndexCounter++,
                )
            )

            "tr" -> (mBuilderStack.peek() as? TableBuilder)?.startNewRow()
            "td", "th" -> (mBuilderStack.peek() as? TableBuilder)?.startNewCell()
            "blockquote" -> mIsInsideBlockquote = true
            "pre" -> mIsInsidePreTag = true
            "b", "strong" -> mStyleStack.push(SpanStyle(fontWeight = FontWeight.Bold))
            "i", "em", "address" -> mStyleStack.push(SpanStyle(fontStyle = FontStyle.Italic))
            "u" -> mStyleStack.push(SpanStyle(textDecoration = TextDecoration.Underline))
            "a" -> mStyleStack.push(SpanStyle(textDecoration = TextDecoration.Underline))
            "q" -> mAnnotatedStringBuilder.append("\"")
            "img" -> {
                val src = attributes["src"] ?: ""
                val alt = attributes["alt"] ?: ""
                val width = attributes["width"]?.toIntOrNull() ?: 0
                val height = attributes["height"]?.toIntOrNull() ?: 0
                if (src.isNotEmpty()) {
                    mBuilderStack.peek().addElement(
                        ArticleElement.Image(
                            url = src,
                            contentDescription = alt,
                            ids = mIdStack.filter(predicate = String::isNotEmpty).toList(),
                            defaultSize = if (width > 0 && height > 0)
                                IntSize(width = width, height = height)
                            else IntSize.Zero,
                            sourceIndex = mSourceIndexCounter++,
                        )
                    )
                }
            }

            "br" -> mAnnotatedStringBuilder.append('\n')
        }
    }


    override fun onEndTag(tagName: String) {
        val tag = tagName.lowercase()
        if (mIgnoredTags.contains(tag) || (mCustomTagFilter != null && !mCustomTagFilter!!(
                tag,
                emptyMap()
            ))
        ) {
            if (mIgnoredTagDepth > 0) {
                mIgnoredTagDepth--
            }
            mIdStack.pop()
            mTagStack.pop()
            return
        }

        if (mIgnoredTagDepth > 0) {
            mIgnoredTagDepth--
            mIdStack.pop()
            mTagStack.pop()
            return
        }



        if (tag == "body") mIsInsideBody = false
        if (tag == "title") mIsInsideTitle = false

        if (!mIsInsideBody && !mIsInsideTitle) return

        if (isBlockTag(tagName = tag)) {
            flushText()
        }

        when (tag) {
            "li" -> {
                val itemBuilder = mBuilderStack.pop() as ListItemBuilder
                val listBuilder = mBuilderStack.peek() as? ListBuilder
                listBuilder?.addItemList(itemBuilder.build())
            }



            "ul", "ol", "table" -> {
                if (mBuilderStack.size > 1) { // Don't pop the root
                    val finishedBuilder = mBuilderStack.pop()
                    mBuilderStack.peek().addElement(finishedBuilder.build().first())
                }
            }

            "blockquote" -> mIsInsideBlockquote = false
            "pre" -> mIsInsidePreTag = false
            "q" -> mAnnotatedStringBuilder.append("\"")
            "b", "strong", "i", "em", "u", "a", "address" -> {
                if (mStyleStack.isNotEmpty()) {
                    mStyleStack.pop()
                }
            }
        }
        mIdStack.pop()
        mTagStack.pop()
    }

    override fun onText(content: String) {
        if (mIgnoredTagDepth > 0) return
        if (mIsInsideTitle) {
            mTitle = content
            return
        }
        if (!mIsInsideBody) return

        val combinedStyle = mStyleStack.fold(initial = SpanStyle()) { acc, style ->
            acc.merge(other = style)
        }
        mAnnotatedStringBuilder.pushStyle(combinedStyle)
        mAnnotatedStringBuilder.append(content)
        mAnnotatedStringBuilder.pop()
    }

    private fun isBlockTag(tagName: String): Boolean {
        return tagName in listOf(
            "p",
            "h1",
            "h2",
            "h3",
            "h4",
            "h5",
            "h6",
            "div",
            "img",
            "br",
            "pre",
            "blockquote",
            "li",
            "tr",
            "td",
            "th",
            "ul",
            "ol"
        )
    }

    private fun flushText() {
        if (mAnnotatedStringBuilder.length > 0) {
            val builtString = mAnnotatedStringBuilder.toAnnotatedString()
            val textToFlush: AnnotatedString = if (mIsInsidePreTag) {
                dedent(annotatedText = builtString)
            } else {
                builtString.collapseWhitespace().trimWhitespace()
            }

            if (textToFlush.isNotEmpty()) {
                val currentTag = if (mTagStack.isNotEmpty()) mTagStack.peek() else null
                val isTitle = currentTag in listOf("h1", "h2", "h3", "h4", "h5", "h6")
                val isCode = currentTag == "code" || currentTag == "pre"
                val element = when {
                    mIsInsideBlockquote -> ArticleElement.Quote(
                        text = textToFlush,
                        style = TextStyle.Default,
                        ids = mIdStack.filter(predicate = String::isNotEmpty).toList(),
                        sourceIndex = mSourceIndexCounter++,
                    )

                    else -> ArticleElement.Text(
                        text = textToFlush,
                        style = TextStyle.Default,
                        ids = mIdStack.filter(predicate = String::isNotEmpty).toList(),
                        isTitle = isTitle,
                        isCode = isCode,
                        tag = currentTag,
                        sourceIndex = mSourceIndexCounter++,
                    )
                }
                mBuilderStack.peek().addElement(element)
            }
            mAnnotatedStringBuilder = AnnotatedString.Builder()
        }
    }

    private fun dedent(annotatedText: AnnotatedString): AnnotatedString {
        val text = annotatedText.text
        if (text.isEmpty()) return annotatedText

        var lines = text.lines()
        if (lines.isEmpty()) return AnnotatedString("")

        // Don't dedent single-line text that doesn't contain a newline.
        if (lines.size == 1 && !text.contains('\n')) return annotatedText

        val firstContentLineIndex = lines.indexOfFirst { it.isNotBlank() }
        if (firstContentLineIndex == -1) return AnnotatedString("") // All lines are blank
        val lastContentLineIndex = lines.indexOfLast { it.isNotBlank() }

        val contentLines = lines.subList(firstContentLineIndex, lastContentLineIndex + 1)
        if (contentLines.isEmpty()) return AnnotatedString("")

        val minIndent = contentLines.filter { it.isNotBlank() }.minOfOrNull {
            it.takeWhile { char -> char.isWhitespace() }.length
        } ?: 0

        // If no dedenting is needed and no lines were trimmed, return the original.
        if (minIndent == 0 && firstContentLineIndex == 0 && lastContentLineIndex == lines.size - 1) {
            return annotatedText
        }

        val builder = AnnotatedString.Builder()

        // Find the start position in the original annotated string corresponding to the first content line.
        var currentPos = 0
        for (i in 0 until firstContentLineIndex) {
            currentPos += lines[i].length + 1 // +1 for the newline
        }

        for ((index, line) in contentLines.withIndex()) {
            val indentToRemove = minOf(minIndent, line.takeWhile { it.isWhitespace() }.length)
            val lineEnd = currentPos + line.length
            val lineContentStart = currentPos + indentToRemove

            if (lineContentStart < lineEnd) {
                // This subsequence preserves the styles from the original string.
                val originalSubSequence = annotatedText.subSequence(lineContentStart, lineEnd)
                builder.append(originalSubSequence)
            }

            // Re-add the newline if it's not the last line of our content block.
            if (index < contentLines.size - 1) {
                builder.append('\n')
            }

            currentPos += line.length + 1 // Move to the start of the next line in the original text
        }

        return builder.toAnnotatedString()
    }
}

private fun AnnotatedString.trimWhitespace(): AnnotatedString {
    if (text.isEmpty()) return this
    val first = text.indexOfFirst { !it.isWhitespace() }
    if (first == -1) return AnnotatedString("")
    val last = text.indexOfLast { !it.isWhitespace() }
    return this.subSequence(first, last + 1)
}

private fun AnnotatedString.collapseWhitespace(): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < this.length) {
        if (this.text[i].isWhitespace()) {
            while (i + 1 < this.length && this.text[i + 1].isWhitespace()) {
                i++
            }
            builder.append(" ")
        } else {
            val start = i
            while (i + 1 < this.length && !this.text[i + 1].isWhitespace()) {
                i++
            }
            builder.append(text = this.subSequence(startIndex = start, endIndex = i + 1))
        }
        i++
    }
    return builder.toAnnotatedString()
}
