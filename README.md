## BibCompact

> Through publication, mankind transmits and therefore preserves its knowledge, culture, and faith.
>
> The ongoing development of new publication media is interwoven with the progress of
> civilization - contributing to it or the result of it. The two are intimately related.
>
> The first images in a new medium are frequently of older and well-established knowledge.
> At a minimum, they are just facsimiles of older representations, rendered in the new medium
> but lacking its richness and leaving vacant its added dimensions. Radio programming can be
> played on television, color photography can reproduce black and white images, printed books
> can simply record a spoken dialog, and electronic books can simply project the printed page
> onto a display screen - but these are merely the first timid steps in the development and
> nurturing of a new medium. Much more is possible and, in fact, inevitable.
>
> Some important possibilities are realized with this first publication of the Holy Bible as a
> hand-held electronic reference. It goes beyond a simple electronic "printing" and begins to
> truly utilize our new medium of micro-components, algorithms, and mathematical data structures.
>
> The reader enjoys an unprecedented "view" of the Bible. Queries never before practical are
> satisfied instantly. Interrelationships and structure never before visible are easily discovered.
>
> It seems to me that these improvements are very much in keeping with the historical tradition
> that has made the Bible increasingly accessible - and perhaps again the vanguard of a new medium.
>
> -- KJ-21 Instruction Manual, Peter N. Yianilos, 1989

This project is part of
an [ongoing investigation](https://danwatt.org/2021/04/studying-an-old-e-reader-for-fun-certainly-not-profit/)
into a device made in 1989, a handheld Bible reader that managed to compress 4.3MB of text into less than 1MB, while
providing for very fast searching.

This project's goal is to determine how it might be possible to accomplish that feat using techniques that should have
been possible with the technology of the time, while at the same time comparing the results to more modern compression
and search tools.

## Works in progress

* Version 1 : Encode all words into the lexicon, then reference the lexicon with variable-width integer lookups
* Version 2 : Encode the lexicon using Huffman codes, then separately encode the integer lookups using Huffman codes
* Version 3 : (WIP) Encode the lexicon using a prefix tree / trie, continue encoding integer lookups using Huffman codes

## Work in progress stats

| Compression          | Size    | Ratio   |
|----------------------|---------|---------|
| zpaq -m5             | 739407  | 16.682% |
| bibcompact V1 + LZMA | 819766  | 18.494% |
| bzip2 -9             | 993406  | 22.412% |
| lzma -9              | 1048408 | 23.653% |
| xz -9                | 1048616 | 23.658% |
| 7z -mx9              | 1048710 | 23.660% |
| bibcompact V2 (HUFF) | 1062154 | 23.963% |
| zstd –ultra -22      | 1068137 | 24.099% |
| rar -m5              | 1142360 | 25.773% |
| bibcompact V1        | 1346800 | 30.386% | 
| gzip -9              | 1385457 | 31.258% |
| zip -9               | 1385595 | 31.261% |
| lz4 -9               | 1596418 | 36.017% |
| lzop -9              | 1611939 | 36.367% |
| Uncompressed         | 4432375 | 100%    |

## What we know:

* Uncompressed and without any special encoding, the ASCII text of the KJV is approximately 4.3 MB.
* The Franklin Electronics KJ-21 had 1.125 MB of memory, and had not only the text of the KJV in what appears to be a
  case-sensitive format, but also had a "file format" that enabled relative fast full-text searching
* The KJV, along with application code and a couple small games was able to fit on a 1MB Gameboy cartridge.
* The best text compressor to date (`zpaq`) can squeeze the KJV down to about 740kb, however it is extremely memory and
  CPU intensive to compress and decompress this much text.
* The KJ-21 also had a built-in thesaurus and word inflection database, mapping relationships between words that had
  similar meanings.

## Design Considerations

* Seeking to a specific point by reference should be very fast (under a second on a 8086 class CPU). Seeking to the
  final verse in Revelations should be close to, if not as fast as, seeking to the first verse in Genesis.
* Full text searching should be possible, and may be slower than seeking by reference (1-10 seconds on a 8086 class CPU)
    * Full text searches by a known prefix are allowed (ie: `word.*`), but not with a known suffix (`.*ord`)
* Assume that there is a reasonable amount of RAM available for decompression / decoding, up to maybe 2-4X the amount of
  memory required to hold the screen-visible text.
* Total storage of all structure - text, indexes, features to speed up full text searches, etc - should be under 1MB.
    * If possible just the text, after any encoding, should be smaller than what is possible with `zpaq`.
* We are only concerned with English text, and only a subset of the ASCII character set. However, if an approach can be
  made that works for other languages, that would be great.
* We should allow for arbitrary capitalization (ie: not just capitalizing the first word of a verse, or the first word
  after a period.)
* The code for decoding, reading and searching should be rather simple - think a few Kb of machine code.
* We may be able to safely omit newlines. Though print Bibles usually attempt to preseve a form of a paragraph
  structure, most of thee time the resolution we care about is at the verse level.
* Devices like the KJ-21 likely did not use LZW, as it was patented from 1983-2003.
* It is assumed that there will always be one space between each token (word). However, punctuation tokens should not
  have a space before, only after.

### Some statistics

* The KJV (OT+NT) has a total of about 790,000 words
* If treat a word as a symbol, and we add individual instances of punctuation ( `.,:;-"?!`, etc), there are about
  970,000 total symbols
* The KVJ has 66 books (8 bits), 1,189 chapters (11 bits), 31,102 verses (15 bits)
* The longest verse in the KJV is Esther 8:9 at:
    * 528 characters
    * 90 words case-sensitive, 51 distinct words
    * 104 tokens, including punctuation, 56 distinct.
* The longest chapter in the Bible is Psalms 119, at 176 verses (8 bits) and around 2,400 total words (12 bits).

Common stop words account for much of the text (case-insensitive):

```
Word  count verses
the	73,611	28,261
and	60,382	28,352
of	40,029	21,249
to	16,372	11,679
that	15,778	12,040
in	14,482	11,285
he	12,425	9,022
shall	10,939	6,796
for	10,889	8,678
unto	10,100	8,325
```

### Encoding words

If we could encode just the words of the Bible using a single byte each (which we can't), that would consume `789,626`
bytes of memory. If we encoded each English word inside a binary `word` (16 bits, which is possible since there are only
14,000 distinct English words) that would consume `1,579,252` bytes. That would not include punctuation or other
symbols. Even using a variable-length encoding (like UTF-8), we would expect the memory usage to be somewhere
between `789k` and `1,579k`.

### Storing the list of words

Even if we assigned a binary number to each word, we would need to store the English representation of that word
somewhere. That also does not take into account capitalization. Including variants due to capitalization, the 13,578
unique words require 108,829 bytes to store in US-ASCII. Sine we are only using the character set `[A-Za-z']` (53
characters, which would fit in 6 bits) we may be able to reduce this dictionary to about 82k. Running the ASCII through
a Huffman encoder yields about 59k.

### Fast Seeking

If we wanted to have an index to enable fast seeking to one of 31,102 verses, we would need to store a byte offset to
where in memory each verse started. Using our lower bound of 789,626 bytes of memory, we would need at least 20 bits to
form this address, or 21 with our upper bound of `1,579,252`. Rounding up to 24 bits, or 3 bytes, this would consume
`93,306 bytes`. This also does not take into account knowing how many verses are in each chapter and how many chapters
are in each book, which would also take some memory to encode. Using a naive approach, this would take close to 100kb of
memory to encode - close to 1/10th of our target memory usage.

We could instead store offsets in memory to starts of chapters. With 1,189 chapters, and 3 bytes necessary for memory
addresses, this would just consume `3,567 bytes`. We then would need to include verse markers, and assuming every verse
needed a marker in between them (let's assume this would be a single byte), this would consume an
additional `31,102 bytes`. In total, storing an offset to the start of a chapter with markers between verses, we would
use `34,669` bytes to enable relatively fast seeking.

Some additional optimizations:

1. If after all encoding the text can fit in 1MB or less (which is the goal), we can use 20-bit addressing for the
   chapter lookups. This takes us from `3,567 bytes` to `2,973 bytes` for our chapter offsets (`594 bytes` savings)
2. If we use an "end of verse" marker, and we know when in memory the next chapter (or book) starts, we can omit the
   final marker in a chapter at the expense of a little more code. This saves `1,189` markers (or bytes).
3. If we are applying Huffman (or similar) coding on a chapter-by-chapter basis, the end of verse marker could be
   encoded like any other symbol. Since we will need at most `31,102` of them, this marker is used more often than the
   word "to" and less than "of", and if we use some sort of variable-bit encoding, chances are the verse marker will
   consume fewer than 8 bits.

### Avenues to explore

* Better understand the "linking signal" used in patent 5,153,831
* N-grams (likely just 2 or 3)
    * Bi-grams
        * `, and` - 24,921
        * `of the` - 11,428
        * `the LORD` - 5,962
        * `in the` - 4,879
    * Tri-grams
        * `, and the` - 2,438
        * `of the LORD` - 1,625
        * `the son of` - 1,289
        * `the children of` - 1,254
        * `out of the` - 794
* Huffman encoding, at the verse and chapter level
* Arithmetic coding?

### Resources

* [CSV and other formats of public domain Bibles](https://github.com/scrollmapper/bible_databases)
* [Huffman encoding](https://github.com/marvinjason/HuffmanCoding)
* [Hutter prize in compression:](https://en.wikipedia.org/wiki/Hutter_Prize)
* [Stop words in Lucene](https://github.com/apache/lucene-solr/blob/master/lucene/analysis/common/src/java/org/apache/lucene/analysis/en/EnglishAnalyzer.java#L46)
* [Other Bible links](https://hackathon.bible/data/)
* [Unlicensed Gameboy Bible ROM (for size comparison)](https://wowroms.com/en/roms/nintendo-gameboy/king-james-bible-usa-unl/9499.html)
    * [An analysis of the Gameboy ROM](https://toasters.rocks/king-james-bible/)
* [Some statistics on the Bible](https://www.artbible.info/concordance/)



