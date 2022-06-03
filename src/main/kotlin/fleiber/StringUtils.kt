package fleiber

import kotlin.math.min


fun String.parseInt(beginIndex: Int, endIndex: Int) = Integer.parseInt(this, beginIndex, endIndex, 10)

/**
 * Implementation adapted from https://github.com/tdebatty/java-string-similarity.
 *
 * The Levenshtein distance, or edit distance, between two words is the
 * minimum number of single-character edits (insertions, deletions or
 * substitutions) required to change one word into the other.
 *
 * http://en.wikipedia.org/wiki/Levenshtein_distance
 *
 * It is always at least the difference of the sizes of the two strings.
 * It is at most the length of the longer string.
 * It is zero if and only if the strings are equal.
 * If the strings are the same size, the Hamming distance is an upper bound
 * on the Levenshtein distance.
 * The Levenshtein distance verifies the triangle inequality (the distance
 * between two strings is no greater than the sum Levenshtein distances from
 * a third string).
 *
 * Implementation uses dynamic programming (Wagner–Fischer algorithm), with
 * only 2 rows of data. The space requirement is thus O(m) and the algorithm
 * runs in O(mn).
 *
 * @param s1 The first string to compare.
 * @param s2 The second string to compare.
 * @param limit The maximum result to compute before stopping. This
 * means that the calculation can terminate early if you
 * only care about strings with a certain similarity.
 * Set this to Integer.MAX_VALUE if you want to run the
 * calculation to completion in every case.
 */
fun levenshteinDistance(s1: String, s2: String, limit: Int = Int.MAX_VALUE): Int {
    if (s1 == s2) return 0
    if (s1.isEmpty()) return s2.length
    if (s2.isEmpty()) return s1.length

    // create two work vectors of integer distances
    // initialize v0 (the previous row of distances)
    // this row is A[0][i]: edit distance for an empty s
    // the distance is just the number of characters to delete from t
    var v0 = IntArray(s2.length + 1) { it }
    var v1 = IntArray(s2.length + 1)

    for (i in s1.indices) {
        // calculate v1 (current row distances) from the previous row v0
        // first element of v1 is A[i+1][0]
        //   edit distance is delete (i+1) chars from s to match empty t
        v1[0] = i + 1
        var v1min = v1[0]

        // use formula to fill in the rest of the row
        for (j in s2.indices) {
            val cost = if (s1[i] == s2[j]) 0 else 1
            v1[j + 1] = min(
                v1[j] + 1,  // Cost of insertion
                min(
                    v0[j + 1] + 1,  // Cost of removal
                    v0[j] + cost
                )
            ) // Cost of substitution
            v1min = min(v1min, v1[j + 1])
        }
        if (v1min >= limit) return limit

        // Flip references to current and previous row
        val vtemp = v0
        v0 = v1
        v1 = vtemp
    }
    return v0[s2.length]
}
