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
* An array of `NULL` (or newline) terminated strings composed of case-sensitive, US-ASCII characters, sorted by number
  of occurrences in the text in descending order
* Values are sorted by number of occurrences in descending order

### Text File

* Store indexes into the dictionary in a variable (1-2 byte) format
    * If the first byte has a high bit of 0, then the remaining 7 bits are used to address positions 0-127 in the
      lexicon
    * If the first byte has a high bit of 1, then use the current byte and following byte (32k values). This will work
      for the KVJ which has `13683` distinct tokens.

## Consequences

* Likely will not make it under the 1MB goal
* No features to directly support "fast" indexing

