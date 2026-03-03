package com.jet.article.core

import com.jet.article.core.ArticleContentTransformer
import org.junit.Assert
import org.junit.Test

class IdExtractionTest {


    @Test
    fun testWithoutFilter() {
        val transformer = ArticleContentTransformer()
        val html = """
            <html>
                <head>
                    <title>Test</title>
                </head>
                <body>
                    <div id="div1">
                        <p id="p1">Hello</p>
                        <p id="p2">World</p>
                    </div>
                    <img id="img1" src="image.jpg" />
                </body>
            </html>
        """.trimIndent()

        val result = transformer.transform(html, "http://example.com")

        Assert.assertEquals(3, result.elements.size)
        Assert.assertEquals(
            listOf("div1", "p1"),
            result.elements[0].ids.filter { it.isNotEmpty() }
        )
        Assert.assertEquals(
            listOf("div1", "p2"),
            result.elements[1].ids.filter { it.isNotEmpty() }
        )
        Assert.assertEquals(
            listOf("img1"),
            result.elements[2].ids.filter { it.isNotEmpty() }
        )
    }

    @Test
    fun testWithFilter() {
        val transformer = ArticleContentTransformer()
        val html = """
            <html>
                <head>
                    <title>Test</title>
                </head>
                <body>
                    <div id="div1">
                        <p id="p1">Hello</p>
                        <p id="p2">World</p>
                        <custom-tag id="custom1">
                            <p id="p3">Ignored text</p>
                        </custom-tag>
                    </div>
                </body>
            </html>
        """.trimIndent()

        val result = transformer.transform(
            html = html,
            url = "http://example.com",
        ) { tagName, _ ->
            tagName != "custom-tag"
        }

        Assert.assertEquals(
            2,
            result.elements.size
        )
        Assert.assertEquals(
            listOf("div1", "p1"),
            result.elements[0].ids.filter { it.isNotEmpty() }
        )
        Assert.assertEquals(
            listOf("div1", "p2"),
            result.elements[1].ids.filter { it.isNotEmpty() }
        )
    }
}
