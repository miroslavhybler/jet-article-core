package com.jet.article.nativelib

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ListsTests {

    private lateinit var articleContentTransformer: ArticleContentTransformer

    @Before
    fun setup() {
        articleContentTransformer = ArticleContentTransformer()
    }

    @Test
    fun testUnorderedList() {
        val html = "<ul><li>Item 1</li><li>Item 2</li><li>Item 3</li></ul>"
        val result = articleContentTransformer.transform(html, "")
        val contentElements = result.elements

        assertEquals(1, contentElements.size)
        val list = contentElements[0] as ArticleElement.ContentList
        assertEquals(false, list.isOrdered)
        assertEquals(3, list.items.size)
        assertEquals("Item 1", (list.items[0][0] as ArticleElement.Text).text.toString())
        assertEquals("Item 2", (list.items[1][0] as ArticleElement.Text).text.toString())
        assertEquals("Item 3", (list.items[2][0] as ArticleElement.Text).text.toString())
    }

    @Test
    fun testOrderedList() {
        val html = "<ol><li>First</li><li>Second</li></ol>"
        val result = articleContentTransformer.transform(html, "")
        val contentElements = result.elements

        assertEquals(1, contentElements.size)
        val list = contentElements[0] as ArticleElement.ContentList
        assertEquals(true, list.isOrdered)
        assertEquals(2, list.items.size)
        assertEquals("First", (list.items[0][0] as ArticleElement.Text).text.toString())
        assertEquals("Second", (list.items[1][0] as ArticleElement.Text).text.toString())
    }

    @Test
    fun testNestedList() {
        val html = "<ul><li>Item 1</li><li>Item 2<ul><li>Nested 1</li><li>Nested 2</li></ul></li></ul>"
        val result = articleContentTransformer.transform(html, "")
        val contentElements = result.elements

        assertEquals(1, contentElements.size)
        val outerList = contentElements[0] as ArticleElement.ContentList
        assertEquals(2, outerList.items.size)

        // First item of outer list
        assertEquals(1, outerList.items[0].size)
        assertEquals("Item 1", (outerList.items[0][0] as ArticleElement.Text).text.toString())

        // Second item of outer list (contains text and a nested list)
        assertEquals(2, outerList.items[1].size)
        assertEquals("Item 2", (outerList.items[1][0] as ArticleElement.Text).text.toString())
        val nestedList = outerList.items[1][1] as ArticleElement.ContentList
        assertEquals(2, nestedList.items.size)
        assertEquals("Nested 1", (nestedList.items[0][0] as ArticleElement.Text).text.toString())
        assertEquals("Nested 2", (nestedList.items[1][0] as ArticleElement.Text).text.toString())
    }
}
