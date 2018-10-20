package fleiber.presents;

import com.google.common.collect.*;
import fleiber.presents.Referential.Adult;
import fleiber.presents.Referential.Child;
import fleiber.presents.Referential.Family;

import java.util.*;

import static fleiber.presents.Referential.Adult.BRUNO;
import static fleiber.presents.Referential.Adult.NELLY;

public final class PresentsAssignment {

    private static final ImmutableSet<Adult> EXEMPTED_FROM_CHILDREN_GIFTS = Sets.immutableEnumSet(BRUNO, NELLY);

    private static final class GiverReceiver<P extends Referential.Person> {

        public final Adult giver;
        public final P receiver;
        public final double proba;

        private GiverReceiver(Adult giver, P receiver, double proba) {
            this.giver = giver;
            this.receiver = receiver;
            this.proba = proba;
        }
    }

    /**
     * Computes 1-1 giver-receiver relations between adults.
     */
    private static BiMap<Adult, Adult> computeAdultToAdultAssignments() {
        BiMap<Adult, Adult> assignments = EnumBiMap.create(Adult.class, Adult.class);
        Random random = new Random();

       do {
            assignments.clear();
            
            // compute the pairs of all giver-receiver possibilities, with a random proba
            List<GiverReceiver<Adult>> pairs = new ArrayList<>();
            for (Adult giver : Adult.values()) {
                for (Adult receiver : Adult.values()) {
                    if (giver != receiver && !Family.areMarried(giver, receiver)) {    // only forbidden: offer to myself or my husband/wife
                        // uniform probability, could be amended (like girls having a higher proba of offering to girl, etc)
                        double proba = random.nextDouble();
                        pairs.add(new GiverReceiver<>(giver, receiver, proba));
                    }
                }
            }
            // sort by proba, then loop (highest probas first) and add pair if giver and receiver were not yet included
            pairs.sort(Comparator.comparingDouble(p -> -p.proba));
            pairs.forEach(pair -> {
                if (!assignments.containsKey(pair.giver) && !assignments.containsValue(pair.receiver)) {
                    assignments.put(pair.giver, pair.receiver);
                }
            });
        } while (assignments.size() < Adult.values().length); // sometimes we may have fallen on an impossible case, just try again
        return assignments;
    }

    /**
     * Computes 1-n giver-receiver relations between adults and children.
     */
    private static SetMultimap<Adult, Child> computeAdultToChildAssignments() {
        SetMultimap<Adult, Child> assignments = Multimaps.newSetMultimap(new EnumMap<>(Adult.class), () -> EnumSet.noneOf(Child.class));
        int maxChildrenByAdult = (int) Math.ceil((double) Child.values().length / Adult.values().length);
        Random random = new Random();

        do {
            assignments.clear();

            // compute the pairs of all giver-receiver possibilities, with a random proba
            List<GiverReceiver<Child>> pairs = new ArrayList<>();
            for (Adult giver : Adult.values()) {
                if (EXEMPTED_FROM_CHILDREN_GIFTS.contains(giver)) {
                    continue;
                }
                for (Child receiver : Child.values()) {
                    if (!Family.isChildOf(giver, receiver) && !Referential.GOD_PARENTS.get(receiver).contains(giver)) {    // forbidden: give to one of my children or my godson
                        for (int i = 0; i < maxChildrenByAdult; i++) {
                            // uniform probability, could be amended
                            double proba = random.nextDouble();
                            pairs.add(new GiverReceiver<>(giver, receiver, proba));
                        }
                    }
                }
            }
            // sort by proba, then loop (highest probas first) and add pair if giver and receiver were not yet included
            pairs.sort(Comparator.comparingDouble(p -> -p.proba));
            pairs.forEach(pair -> {
                if (assignments.get(pair.giver).size() < maxChildrenByAdult && !assignments.containsValue(pair.receiver)) {
                    assignments.put(pair.giver, pair.receiver);
                }
            });
        } while (assignments.size() < Child.values().length || assignments.keys().size() < Adult.values().length); // sometimes we may have fallen on an impossible case, just try again
        return assignments;
    }

    public static void main(String[] args) {
        System.out.println("\n===================================");
        System.out.println("Adults:\n");
        for (Adult adult : Adult.values()) {
            System.out.println(adult.getInitals() + ". " + adult.name + (adult.nickName != null ? " [" + adult.nickName + "]" : ""));
        }

        System.out.println("\n===================================");
        System.out.println("Families:\n");
        for (Family family : Family.values()) {
            System.out.println(family + ": " + family.father.getDisplayName() + " and " + family.mother.getDisplayName() + " " + (family.children.isEmpty() ? "" : family.children));
        }

        System.out.println("\n===================================");
        System.out.println("God-parents:\n");
        Referential.GOD_PARENTS.forEach((child,adults) -> System.out.println(child + ": " + adults));

        System.out.println("\n===================================");
        System.out.println("Adults' presents assignments:\n");
        Map<Adult, Adult> adultToAdultAssignment = computeAdultToAdultAssignments();
        adultToAdultAssignment.forEach((giver, receiver) -> System.out.println(giver + " => " + receiver));

        System.out.println("\n===================================");
        System.out.println("Children's presents assignments:\n");
        SetMultimap<Adult, Child> adultToChildAssignment = computeAdultToChildAssignments();
        adultToChildAssignment.asMap().forEach((giver, receivers) -> System.out.println(giver + " => " + receivers));
    }
}
