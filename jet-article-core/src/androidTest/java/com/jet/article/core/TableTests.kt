package com.jet.article.core

import com.jet.article.core.ArticleContentTransformer
import com.jet.article.core.ArticleElement
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class TableTests {

    private lateinit var articleContentTransformer: ArticleContentTransformer

    @Before
    fun setup() {
        articleContentTransformer = ArticleContentTransformer()
    }

    @Test
    fun testSimpleTable() {
        val html = "<table><tr><td>Cell 1</td><td>Cell 2</td></tr><tr><td>Cell 3</td><td>Cell 4</td></tr></table>"
        val result = articleContentTransformer.transform(html, "")
        val contentElements = result.elements

        assertEquals(1, contentElements.size)
        val table = contentElements[0] as ArticleElement.ContentTable
        assertEquals(2, table.rows.size)

        // Row 1
        assertEquals(2, table.rows[0].cells.size)
        assertEquals("Cell 1", (table.rows[0].cells[0].content[0] as ArticleElement.Text).text.toString())
        assertEquals("Cell 2", (table.rows[0].cells[1].content[0] as ArticleElement.Text).text.toString())

        // Row 2
        assertEquals(2, table.rows[1].cells.size)
        assertEquals("Cell 3", (table.rows[1].cells[0].content[0] as ArticleElement.Text).text.toString())
        assertEquals("Cell 4", (table.rows[1].cells[1].content[0] as ArticleElement.Text).text.toString())
    }

    @Test
    fun testTableWithNestedContent() {
        val html = "<table><tr><td>Cell 1<b> with bold</b></td><td><img src='image.png'></td></tr></table>"
        val result = articleContentTransformer.transform(html, "")
        val contentElements = result.elements

        assertEquals(1, contentElements.size)
        val table = contentElements[0] as ArticleElement.ContentTable
        assertEquals(1, table.rows.size)
        assertEquals(2, table.rows[0].cells.size)

        // Cell 1
        val cell1Content = table.rows[0].cells[0].content
        assertEquals(1, cell1Content.size)
        val cell1Text = cell1Content[0] as ArticleElement.Text
        assertEquals("Cell 1 with bold", cell1Text.text.toString())
        // Further assertions could be made on the AnnotatedString's spans

        // Cell 2
        val cell2Content = table.rows[0].cells[1].content
        assertEquals(1, cell2Content.size)
        val cell2Image = cell2Content[0] as ArticleElement.Image
        assertEquals("image.png", cell2Image.url)
    }
}
