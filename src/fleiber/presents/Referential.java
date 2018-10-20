package fleiber.presents;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.ImmutableSetMultimap;
import com.google.common.collect.SetMultimap;

import java.util.Optional;

import static fleiber.presents.Referential.Adult.*;
import static fleiber.presents.Referential.Child.*;

public class Referential {

    public interface Person {
        // just a tag
    }

    public enum Adult implements Person {
        DENIS           ("Denis", "Papé"),
        CATHERINE       ("Catherine", "Mamé"),
        FRANCOIS        ("François"),
        JIE             ("Jie"),
        EMMANUEL        ("Emmanuel", "Manu"),
        HELENE          ("Hélène"),
        LOUIS_MARIE     ("Louis-Marie"),
        ANNE_EMMANUEL   ("Anne-Emmanuel", "Manue"),
        BRUNO           ("Bruno"),
        NELLY           ("Nelly");

        public final String name;
        public final String nickName;

        Adult(String name) {
            this(name, null);
        }
        Adult(String name, String nickName) {
            this.name = name;
            this.nickName = nickName;
        }

        public Optional<String> getNickname() {
            return Optional.ofNullable(nickName);
        }
        public String getDisplayName() {
            return nickName != null ? nickName : name;
        }
        public String getInitals() {
            return name.substring(0, 1) + (name.contains("-") ? name.charAt(name.indexOf("-") + 1) : "");
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Child implements Person {    // Not as in "child of" but more "underage", otherwise Adults should also be Children
        AMELIE      ("Amélie"),
        BARNABE     ("Barnabé"),
        CLAUDE      ("Claude"),
        DANAELLE    ("Danaelle"),
        EMMA        ("Emma"),
        JEANNE      ("Jeanne"),
        PIERRE      ("Pierre"),
        TIMOTHEE    ("Timothée"),
        CYPRIEN     ("Cyprien"),
        ETIENNE     ("Étienne"),
        AUGUSTIN    ("Augustin"),
        GREGOIRE    ("Grégoire"),
        JOSEPH      ("Joseph"),
        ANTOINE     ("Antoine");

        public final String name;

        Child(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public enum Family {   // Very traditional representation of families :p

        PAPE_MAME       (DENIS, CATHERINE),
        FRANCOIS_JIE    (FRANCOIS, JIE, AMELIE, BARNABE, CLAUDE, DANAELLE, EMMA),
        MANU_HELENE     (EMMANUEL, HELENE, JEANNE, PIERRE),
        LM_MANUE        (LOUIS_MARIE, ANNE_EMMANUEL, TIMOTHEE, CYPRIEN, ETIENNE, AUGUSTIN, GREGOIRE, JOSEPH, ANTOINE),
        BRUNO_NELLY     (BRUNO, NELLY);

        public final Adult father;
        public final Adult mother;
        public final ImmutableSet<Child> children;

        Family(Adult father, Adult mother, Child... children) {
            this.father = father;
            this.mother = mother;
            this.children = ImmutableSet.copyOf(children);
        }

        @Override
        public String toString() {
            return father.getInitals() + "&" + mother.getInitals();
        }


        private static final ImmutableMap<Person,Family> PER_PERSON;
        static {
            ImmutableMap.Builder<Person,Family> builder = ImmutableMap.builder();
            for (Family family : Family.values()) {
                builder.put(family.father, family);
                builder.put(family.mother, family);
                family.children.forEach(child -> builder.put(child, family));
            }
            PER_PERSON = builder.build();
        }

        public static Family getFamily(Person person) {
            return PER_PERSON.get(person);
        }
        public static boolean areMarried(Adult adult1, Adult adult2) {
            return getFamily(adult1).equals(getFamily(adult2));
        }
        public static boolean isChildOf(Adult adult, Child child) {
            return getFamily(adult).equals(getFamily(child));
        }
    }

    public static final SetMultimap<Child,Adult> GOD_PARENTS = ImmutableSetMultimap.<Child,Adult>builder()
            .put(AMELIE, HELENE)
            .put(BARNABE, LOUIS_MARIE)
            .put(CLAUDE, BRUNO)
            .putAll(EMMA, EMMANUEL, ANNE_EMMANUEL)
            .put(JEANNE, FRANCOIS)
            .put(CYPRIEN, JIE)
            .put(ETIENNE, HELENE)
            .put(GREGOIRE, EMMANUEL)
            .put(JOSEPH, FRANCOIS)
            .build();
}
