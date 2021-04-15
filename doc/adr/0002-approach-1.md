# 2. Approach 1

Date: 2021-03-15

## Status

Complete

## Context

* Start simple

## Decision

Develop a simple binary format:

### Header

* 1 byte `V` : Version number. Fixed at `0x01`
* 1 byte, `B` : number of books
* `B` bytes : one byte for each book, to indicate how many chapters are in that book. Total of these values is `C`
* `C` bytes : one byte for each chapter, to indicate how many verses there are in that chapter. (the longest is Ps 119
  at 176 verses)

In the KJV, this header should have a total of 1 + 1 + 66 + 1189 = 1257 bytes

### Lexicon

* 2 bytes: an integer indicating how many terms are in the lexicon
* An array of `NULL` terminated single-byte, case-sensitive ASCII strings, sorted by:
    * number of occurrences in the text in descending order
    * then by alphabetical order

### Text File

* No header
* For each verse:
    * Store a single unsigned byte indicate the number of tokens in the verse
    * Store tokens as indexes into the lexicon in a variable (1-2 byte) format
        * If the first byte has a high bit of 0, then the remaining 7 bits are used to address positions 0-127 in the
          lexicon
        * If the first byte has a high bit of 1, then use the current byte and following byte (32k values). This will
          work for the KVJ which has about `13600` distinct tokens.

## Consequences

* Likely will not make it under the 1MB goal without additional compression
* No features to directly support "fast" indexing

## Results

* Header data: 1257
* Lexicon data: 109,035
* Text file: 1,236,508
    * 628,773 single byte tokens
    * 288,316 double byte tokens
* Raw file: 1,345,543 bytes

| Compression | From Raw | Version 1 | Ratio |
| ------------| -------- | --------- | ----- | 
| Raw         | 4745446  | 1346800   | 28%   |
| bzip2 -9    | 1015776  | 860056    | 84%   |
| xz -9       | 1026220  | 819848    | 80%   |
| zpaq -m5    | 728933   | 737351    | 100.1%|

### Observations

* The raw file is now small enough to fit on a 1.44mb floppy (if that is a metric that even matters)
* Both `xz` and `bzip2` saw a surprising reduction in size. Considering the entire xz executable is only `71184` on my
  MBP, this puts the decompressor and data at under 900kb.

## Possible simple improvements

### Lexicon file

* Don't use `0x00` to terminate each string
    * Since we are dealing with US-ASCII only (which falls in the range of 32-126), we could set the high bit in the
      final character to `1` to indicate it is the last letter of a token. This could save about 13,600 bytes.
    * However, this would come at the expense of supporting other languages, most notably Spanish.
* Add some sorting by length, and prefix a group of tokens with the expected length
    * Ie: `0x01` followed by `0x41 (A) 0x61 (a) 0x49 (I) 0x4F (O)`, or `0x02` followed by `0x616E (an) 0x6F66 (of)`, etc
    * This would only work if all tokens were shorter than 32 bytes, unless we reserved one value to indicate a longer
      length, and would just add to the decoding complexity. Overall savings would be ~10,000 bytes
* Apply compression to the Lexicon
    * Using Huffman, approximately 644 bits of overhead, and reduced the overall size to about 59kb (about 50kb of savings), using a
      simple JS test (http://craftyspace.net/huffman/)
    * Using Deflate, 49,727 bytes (59kb savings)
    * Using LZMA, 43,786 bytes (65kb savings)

### Text file

* Using LZMA, the text data as it stands can be compressed to 774,723 bytes
* The next thing to explore will be splitting the text file into two - one for stopwords and a master file, containing
  all other tokens. One approach would be to use a fixed number of bits to refernece a stopword.
    * Theoretically there are 628,773 single bytes in version 1. 0-15 are used a total of `351,687` times. Now, at the
      current version these are not all stopwords, so the total might be a little lower. But if we can cut that in half,
      plus some overhead, that would be a savings of maybe 150kb. Combine that with 50kb of savings in the Lexicon file,
      that gains us almost 200kb of savings, and gets us down to about 1.15mb
    * Indexes 0-7 : 271035. 3-bit encoding: 101,638. Potential savings : 169,397
    * Indexes 0-15: 351687. 4-bit encoding: 175,844. Potential savings : 175,843
    * Indexes 0-31: 449227. 5-bit encoding: 280,766. Potential savings : 168,461
    * Indexes 0-63: 543285. 6-bit encoding: 407,463. Potential savings : 135,822
* Alternatively, the text file could be encoded using a huffman encoder. The top 3 terms could be encoded in 3 bits
each, the next 7 could be encoded in 4 bits

### Final Notes on Compression

If we were to compress the lexicon data and text data separately using the best compressor that is easily available
inside the JVM (LZMA), we would be left with:

* Header data: 1,257 bytes
* Lexicon data: 43,786 bytes
* Text file: 774,723 bytes
* Total: 819,766 bytes

Compressing the lexicon and text data inside this container format is actually slightly smaller - by 72 bytes -
than compressing the entire file using `xz -9` on the command line. And if we really want to save a few more bytes,
the header can be compressed independently from 1,257 down to 936 bytes, resulting in a file 393 bytes smaller
than `xz -9`.

This is still larger than the `zpaq` file by about 90kb, but can be decompressed much faster and with
a much smaller amount of memory.

On my Macbook, the 64-bit LZMA executable (which links to XZ) is `71,184` bytes. The sample application in this
repository is written using Kotlin, but I think it would be reasonable to say that the decompression code for
LZMA plus the code necessary to parse the file format I have proposed here could be written in a lower
level language and produce an executable in less than `100,000` bytes, or even less than `180,000` bytes,
so I think it is entirely possible that the decompression code plus data could fit in less than one million bytes.