package com.jet.article.core

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.IntSize
import java.net.URI
import java.net.URISyntaxException
import java.util.Stack
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextLinkStyles


private interface ContentBuilder {
    fun addElement(element: ArticleElement)
    fun build(): List<ArticleElement>
}

private class RootBuilder : ContentBuilder {
    private val mElements = mutableListOf<ArticleElement>()
    override fun addElement(element: ArticleElement) {
        mElements.add(element)
    }

    override fun build(): List<ArticleElement> = mElements
}

private class ListBuilder(val isOrdered: Boolean, val ids: List<String>, val sourceIndex: Int) :
    ContentBuilder {
    private val mItems = mutableListOf<List<ArticleElement>>()
    override fun addElement(element: ArticleElement) {
        // This builder should only receive lists of elements representing an 'li'
        if (element is ArticleElement.Text || element is ArticleElement.Image || element is ArticleElement.ContentList) {
            mItems.add(listOf(element))
        }
    }

    fun addItemList(item: List<ArticleElement>) {
        mItems.add(item)
    }

    override fun build(): List<ArticleElement> {
        val allIds = ids.toMutableList()
        mItems.forEach { row ->
            row.forEach { cell ->
                allIds.addAll(cell.ids)
            }
        }
        return listOf(
            ArticleElement.ContentList(
                sourceIndex = sourceIndex,
                items = mItems,
                isOrdered = isOrdered,
                ids = allIds.distinct()
            )
        )
    }
}


private class ListItemBuilder constructor(
    val ids: List<String>
) : ContentBuilder {
    private val mContent = mutableListOf<ArticleElement>()
    override fun addElement(element: ArticleElement) {
        mContent.add(element = element)
    }

    override fun build(): List<ArticleElement> = mContent
}


private class TableBuilder(
    val ids: List<String>,
    val sourceIndex: Int,
) : ContentBuilder {
    private val mRows = mutableListOf<ArticleElement.ContentTable.TableRow>()
    private var mCurrentRow = mutableListOf<ArticleElement.ContentTable.TableCell>()
    private var mCurrentCell = mutableListOf<ArticleElement>()

    fun startNewRow() {
        if (mCurrentCell.isNotEmpty()) {
            mCurrentRow.add(element = ArticleElement.ContentTable.TableCell(content = mCurrentCell))
        }
        if (mCurrentRow.isNotEmpty()) {
            mRows.add(element = ArticleElement.ContentTable.TableRow(cells = mCurrentRow))
        }
        mCurrentRow = mutableListOf()
        mCurrentCell = mutableListOf()
    }

    fun startNewCell() {
        if (mCurrentCell.isNotEmpty()) {
            mCurrentRow.add(element = ArticleElement.ContentTable.TableCell(content = mCurrentCell))
        }
        mCurrentCell = mutableListOf()
    }

    override fun addElement(element: ArticleElement) {
        mCurrentCell.add(element = element)
    }

    override fun build(): List<ArticleElement> {
        if (mCurrentCell.isNotEmpty()) {
            mCurrentRow.add(element = ArticleElement.ContentTable.TableCell(content = mCurrentCell))
        }
        if (mCurrentRow.isNotEmpty()) {
            mRows.add(element = ArticleElement.ContentTable.TableRow(cells = mCurrentRow))
        }
        val allIds = ids.toMutableList()
        mRows.forEach { row ->
            row.cells.forEach { cell ->
                cell.content.forEach {
                    allIds.addAll(elements = it.ids)
                }
            }
        }
        return listOf(
            ArticleElement.ContentTable(
                rows = mRows,
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
    private var mBaseUrl: String? = null

    private val mIgnoredTags = setOf(
        "noscript", "script", "svg", "button", "input", "style"
    )


    fun transform(
        html: String,
        url: String,
        customTagFilter: ((tagName: String, attributes: Map<String, String>) -> Boolean)? = null,
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
        mBaseUrl = url

        mJetArticleNativeLib.parse(html = html, callback = this)
        flushText()

        val rootElements = mBuilderStack.pop().build()
        return ArticleData(
            url = url,
            headData = ArticleHeadData(title = mTitle),
            elements = rootElements,
        )
    }

    override fun onStartTag(
        tagName: String,
        attributes: Map<String, String>
    ) {
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
            "a" -> {
                mStyleStack.push(SpanStyle(textDecoration = TextDecoration.Underline))
                attributes["href"]?.let { href ->
                    val link = getLink(rawLink = href, articleUrl = mBaseUrl)
                    val customUrl = when (link) {
                        is Link.SameDomainLink -> "jetarticle://same-domain?url=${link.fullLink}"
                        is Link.OtherDomainLink -> "jetarticle://other-domain?url=${link.fullLink}"
                        is Link.SectionLink -> "jetarticle://section?id=${
                            link.fullLink.removePrefix(
                                "#"
                            )
                        }"

                        is Link.UriLink -> "jetarticle://uri?uri=${link.fullLink}"
                    }
                    //Just pust the annotation now, it updated later with styles and listener in Text.kt in UI module
                    mAnnotatedStringBuilder.pushLink(
                        link = LinkAnnotation.Clickable(
                            tag = customUrl,
                            styles = TextLinkStyles(),
                            linkInteractionListener = null,
                        )
                    )
                }
            }

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
            "a" -> {
                if (mStyleStack.isNotEmpty()) {
                    mStyleStack.pop()
                }
                try {
                    mAnnotatedStringBuilder.pop()
                } catch (e: IllegalStateException) {
                    // This can happen for an <a> tag without an href attribute.
                    // In this case, no link is pushed, so popping would cause a crash.
                }
            }

            "b", "strong", "i", "em", "u", "address" -> {
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

private fun resolveUrl(baseUrl: String?, link: String): String {
    if (baseUrl == null) return link
    val baseUri = URI.create(baseUrl)
    val resolvedUri = baseUri.resolve(link)
    return resolvedUri.toString()
}

private fun AnnotatedString.trimWhitespace(): AnnotatedString {
    if (text.isEmpty()) return this
    val first = text.indexOfFirst { !it.isWhitespace() }
    if (first == -1) return AnnotatedString("")
    val last = text.indexOfLast { !it.isWhitespace() }
    return this.subSequence(first, last + 1)
}

@OptIn(ExperimentalTextApi::class)
private fun AnnotatedString.collapseWhitespace(): AnnotatedString {
    val builder = AnnotatedString.Builder()
    var i = 0
    while (i < this.length) {
        if (this.text[i].isWhitespace()) {
            val whitespaceStart = i
            while (i + 1 < this.length && this.text[i + 1].isWhitespace()) {
                i++
            }
            // Now 'i' is the last character of the whitespace block

            val singleSpaceAnnotatedBuilder = AnnotatedString.Builder()
            singleSpaceAnnotatedBuilder.append(" ")

            // Transfer span styles from the first character of the whitespace block
            this.spanStyles.filter {
                it.start <= whitespaceStart && it.end > whitespaceStart
            }.forEach { spanRange ->
                singleSpaceAnnotatedBuilder.addStyle(spanRange.item, 0, 1)
            }

            // Transfer link annotations from the first character of the whitespace block
            this.getLinkAnnotations(whitespaceStart, whitespaceStart + 1).forEach { linkRange ->
                when (val item = linkRange.item) {
                    is LinkAnnotation.Url -> singleSpaceAnnotatedBuilder.addLink(item, 0, 1)
                    is LinkAnnotation.Clickable -> singleSpaceAnnotatedBuilder.addLink(item, 0, 1)
                }
            }

            builder.append(singleSpaceAnnotatedBuilder.toAnnotatedString())

        } else {
            val start = i
            while (i + 1 < this.length && !this.text[i + 1].isWhitespace()) {
                i++
            }
            builder.append(this.subSequence(startIndex = start, endIndex = i + 1))
        }
        i++
    }
    return builder.toAnnotatedString()
}

private fun getLink(
    rawLink: String,
    articleUrl: String?,
): Link {
    if (rawLink.startsWith(prefix = "#")) {
        return Link.SectionLink(rawLink = rawLink, fullLink = rawLink)
    }

    if (rawLink.startsWith(prefix = "mailto:") || rawLink.startsWith(prefix = "tel:")) {
        return Link.UriLink(rawLink = rawLink, fullLink = rawLink)
    }

    val fullLink = validateLink(
        rawLink = rawLink,
        articleUrl = articleUrl
    )

    val mDomain = try {
        articleUrl?.toDomainName()
    } catch (e: URISyntaxException) {
        null
    }
    val linkDomain = try {
        fullLink.toDomainName()
    } catch (e: URISyntaxException) {
        null
    }

    if (
        (mDomain != null && linkDomain != null)
        && mDomain == linkDomain
    ) {
        //Must be link within same domain
        return Link.SameDomainLink(rawLink = rawLink, fullLink = fullLink)
    }

    return Link.OtherDomainLink(rawLink = rawLink, fullLink = fullLink)
}

private fun validateLink(rawLink: String, articleUrl: String?): String {
    if (articleUrl == null) return rawLink
    val baseUri = try {
        URI(articleUrl)
    } catch (e: URISyntaxException) {
        return rawLink
    }
    val resolvedUri = baseUri.resolve(rawLink)
    return resolvedUri.toString()
}
