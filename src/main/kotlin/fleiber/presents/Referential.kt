package fleiber.presents

import fleiber.presents.Adult.*
import fleiber.presents.Child.*


interface Person {

    val firstName: String

    val initials get() = firstName[0].toString() + firstName.indexOf('-').let { if (it == -1) "" else firstName[it + 1] }
}

@Suppress("SpellCheckingInspection")
enum class Adult(override val firstName: String, val nickName: String? = null) : Person {

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

    val displayName get() = nickName ?: firstName

    override fun toString() = firstName
}

// Not as in "child of" but more "underage", otherwise Adults should also be Children
// Another possibility would be to have everyone in the same enum, and rely on the Family enum to extract the generations
@Suppress("SpellCheckingInspection")
enum class Child(override val firstName: String) : Person {

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
//    JOSEPH      ("Joseph"),
//    ANTOINE     ("Antoine"),
    TERESA      ("Teresa"),
    NORA        ("Nora"),
    NATHAN      ("Nathan"),
;

    override fun toString() = firstName
}

// Very traditional representation of families :p
@Suppress("SpellCheckingInspection")
enum class Family(val father: Adult, val mother: Adult, vararg children: Child) {

    PAPE_MAME       (DENIS, CATHERINE),
    FRANCOIS_JIE    (FRANCOIS, JIE,                 AMELIE, BARNABE, CLAUDE, DANAELLE, EMMA),
    MANU_HELENE     (EMMANUEL, HELENE,              JEANNE, PIERRE, TERESA),
    LM_MANUE        (LOUIS_MARIE, ANNE_EMMANUEL,    TIMOTHEE, CYPRIEN, ETIENNE, AUGUSTIN, GREGOIRE/*, JOSEPH, ANTOINE*/),
    BRUNO_NELLY     (BRUNO, NELLY,                  NORA, NATHAN);

    val children = children.toSet()

    override fun toString() = "${father.initials}&${mother.initials}"


    companion object {
        internal val BY_PERSON: Map<Person, Family> = mutableMapOf<Person, Family>().apply {
            Family.entries.forEach { family ->
                this[family.father] = family
                this[family.mother] = family
                family.children.forEach { child -> this[child] = family }
            }
        }
    }
}


val GOD_PARENTS = listOf(
    AMELIE to HELENE,
    BARNABE to LOUIS_MARIE,
    CLAUDE to BRUNO,
    EMMA to EMMANUEL, EMMA to ANNE_EMMANUEL,
    JEANNE to FRANCOIS,
    TERESA to LOUIS_MARIE,
    CYPRIEN to JIE,
    ETIENNE to HELENE,
    GREGOIRE to EMMANUEL,
//    JOSEPH to FRANCOIS,
).groupBy({ it.first }, { it.second })


fun Adult.isMarriedTo(other: Adult) = family === other.family
fun Adult.isParentOf(child: Child) = family === child.family
fun Adult.isGodParentOf(child: Child) = GOD_PARENTS[child]?.let { this in it } ?: false
val Person.family get() = Family.BY_PERSON[this] ?: error("Oh you poor little soul!")
