# 2. Approach 1

Date: 2021-03-08

## Status

Accepted

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