package com.jet.article.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jet.article.core.ArticleContentTransformer
import com.jet.article.core.ArticleElement
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HtmlParserInstrumentedTest {

    private lateinit var articleContentTransformer: ArticleContentTransformer

    @Before
    fun setup() {
        articleContentTransformer = ArticleContentTransformer()
    }

    @Test
    fun testTransformSimpleHtml() {
        val html = """
            <h1>A Big Heading</h1>
            <p>This is a paragraph with <b>bold text</b> and a <a href="#">link</a>.</p>
            <img src="image.jpg" alt="Test Image" id="img1">
            <p>Another paragraph with <i>italic</i> text.</p>
        """.trimIndent()

        val contentElements = articleContentTransformer.transform(
            html = html,
            url = "https://www.example.com",
        ).elements

        assertEquals(4, contentElements.size)

        // Check Heading
        val heading = contentElements[0] as ArticleElement.Text
        assertEquals("A Big Heading", heading.text.text.trim())

        // Check first paragraph
        val paragraph1 = contentElements[1] as ArticleElement.Text
        val expectedParagraph1Text = "This is a paragraph with bold text and a link."
        assertEquals(expectedParagraph1Text, paragraph1.text.text.trim())
        // We will add more detailed span verification later.

        // Check Image
        val image = contentElements[2] as ArticleElement.Image
        assertEquals("image.jpg", image.url)
        assertEquals("Test Image", image.contentDescription)

        // Check second paragraph
        val paragraph2 = contentElements[3] as ArticleElement.Text
        assertEquals("Another paragraph with italic text.", paragraph2.text.text.trim())
    }

    @Test
    fun testWhitespaceAndNestedTags() {
        val html = "<p>  Some <b> nested <i> italic and bold </i></b> text. </p>"
        val contentElements = articleContentTransformer.transform(
            html = html,
            url = "https://www.example.com",
        ).elements

        assertEquals(1, contentElements.size)
        val textElement = contentElements[0] as ArticleElement.Text

        val expectedText = "Some nested italic and bold text."
        assertEquals(expectedText, textElement.text.text.trim())
        // We will add more detailed span verification for nested styles later.
    }

    @Test
    fun testSourceIndexAssignment() {
        val html = """
            <h1>First</h1>
            <p>Second</p>
            <img src="third.jpg" alt="Third">
            <blockquote>Fourth</blockquote>
            <ul><li>Fifth</li></ul>
        """.trimIndent()

        val contentElements = articleContentTransformer.transform(
            html = html,
            url = "https://www.example.com",
        ).elements

        assertEquals(5, contentElements.size)
        assertTrue(contentElements[0].sourceIndex == 0)
        assertTrue(contentElements[1].sourceIndex == 1)
        assertTrue(contentElements[2].sourceIndex == 2)
        assertTrue(contentElements[3].sourceIndex == 3)
        assertTrue(contentElements[4].sourceIndex == 4)
    }
}
