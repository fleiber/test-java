package fleiber.presents

import com.google.common.collect.*
import fleiber.presents.Adult.*
import fleiber.presents.Child.*
import java.util.*
import kotlin.math.ceil
import kotlin.random.Random


fun main() {
    println("\n===================================")
    println("Adults:\n")
    for (adult in Adult.values()) {
        println(adult.initials + ". " + adult.firstName + if (adult.nickName != null) " [" + adult.nickName + "]" else "")
    }
    println("\n===================================")
    println("Families:\n")
    for (family in Family.values()) {
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


private val EXEMPTED_FROM_CHILDREN_GIFTS = setOf(BRUNO, NELLY)

val ASSIGNMENTS_2018 = mapOf(
        DENIS to listOf<Person>(LOUIS_MARIE, CYPRIEN, AUGUSTIN),
        CATHERINE to listOf(EMMANUEL, CLAUDE, /*JOSEPH*/),
        FRANCOIS to listOf(HELENE, PIERRE, ANTOINE),
        JIE to listOf(CATHERINE, TIMOTHEE),
        EMMANUEL to listOf(DENIS, BARNABE, ETIENNE),
        HELENE to listOf(JIE, DANAELLE, GREGOIRE),
        LOUIS_MARIE to listOf(NELLY, JEANNE, EMMA),
        ANNE_EMMANUEL to listOf(BRUNO, AMELIE),
        BRUNO to listOf(FRANCOIS),
        NELLY to listOf(ANNE_EMMANUEL))
val ASSIGNMENTS_2019 = mapOf(
        DENIS to listOf<Person>(EMMANUEL, BARNABE, GREGOIRE),
        CATHERINE to listOf(ANNE_EMMANUEL, AMELIE, TIMOTHEE),
        FRANCOIS to listOf(BRUNO, CYPRIEN, AUGUSTIN),
        JIE to listOf(HELENE, ANTOINE),
        EMMANUEL to listOf(CATHERINE, ETIENNE),
        HELENE to listOf(LOUIS_MARIE, CLAUDE, EMMA),
        LOUIS_MARIE to listOf(JIE, JEANNE, PIERRE),
        ANNE_EMMANUEL to listOf(NELLY, DANAELLE),
        BRUNO to listOf(DENIS),
        NELLY to listOf(FRANCOIS))

private data class GiverReceiver<P : Person>(val giver: Adult, val receiver: P) {
    // start with uniform probability, then reducing the probability for past giver/receiver pairs
    // could be tuned in various ways, like girls having a higher proba of offering to girl, etc
    val proba = Random.nextDouble() + (if (receiver in ASSIGNMENTS_2019[giver]!!) -0.3 else 0.0) + (if (receiver in ASSIGNMENTS_2018[giver]!!) -0.15 else 0.0)
}

/**
 * Computes 1-1 giver-receiver relations between adults.
 */
private fun computeAdultToAdultAssignments(): BiMap<Adult, Adult> {
    val assignments = EnumBiMap.create(Adult::class.java, Adult::class.java)
    do {
        assignments.clear()

        // compute the pairs of all giver-receiver possibilities, with a random proba
        val pairs = mutableListOf<GiverReceiver<Adult>>()
        for (giver in Adult.values()) {
            for (receiver in Adult.values()) {
                if (giver !== receiver && !giver.isMarriedTo(receiver)) {    // only forbidden: offer to myself or my husband/wife
                    pairs += GiverReceiver(giver, receiver)
                }
            }
        }
        // sort by proba, then loop (highest probas first) and add pair if giver and receiver were not yet included
        pairs.sortBy { -it.proba }
        pairs.forEach { (giver, receiver) ->
            if (!assignments.containsKey(giver) && !assignments.containsValue(receiver)) {
                assignments[giver] = receiver
            }
        }
    } while (assignments.size < Adult.values().size) // sometimes we may have fallen on an impossible case, just try again
    return assignments
}

/**
 * Computes 1-n giver-receiver relations between adults and children.
 */
private fun computeAdultToChildAssignments(): SetMultimap<Adult, Child> {
    val assignments = Multimaps.newSetMultimap(EnumMap(Adult::class.java)) { EnumSet.noneOf(Child::class.java) }
    val maxChildrenByAdult = ceil(Child.values().size.toDouble() / Adult.values().size).toInt()
    do {
        assignments.clear()

        // compute the pairs of all giver-receiver possibilities, with a random proba
        val pairs = mutableListOf<GiverReceiver<Child>>()
        for (giver in Adult.values()) {
            if (giver in EXEMPTED_FROM_CHILDREN_GIFTS) continue
            for (receiver in Child.values()) {
                if (!giver.isParentOf(receiver) && !giver.isGodParentOf(receiver)) {    // forbidden: give to one of my children or my godson
                    for (i in 0 until maxChildrenByAdult) pairs += GiverReceiver(giver, receiver)
                }
            }
        }
        // sort by proba, then loop (highest probas first) and add pair if giver and receiver were not yet included
        pairs.sortBy { -it.proba }
        pairs.forEach { (giver, receiver) ->
            if (assignments[giver].size < maxChildrenByAdult && !assignments.containsValue(receiver)) {
                assignments.put(giver, receiver)
            }
        }
    } while (assignments.size() < Child.values().size || assignments.keys().size < Adult.values().size) // sometimes we may have fallen on an impossible case, just try again
    return assignments
}
