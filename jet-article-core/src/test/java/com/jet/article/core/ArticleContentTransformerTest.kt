package com.jet.article.core

import org.junit.Assert.assertEquals
import org.junit.Test

class ArticleContentTransformerTest {

    @Test
    fun getClasses() {
        // Test with a single class
        val singleClass = mapOf("class" to "first")
        assertEquals(listOf("first"), ArticleContentTransformer.getClasses(singleClass))

        // Test with multiple classes
        val multipleClasses = mapOf("class" to "first second third")
        assertEquals(
            listOf("first", "second", "third"),
            ArticleContentTransformer.getClasses(multipleClasses)
        )

        // Test with extra spaces
        val extraSpaces = mapOf("class" to "  first   second  ")
        assertEquals(listOf("first", "second"), ArticleContentTransformer.getClasses(extraSpaces))

        // Test with no class attribute
        val noClass = emptyMap<String, String>()
        assertEquals(emptyList<String>(), ArticleContentTransformer.getClasses(noClass))

        // Test with an empty class attribute
        val emptyClass = mapOf("class" to "")
        assertEquals(emptyList<String>(), ArticleContentTransformer.getClasses(emptyClass))

        // Test with a blank class attribute
        val blankClass = mapOf("class" to "   ")
        assertEquals(emptyList<String>(), ArticleContentTransformer.getClasses(blankClass))
    }
}
