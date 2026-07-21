package com.skylark.skylarkbiagentbackend.resolution;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class JaroWinklerSimilarityTest {

    @Test
    void identicalStrings_scoreOne() {
        assertThat(JaroWinklerSimilarity.similarity("SKYLARK", "SKYLARK")).isEqualTo(1.0);
    }

    @Test
    void classicReferencePair_marthaMarhta() {
        // Well-known reference value for Jaro-Winkler similarity implementations.
        assertThat(JaroWinklerSimilarity.similarity("MARTHA", "MARHTA")).isCloseTo(0.961, within(0.005));
    }

    @Test
    void completelyDifferentStrings_lowScore() {
        assertThat(JaroWinklerSimilarity.similarity("ABCDEF", "ZYXWVU")).isLessThan(0.3);
    }

    @Test
    void emptyOrNullInput_scoreZero() {
        assertThat(JaroWinklerSimilarity.similarity("", "ABC")).isZero();
        assertThat(JaroWinklerSimilarity.similarity(null, "ABC")).isZero();
        assertThat(JaroWinklerSimilarity.similarity("ABC", null)).isZero();
    }

    @Test
    void commonPrefixBoostsScoreOverPlainJaro() {
        double withSharedPrefix = JaroWinklerSimilarity.similarity("SKYLARK DRONES", "SKYLARK DRONE");
        double withoutSharedPrefix = JaroWinklerSimilarity.similarity("DRONES SKYLARK", "DRONE SKYLARK");
        assertThat(withSharedPrefix).isGreaterThan(withoutSharedPrefix);
    }
}
