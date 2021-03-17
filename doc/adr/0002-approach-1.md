# 2. Approach 1

Date: 2021-03-15

## Status

Accepted

## Context

* Start simple

## Decision

Develop a simple binary format:

### Header

* No header at this time. The start position of the text file can be determined by reading past the Lexicon

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
    * If the first byte has a high bit of 1, then use the current byte and following byte (32k values). This will work
      for the KVJ which has about `13600` distinct tokens.

## Consequences

* Likely will not make it under the 1MB goal
* No features to directly support "fast" indexing

## Results

* Lexicon data: 109,035
* Text file: 1,236,508
  * 628,773 single byte tokens 
  * 288,316 double byte tokens
* Raw file: 1,345,543 bytes

| Compression | Size | v
| ----------- | -----|
| `zpaq -m5`  | 736,310 |
| `xz -9`     | 818,884 |
| `bzip2 -9`  | 858,365 |
| `zip -9`    | 911,950 |
| `gzip -9`   | 912,105 |  
| Uncompressed | 1,345,543 |

* `zpaq -m5` comes in at about 3kb smaller than compressing the raw KJV (`739,407`)
* `bzip2 -9` is much improved, vs `993,406`
* `xz -9` is hugely improved as well, vs `1,048,616`

## Possible simple improvements

### Lexicon file

* Don't use `0x00` to terminate each string
  * Since we are dealing with US-ASCII only (which falls in the range of 32-126), we could set the high bit in the final character
    to `1` to indicate it is the last letter of a token. This could save about 13,600 bytes. 
  * However, this would come at the expense of supporting other languages, most notably Spanish.
* Add some sorting by length, and prefix a group of tokens with the expected length
  * Ie: `0x01` followed by `0x41 (A) 0x61 (a) 0x49 (I) 0x4F (O)`, or `0x02` followed by `0x616E (an) 0x6F66 (of)`, etc
  * This would only work if all tokens were shorter than 32 bytes, unless we reserved one value to indicate a longer length,
  and would just add to the decoding complexity. Overall savings would be ~10,000 bytes
* Apply Huffman encoding to the Lexicon
  * Approximately 644 bits of overhead, and reduced the overall size to about 59kb (about 50kb of savings), using
  a simple JS test (http://craftyspace.net/huffman/)

### Text file

* The next thing to explore will be splitting the text file into two - one for stopwords, which can be encoded in 4 bits,
  and a master file, containing all other tokens
  * Theoretically there are 628,773 single bytes in this version. 0-15 are used a total of `351,687` times. Now,
  at the current version these are not all stopwords, so the total might be a little lower. But if we can cut that
    in half, plus some overhead, that would be a savings of maybe 150kb. Combine that with 50kb of savings in the Lexicon
    file, that gains us almost 200kb of savings, and gets us down to about 1.15mb
  * Indexes 0-7 : 271035. 3-bit encoding: 101,638. Potential savings : 169,397
  * Indexes 0-15: 351687. 4-bit encoding: 175,844. Potential savings : 175,843
  * Indexes 0-31: 449227. 5-bit encoding: 280,766. Potential savings : 168,461
  * Indexes 0-63: 543285. 6-bit encoding: 407,463. Potential savings : 135,822