package fleiber.presents

import com.google.common.collect.*
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


private data class GiverReceiver<P : Person>(val giver: Adult, val receiver: P, val proba: Double)
private val EXEMPTED_FROM_CHILDREN_GIFTS = setOf(Adult.BRUNO, Adult.NELLY)

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
                    // uniform probability, could be amended (like girls having a higher proba of offering to girl, etc)
                    val proba = Random.nextDouble()
                    pairs.add(GiverReceiver(giver, receiver, proba))
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
                    for (i in 0 until maxChildrenByAdult) {
                        // uniform probability, could be amended
                        val proba = Random.nextDouble()
                        pairs.add(GiverReceiver(giver, receiver, proba))
                    }
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
