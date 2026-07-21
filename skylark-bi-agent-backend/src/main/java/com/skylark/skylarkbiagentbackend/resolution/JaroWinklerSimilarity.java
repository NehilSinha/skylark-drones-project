package com.skylark.skylarkbiagentbackend.resolution;

/**
 * Standard Jaro-Winkler string similarity, hand-implemented rather than pulling in
 * a text-similarity library for one algorithm. Returns a score in {@code [0, 1]},
 * 1.0 meaning identical.
 */
final class JaroWinklerSimilarity {

    private static final int MAX_PREFIX_LENGTH = 4;
    private static final double WINKLER_SCALING_FACTOR = 0.1;

    private JaroWinklerSimilarity() {
    }

    static double similarity(String a, String b) {
        if (a == null || b == null) {
            return 0.0;
        }
        if (a.equals(b)) {
            return 1.0;
        }

        int len1 = a.length();
        int len2 = b.length();
        if (len1 == 0 || len2 == 0) {
            return 0.0;
        }

        int matchDistance = Math.max(0, Math.max(len1, len2) / 2 - 1);

        boolean[] aMatches = new boolean[len1];
        boolean[] bMatches = new boolean[len2];
        int matches = 0;

        for (int i = 0; i < len1; i++) {
            int start = Math.max(0, i - matchDistance);
            int end = Math.min(i + matchDistance + 1, len2);
            for (int j = start; j < end; j++) {
                if (bMatches[j] || a.charAt(i) != b.charAt(j)) {
                    continue;
                }
                aMatches[i] = true;
                bMatches[j] = true;
                matches++;
                break;
            }
        }

        if (matches == 0) {
            return 0.0;
        }

        double transpositions = 0;
        int k = 0;
        for (int i = 0; i < len1; i++) {
            if (!aMatches[i]) {
                continue;
            }
            while (!bMatches[k]) {
                k++;
            }
            if (a.charAt(i) != b.charAt(k)) {
                transpositions++;
            }
            k++;
        }
        transpositions /= 2;

        double jaro = ((double) matches / len1 + (double) matches / len2 + (matches - transpositions) / matches) / 3.0;

        int prefixLength = 0;
        int maxPrefix = Math.min(MAX_PREFIX_LENGTH, Math.min(len1, len2));
        for (int i = 0; i < maxPrefix; i++) {
            if (a.charAt(i) != b.charAt(i)) {
                break;
            }
            prefixLength++;
        }

        return jaro + prefixLength * WINKLER_SCALING_FACTOR * (1 - jaro);
    }
}
