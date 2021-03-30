package org.danwatt.bibcompact.futurework

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class SkipCondenserTest {
    @Test
    fun noCondensing() {
        assertThat(SkipCondenser().condense(listOf(1, 2, 3)))
            .containsExactly(Lookup(1), Lookup(2), Lookup(3))
        assertThat(SkipCondenser().condense(listOf(3, 2, 1)))
            .containsExactly(Lookup(3), Lookup(2), Lookup(1))
    }

    @Test
    fun simpleRepeats() {
        assertThat(SkipCondenser().condense(listOf(1, 1)))
            .containsExactly(Skip(1), Lookup(1))
        assertThat(SkipCondenser().condense(listOf(1, 1, 1)))
            .containsExactly(Skip(1),Skip(1), Lookup(1))
    }

    @Test
    fun skipDistanceGreaterThanOne() {
        assertThat(SkipCondenser().condense(listOf(1, 2, 1)))
            .containsExactly(Skip(2), Lookup(2), Lookup(1))
        assertThat(SkipCondenser().condense(listOf(1, 2, 3, 1)))
            .containsExactly(Skip(3), Lookup(2), Lookup(3), Lookup(1))
    }

    @Test
    fun multipleRepeatedTokens() {
        assertThat(SkipCondenser().condense(listOf(1, 2, 1, 2)))
            .containsExactly(Skip(2), Skip(2), Lookup(1), Lookup(2))
    }

}