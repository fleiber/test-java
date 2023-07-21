package fleiber.presents

import com.google.common.collect.BiMap
import com.google.common.collect.EnumBiMap
import com.google.common.collect.Multimaps
import com.google.common.collect.SetMultimap
import fleiber.presents.Adult.*
import fleiber.presents.Child.*
import java.util.*
import kotlin.math.ceil
import kotlin.random.Random


fun main() {
    println("\n===================================")
    println("Adults:\n")
    for (adult in Adult.entries) {
        println(adult.initials + ". " + adult.firstName + if (adult.nickName != null) " [" + adult.nickName + "]" else "")
    }
    println("\n===================================")
    println("Families:\n")
    for (family in Family.entries) {
        println(family.toString() + ": " + family.father.displayName + " and " + family.mother.displayName + " " + if (family.children.isEmpty()) "" else family.children)
    }
    println("\n===================================")
    println("God-parents:\n")
    GOD_PARENTS.forEach { (child, adults) -> println("$child: $adults") }
    println("\n===================================")
    println("Adults' presents assignments:\n")
    val adultToAdultAssignment: Map<Adult, Adult> = computeAdultToAdultAssignments()
    adultToAdultAssignment.forEach { (giver: Adult, receiver: Adult) -> println("$giver => $receiver") }
    println("\n===================================")
    println("Children's presents assignments:\n")
    val adultToChildAssignment = computeAdultToChildAssignments()
    adultToChildAssignment.asMap().forEach { (giver: Adult, receivers: Collection<Child>) -> println("$giver => $receivers") }
}


private val MAX_CHILDREN_GIFTS: Map<Adult, Int> = mapOf(BRUNO to 1, NELLY to 1)

val ASSIGNMENTS_2018 = mapOf<Adult, List<Person>>(
    DENIS to listOf(LOUIS_MARIE, CYPRIEN, AUGUSTIN),
    CATHERINE to listOf(EMMANUEL, CLAUDE /*JOSEPH*/),
    FRANCOIS to listOf(HELENE, PIERRE/*, ANTOINE*/),
    JIE to listOf(CATHERINE, TIMOTHEE),
    EMMANUEL to listOf(DENIS, BARNABE, ETIENNE),
    HELENE to listOf(JIE, DANAELLE, GREGOIRE),
    LOUIS_MARIE to listOf(NELLY, JEANNE, EMMA),
    ANNE_EMMANUEL to listOf(BRUNO, AMELIE),
    BRUNO to listOf(FRANCOIS),
    NELLY to listOf(ANNE_EMMANUEL)
)
val ASSIGNMENTS_2019 = mapOf<Adult, List<Person>>(
    DENIS to listOf(EMMANUEL, BARNABE, GREGOIRE),
    CATHERINE to listOf(ANNE_EMMANUEL, AMELIE, TIMOTHEE),
    FRANCOIS to listOf(BRUNO, CYPRIEN, AUGUSTIN),
    JIE to listOf(HELENE/*, ANTOINE*/),
    EMMANUEL to listOf(CATHERINE, ETIENNE),
    HELENE to listOf(LOUIS_MARIE, CLAUDE, EMMA),
    LOUIS_MARIE to listOf(JIE, JEANNE, PIERRE),
    ANNE_EMMANUEL to listOf(NELLY, DANAELLE),
    BRUNO to listOf(DENIS),
    NELLY to listOf(FRANCOIS)
)
val ASSIGNMENTS_2020 = mapOf<Adult, List<Person>>(
    DENIS to listOf(BRUNO, CLAUDE, JEANNE),
    CATHERINE to listOf(JIE, BARNABE, EMMA),
    FRANCOIS to listOf(NELLY, TIMOTHEE, GREGOIRE),
    JIE to listOf(EMMANUEL, ETIENNE, TERESA),
    EMMANUEL to listOf(ANNE_EMMANUEL, DANAELLE/*, ANTOINE*/),
    HELENE to listOf(DENIS, CYPRIEN, AUGUSTIN),
    LOUIS_MARIE to listOf(CATHERINE, AMELIE),
    ANNE_EMMANUEL to listOf(FRANCOIS, PIERRE),
    BRUNO to listOf(HELENE),
    NELLY to listOf(LOUIS_MARIE)
)
val ASSIGNMENTS_2021 = mapOf<Adult, List<Person>>(
    DENIS to listOf(HELENE, EMMA, TERESA),
    CATHERINE to listOf(LOUIS_MARIE, AMELIE, GREGOIRE),
    FRANCOIS to listOf(ANNE_EMMANUEL, ETIENNE),
    JIE to listOf(NELLY, AUGUSTIN),
    EMMANUEL to listOf(BRUNO, TIMOTHEE, CYPRIEN),
    HELENE to listOf(FRANCOIS, BARNABE),
    LOUIS_MARIE to listOf(EMMANUEL, DANAELLE),
    ANNE_EMMANUEL to listOf(CATHERINE, JEANNE),
    BRUNO to listOf(JIE, PIERRE),
    NELLY to listOf(DENIS, CLAUDE)
)

private data class Assignment<P : Person>(
    val giver: Adult,
    val receiver: P,
    // start with uniform probability, then reducing the probability for past giver/receiver pairs
    // could be tuned in various ways, like women having a higher proba of offering to girls (or the opposite), etc
    val proba: Double = Random.nextDouble() + when (receiver) {
        in ASSIGNMENTS_2021[giver]!! -> -0.8
        in ASSIGNMENTS_2020[giver]!! -> -0.4
        in ASSIGNMENTS_2019[giver]!! -> -0.2
        in ASSIGNMENTS_2018[giver]!! -> -0.1
        else -> 0.0
    }
)

/**
 * Computes 1-1 giver-receiver relations between adults.
 */
private fun computeAdultToAdultAssignments(): BiMap<Adult, Adult> {
    val assignments = EnumBiMap.create(Adult::class.java, Adult::class.java)
    do {
        assignments.clear()

        // compute the pairs of all giver-receiver possibilities, with a random proba
        val possibilities = mutableListOf<Assignment<Adult>>()
        for (giver in Adult.entries) {
            for (receiver in Adult.entries) {
                if (giver !== receiver && !giver.isMarriedTo(receiver)) {    // only forbidden: offer to myself or my husband/wife
                    possibilities += Assignment(giver, receiver)
                }
            }
        }
        // sort by proba, then loop (highest probas first) and add pair if giver and receiver were not yet included
        possibilities.sortBy { -it.proba }
        possibilities.forEach { (giver, receiver) ->
            if (!assignments.containsKey(giver) && !assignments.containsValue(receiver)) {
                assignments[giver] = receiver
            }
        }
    } while (assignments.size < Adult.entries.size) // sometimes we may have fallen on an impossible case, just try again
    return assignments
}

/**
 * Computes 1-n giver-receiver relations between adults and children.
 */
private fun computeAdultToChildAssignments(): SetMultimap<Adult, Child> {
    val assignments = Multimaps.newSetMultimap(EnumMap(Adult::class.java)) { EnumSet.noneOf(Child::class.java) }
    val maxChildrenByAdult = ceil(Child.entries.size.toDouble() / Adult.entries.size).toInt()
    do {
        assignments.clear()

        // all this would be so much more readable with a matrix library!

        // compute the pairs of all giver-receiver possibilities, with a random proba
        val possibilities = mutableListOf<Assignment<Child>>()
        for (giver in Adult.entries) {
            if (MAX_CHILDREN_GIFTS[giver] == 0) continue
            for (receiver in Child.entries) {
                if (!giver.isParentOf(receiver) && !giver.isGodParentOf(receiver)) {    // forbidden: give to one of my children or my godson
                    possibilities += Assignment(giver, receiver)
                }
            }
        }
        // sort by proba, then loop (highest probas first) and add pair if giver and receiver were not yet included
        while (possibilities.isNotEmpty()) {
            val (currentGiver, currentReceiver) = possibilities.maxByOrNull { it.proba }!!
            assignments[currentGiver] += currentReceiver
            val copy = possibilities.toMutableList()
            possibilities.clear()
            val isGiverDone = assignments[currentGiver].size == (MAX_CHILDREN_GIFTS[currentGiver] ?: maxChildrenByAdult)
            copy.forEach { pair ->
                if (pair.receiver === currentReceiver) return@forEach
                if (isGiverDone && pair.giver === currentGiver) return@forEach
                possibilities += if (pair.giver === currentGiver) pair.copy(proba = pair.proba - 0.5) else pair
            }
        }
    } while (assignments.size() < Child.entries.size || assignments.keys().size < Adult.entries.size) // sometimes we may have fallen on an impossible case, just try again
    return assignments
}
