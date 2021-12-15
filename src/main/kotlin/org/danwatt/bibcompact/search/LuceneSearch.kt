package org.danwatt.bibcompact.search

import org.apache.lucene.analysis.Analyzer
import org.apache.lucene.analysis.standard.StandardAnalyzer
import org.apache.lucene.document.*
import org.apache.lucene.index.IndexWriter
import org.apache.lucene.index.IndexWriterConfig
import org.apache.lucene.store.Directory
import org.apache.lucene.store.FSDirectory
import org.apache.lucene.store.RAMDirectory
import org.danwatt.bibcompact.TokenizedVerse
import org.danwatt.bibcompact.VerseTokenizer
import java.io.BufferedReader
import java.nio.file.Files


class LuceneSearch {
    fun buildIndex(verses: List<TokenizedVerse>) {
        val analyzer: Analyzer = StandardAnalyzer()
        val iwc = IndexWriterConfig(analyzer)
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        val tempDir = Files.createTempDirectory("/tmp")
        val dir: Directory = FSDirectory.open(tempDir.toAbsolutePath())
        val writer = IndexWriter(dir, iwc)
        val byBook: Map<Int, List<TokenizedVerse>> = verses.groupBy { it.book }
        byBook.forEach { book, verses ->
            val doc = Document()
            doc.add(IntPoint("book", book))
            val joined: String = verses.joinToString { it.tokens.joinToString { " " } }
            doc.add(TextField("contents", joined, Field.Store.NO))
            writer.addDocument(doc);
        }

    }
}