package com.jet.article.core

import com.jet.article.core.ArticleContentTransformer
import com.jet.article.core.ArticleElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test

class CustomTagFilterTest {

    private lateinit var articleContentTransformer: ArticleContentTransformer

    @Before
    fun setup() {
        articleContentTransformer = ArticleContentTransformer()
    }

    @Test
    fun testFilterDivWithMenuClass() {
        val html = """
            <body>
                <h1>Welcome</h1>
                <div class="menu">
                    <a href="#">Home</a>
                    <a href="#">About</a>
                </div>
                <p>This is the main content.</p>
                <div class="footer">
                    <p>Copyright</p>
                </div>
            </body>
        """.trimIndent()

        val result = articleContentTransformer.transform(html, "") { tagName, attributes ->
            // Filter out (return false for) any div with the class "menu" or "footer"
            !(tagName == "div" && (attributes["class"] == "menu" || attributes["class"] == "footer"))
        }

        val contentElements = result.elements
        val contentAsText = contentElements.joinToString { (it as? ArticleElement.Text)?.text.toString() }

        assertEquals(2, contentElements.size)
        assertEquals("Welcome", (contentElements[0] as ArticleElement.Text).text.toString())
        assertEquals("This is the main content.", (contentElements[1] as ArticleElement.Text).text.toString())
        assertFalse(contentAsText.contains("Home"))
        assertFalse(contentAsText.contains("About"))
        assertFalse(contentAsText.contains("Copyright"))
    }
}
