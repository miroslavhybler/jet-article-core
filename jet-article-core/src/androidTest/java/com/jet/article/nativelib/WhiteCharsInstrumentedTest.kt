package com.jet.article.nativelib

import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class WhiteCharsInstrumentedTest {

    private lateinit var articleContentTransformer: ArticleContentTransformer

    @Before
    fun setup() {
        articleContentTransformer = ArticleContentTransformer()
    }

    @Test
    fun spansTest() {
        val input =
            "<div><p>a <span> b</span><span> c </span> <span>d</span></p> e <span>f</span> <span>g</span><span> h</span><span> i </span><span> </span></div>"
        val expectedNodesTest: List<String> = listOf(
            "a b c d",
            "e f g h i"
        )
        val actualNodes = articleContentTransformer.transform(
            html = input,
            url = "https://www.example.com",
        ).elements
            .map { (it as ArticleElement.Text).text.toString() }
        assertEquals(expectedNodesTest, actualNodes)
    }

    @Test
    fun preTagTest() {
        val input = "<div>\n" +
                "    Test for pre tag\n" +
                "    <pre>\n" +
                "        a\n" +
                "        b\n" +
                "        c\n" +
                "    </pre>\n" +
                "\n" +
                "    Another test for pre tag\n" +
                "    <pre>  This  is  single  line  pre  with  double  spaces.  </pre>\n" +
                "</div>"

        // Expected: Text outside <pre> is trimmed, text inside <pre> is de-dented and preserves internal whitespace.
        val expectedNodesTest: List<String> = listOf(
            "Test for pre tag",
            "a\nb\nc",
            "Another test for pre tag",
            "  This  is  single  line  pre  with  double  spaces.  "
        )
        val actualNodes = articleContentTransformer.transform(
            html = input,
            url = "https://www.example.com",
        ).elements.map { (it as ArticleElement.Text).text.toString() }
        assertEquals(expectedNodesTest, actualNodes)
    }

    @Test
    fun blockElementsTest() {
        val input = "<div>\n" +
                "    a\n" +
                "    <p>\n" +
                "        b\n" +
                "    </p>\n" +
                "    <div>\n" +
                "        c\n" +
                "    </div>\n" +
                "    <div>\n" +
                "        <div>\n" +
                "            <span>d</span>\n" +
                "        </div>\n" +
                "    </div>\n" +
                "    <p>\n" +
                "        e\n" +
                "    <p>\n" +
                "    f\n" +
                "</p>\n" +
                "    </p>\n" +
                "</div>"

        val expectedNodesTest: List<String> = listOf(
            "a", "b", "c", "d", "e", "f"
        )
        val actualNodes =
            articleContentTransformer.transform(
                html = input,
                url = "https://www.example.com",
            ).elements.map { (it as ArticleElement.Text).text.toString() }
        assertEquals(expectedNodesTest, actualNodes)
    }
}
