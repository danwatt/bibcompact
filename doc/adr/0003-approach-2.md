# 2. Approach 1

Date: 2021-03-15

## Status

Proposed

## Context

* Buildd on approach 1

## Decision


### Header
* 1 byte `V` : Version number. Fixed at `0x01`
* 1 byte, `B` : number of books
* `B` bytes : one byte for each book, to indicate how many chapters are in that book. Total of these values is `C`
* `C` bytes : one byte for each chapter, to indicate how many verses there are in that chapter. (the longest is Ps 119
  at 176 verses)

In the KJV, this header should have a total of 1 + 1 + 66 + 1189 = 1257 bytes

### Lexicon

Introduce the concept of a stop-word. At least for the KJV, we are going to use a fixed array of stop-words
that are case-sensitive for the time being. This list will be handcrafted, but based on
https://gist.github.com/sebleier/554280 with some changes to account for KVJ-isms.

The Lexicon will be split into two lists - the stop word list, and the significant word list.

* 1 byte `S`: an integer indicating how many terms are in the stop word list
* 2 bytes `L`: an integer indicating how many terms are in the significant word list
* An array of `S` elements with `NULL` terminated single-byte, case-sensitive ASCII strings from the stop-word list
* An array of `L` elements of `NULL` terminated single-byte, case-sensitive ASCII strings, sorted by:
    * number of occurrences in the text in descending order
    * then by alphabetical order

### Text File

The text file now will be separated by book, and then further into three streams:

1. A bit array indicating if the word in this position is in the skip stream or significant stream
2. A stream of single byte integer lookups into the stop word list
3. A stream of variable byte (1-2) integer lookups into the significant word list

For example, if we have the following sentence:

> This is a test of split streams.

We have 8 tokens:
| Token | Type | Index |
| ----- | ---- | ----- |
| This  | stop          | 10 |
| is    | stop          | 5  |
| a     | stop          | 0  |
| test  | significant   | 20 |
| of    | stop          | 3  |
| split | significant   | 50 |
| streams | significant | 59 |
| .     | stop          | 1  |

* Stream 1: `00010110` (or `0x16`)
* Stream 2: `0x0A 05 00 03 01`
* Stream 3: `0x14 32 3b`

This actually introduces one extra byte of overhead for our sample sentence. We can compensate for this later on
when we introduce a low-overhead compressor to the stream.

## Consequences

* Likely will not make it under the 1MB goal
* No features to directly support "fast" indexing

## Results

* Lexicon data: 109,035
* Text file: 1,236,508
    * 628,773 single byte tokens
    * 288,316 double byte tokens
* Raw file: 1,345,543 bytes

| Compression | Size | v | ----------- | -----| | `zpaq -m5`  | 736,310 | | `xz -9`     | 818,884 | | `bzip2 -9`  |
858,365 | | `zip -9`    | 911,950 | | `gzip -9`   | 912,105 |  
| Uncompressed | 1,345,543 |

* `zpaq -m5` comes in at about 3kb smaller than compressing the raw KJV (`739,407`)
* `bzip2 -9` is much improved, vs `993,406`
* `xz -9` is hugely improved as well, vs `1,048,616`

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
* Apply Huffman encoding to the Lexicon
    * Approximately 644 bits of overhead, and reduced the overall size to about 59kb (about 50kb of savings), using a
      simple JS test (http://craftyspace.net/huffman/)

### Text file

* The next thing to explore will be splitting the text file into two - one for stopwords, which can be encoded in 4
  bits, and a master file, containing all other tokens
    * Theoretically there are 628,773 single bytes in this version. 0-15 are used a total of `351,687` times. Now, at
      the current version these are not all stopwords, so the total might be a little lower. But if we can cut that in
      half, plus some overhead, that would be a savings of maybe 150kb. Combine that with 50kb of savings in the Lexicon
      file, that gains us almost 200kb of savings, and gets us down to about 1.15mb
    * Indexes 0-7 : 271035. 3-bit encoding: 101,638. Potential savings : 169,397
    * Indexes 0-15: 351687. 4-bit encoding: 175,844. Potential savings : 175,843
    * Indexes 0-31: 449227. 5-bit encoding: 280,766. Potential savings : 168,461
    * Indexes 0-63: 543285. 6-bit encoding: 407,463. Potential savings : 135,822