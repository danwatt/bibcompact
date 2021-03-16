# 2. Approach 2

Date: 2021-03-15

## Status

Proposed

## Context

* Start simple

## Decision

Develop a simple binary format:

### Dictionary

* 2 bytes: an integer indicating how many terms are in the dictionary
* An array of `NULL` (or newline) terminated strings composed of case-sensitive, US-ASCII characters,
    sorted by number of occurrences in the text in descending order

### Verses
* Store indexes into the dictionary in a variable (1-2 byte) format
  * If the first byte has a high bit of 0, then the remaining 7 bits
    are used to address positions 0-127 in the dictionary
  * If the first byte has a high bit of 1, then use the current byte
    and next byte for a total of 15 bits of address space (32k values).
    This will work for the KVJ which has `13683` distinct tokens.

## Consequences

* Likely will not make it under the 1MB goal
* No features to directly support "fast" indexing


## Random musings

The patent refers to two, maybe three files existing:

* One that is compacted uses the skipping method, containing usable/searchable tokens
* One that directly encodes stop words/tokens
* Maybe one that has a bitmap of which file has which token

It is possible thaat the stop wordds can be encoded in 4-6 bits (giving us 15, 31 or 63 stop words),
so file number two could be smaller than 8 bits/token

The compacted file likely uses a variable byte encoding, using 1-2 bytes
per token.

The bitmap file would likely need one bit per token. If the KJV has 916847 total tokens,
this means we could need at a minimum 916847/8 = 114,605 bytes, or over 1/10th of our
storage space, to store a 0 (compact) or 1 (stop word) switch bit.

| Number | Token | Stop? | Count |
| ------ | ----- | ----- | ----- |
|1|,|Y|70574|
|2|the|Y|62058|
|3|and|Y|38842|
|4|of|Y|34426|
|5|.|Y|26201|
|6|to|Y|13378|
|7|And|Y|12840|
|8|:|Y|12698|
|9|that|Y|12573|
|10|in|Y|12330|
|11|;|Y|10139|
|12|shall|?|9760|
|13|he| |9658|
|14|unto| |8940|
|15|I| |8847|
|16|his| |8385|
|17|a|Y|7942|
|18|for|Y|7180|
|19|they| |6967|
|20|be|Y|6877|
|21|is|Y|6831|
|22|him| |6649|
|23|LORD| |6646|
|24|not|Y|6550|
|25|them| |6425|
|26|with| |5961|
|27|it|Y|5891|
|28|all|Y|5426|
|29|thou| |4889|
|30|was| |4515|
|31|thy| |4450|
|32|which| |4277|
|33|my| |4135|
|34|God|N|4111|
|35|me| |4092|
|36|said|Y|3994|
|37|their| |3878|
|38|have| |3843|
|39|thee| |3826|
|40|will| |3807|
|41|ye| |3698|
|42|from| |3578|
|43|?|Y|3297|
|44|as|Y|3256|
|45|are|Y|2912|
|46|were|Y|2767|
|47|out| |2750|
|48|upon| |2730|
|49|man| |2720|
|50|you| |2612|
|51|Israel| |2574|
|52|by|Y|2539|
|53|when| |2484|
|54|king| |2465|
|55|thisY |2453|
|56|but|Y|2427|
|57|up|Y|2372|
|58|hath| |2239|
|59|people| |2143|
|60|son| |2107|
|61|came| |2091|
|62|there| |2086|
|63|had|Y|2025|
|64|house| |2023|
|65|into| |2014|
|66|'|Y|2010|
|67|her| |1977|
|68|on| |1968|
|69|come| |1849|
|70|one| |1847|
|71|The| |1840|
|72|children| |1816|
|73|s| |1783|
|74|before| |1775|
|75|your| |1761|
|76|day| |1740|
|77|land| |1718|
|78|For|Y|1709|
|79|an|Y|1673|
|80|also| |1665|
|81|men| |1658|
|82|against| |1657|
|83|we| |1643|
|84|shalt| |1613|
|85|But| |1556|
|86|at|Y|1507|
|87|hand| |1466|
|88|us| |1447|
|89|made| |1405|
|90|went| |1399|
|91|saying| |1390|
|92|Then| |1373|
|93|no|Y|1343|
|94|do|Y|1301|
|95|even| |1297|
|96|saith| |1262|
|97|go| |1257|
|98|every| |1178|
|99|things| |1162|
|100|our| |1136|

So if we limited ourselves to 4 bits for stop words, or 16 total:

1. ,
2. the
3. and
4. of
5. .
6. to
7. And
8. :
9. that
10. in
11. ;
12. a
13. for
14. be
15. is
16. not
    (we may need to store a end-of-verse marker instead)

That would be a total of 341,439 occurrences of stopwords. Meaning,
we could store those in 170,720 bytes, plus the switch file at 114605 bytes,
yielding 285,325 total bytes for the top 16 stopwords,
which give us an overall savings of 56,114 bytes.

However, for the switch file, we would have a 916847-bit long
field to have to search for a given token. For a given verse,
we do not know (with the information we have so far) where in that bit field
a verse begins. It may make more sense to store this instead
as a header inside the verse, which may mean there are a lot of bits
that are unused.

If we stored it as a per-verse header, we would need 128309 bytes, vs 114605.

The longest verse is 104 tokens. So, we may want to store a flat array
of verse lengths instead of using an end-of-verse marker.
If we got really clever we could store this as 7 bits (128 values),
but that might not work for other translations, so we should stick with
8 bit bytes. In fact, we likely want to store a mapping of book to number of chapters,
and chapter to number of verses.

* 66 bytes to store how many chapters are in each book
* 1189 bytes to store how many verses are in each chapter
* 31103 bytes to store how many tokens are in each verse
