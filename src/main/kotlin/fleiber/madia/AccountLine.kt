package fleiber.madia

import fleiber.madia.AccountLineCategory.*
import fleiber.parseInt
import java.nio.file.Path
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.Month
import kotlin.io.path.forEachLine
import fleiber.madia.AccountLineCategory.DEPENSE as CAT_DEPENSE
import fleiber.madia.AccountLineCategory.AUTRE_RESSOURCE as CAT_RESSOURCE


@Suppress("SpellCheckingInspection")
enum class AccountLineCategory(
    val description: String,
) {

    VIREMENT_INTERNE    ("Virement interne"),
    PROJET              ("Projets"),
    DON                 ("Don"),
    AUTRE_RESSOURCE     ("Autre ressource"),
    DEPENSE             ("Dépense"),
    ;
}



@Suppress("SpellCheckingInspection")
enum class AccountLineSubCategory(
    val category: AccountLineCategory,
    val description: String,
    private val predicate: (firstLine: String, full: String) -> Boolean
) {

    // MG account
    VIREMENT_EUR_MGA          (VIREMENT_INTERNE, "Virement depuis compte FR",       { firstLine, _ -> firstLine == "VIRT RECU DE : MADIA" }),

    PROJET_ORPHELINAT_BROUSSE (PROJET,           "Projet orphelinat brousse",       { firstLine, _ -> firstLine == "VIRT FAV: ORPHELINAT DE BROUSS" }),
    PROJET_BETONNIER          (PROJET,           "Projet bétonnier",                { firstLine, _ -> firstLine == "VIRT FAV: RAZAFINDRAKOTO RENE" }),
    PROJET_FIANAR             (PROJET,           "Projet Fianarantsoa",             { firstLine, _ -> firstLine.replace("VIRT FAV :", "VIRT FAV:") == "VIRT FAV: AIC FIANARANTSOA" }),

    // FR account
    AUTRE_RESOURCE            (CAT_RESSOURCE,    "Autre ressource",                 { firstLine, full -> firstLine.startsWith("VIR RECU") && "MOTIF: MARCHE DE LAVENT" in full }),

    VIREMENT_RECU             (DON,              "Virement reçu",                   { firstLine, _ -> firstLine.startsWith("VIR RECU") || firstLine.startsWith("VIR INST REC") }),
    PRELEVEMENT               (DON,              "Prélèvement",                     { firstLine, _ -> "PRLV EUROPEEN EMIS" in firstLine }),
    DEPOT_CHEQUE              (DON,              "Dépôt chèques",                   { firstLine, _ -> firstLine.startsWith("REMISE CHEQUE") }),

    DEPOT_ESPECES             (DON,              "Dépôt espèces",                   { firstLine, _ -> firstLine.startsWith("VERSEMENT EXPRESS") }),

    VIREMENT_MG               (VIREMENT_INTERNE, "Virement vers compte MG",         { firstLine, full -> if ("VIR INTL EMIS" in firstLine) { check(full.endsWith("CHEZ: BFAVMGMGXXX BFV-STE.GENERALE/TANANAR")) { "Destinataire virement inconnu: $full" }; true } else false }),

    // Common
    FRAIS_BANCAIRES           (CAT_DEPENSE,      "Frais bancaires",                 { firstLine, _ ->
                firstLine.startsWith("FACTURATION PROGELIANCE NET") ||  // abonnement SG FR
                firstLine.startsWith("COTISATION JAZZ ASSOCIATIONS") || // abonnement SG FR
                firstLine.startsWith("FRAIS SUR PRLV EUROP. EMIS") ||   // frais pour remise prélèvements
                firstLine.startsWith("FRAIS VIR INTL") ||               // frais pour virement FR -> MG
                firstLine.startsWith("COMMISSION") ||                   // frais virement MG ?
                firstLine.startsWith("AGIOS DU") ||                     // frais compte SG MG ?
                firstLine == "ABONNEMENT SG CONNECT" }                  // abonnement SG MG
    ),
    DEPENSE                   (CAT_DEPENSE,      "Dépense",                         { _, _ -> true }),
    ;


    companion object {
        private val ALL = values()
        fun fromBankStatement(firstLine: String, full: String): AccountLineSubCategory = ALL.first { it.predicate(firstLine, full) }
        fun fromDescription(description: String): AccountLineSubCategory = ALL.firstOrNull { it.description == description } ?: error("Could not find AccountLineType for $description")
    }
}

data class AccountLine(
    val date: LocalDate,
    val subCategory: AccountLineSubCategory,
    val debit: Float,
    val credit: Float,
    val detailsText: String
) {

    val fiscalYear get() = if (subCategory === AccountLineSubCategory.DEPOT_CHEQUE && date.month === Month.JANUARY && date.dayOfMonth < 20) date.year - 1 else date.year
    val amount get() = if (credit.isNaN()) -debit else credit

    fun toCsv() = "$fiscalYear,$date,${subCategory.category.description},${subCategory.description},${debit.formatCsv()},${credit.formatCsv()},\"$detailsText\""

    override fun toString() = date.toString().padEnd(12) +
            ' ' + subCategory.category.description.padEnd(20) +
            ' ' + subCategory.description.padEnd(32) +
            ' ' + (if (debit.isNaN()) "" else AMOUNT_FORMAT.format(debit.toDouble())).padStart(14) +
            ' ' + (if (credit.isNaN()) "" else AMOUNT_FORMAT.format(credit.toDouble())).padStart(14) +
            "  / " + detailsText.replace("\n", "    ")


    companion object {
        fun fromBankStatement(fileDate: LocalDate, dateStr: String, detailsStr: String, debitStr: String, creditStr: String): AccountLine {
            val date = if (dateStr.length == 5 && dateStr[2] == '.') {
                LocalDate.of(fileDate.year, dateStr.parseInt(3, 5), dateStr.parseInt(0, 2))
            } else {
                check(dateStr.length > 10 && dateStr[2] == '/' && dateStr[5] == '/') { "Unknown date format: $dateStr" }
                LocalDate.of(dateStr.parseInt(6, 10), dateStr.parseInt(3, 5), dateStr.parseInt(0, 2))
            }
            val firstLine = detailsStr.substringBefore('\n')
            val type = AccountLineSubCategory.fromBankStatement(firstLine, detailsStr)
            return AccountLine(date, type, debitStr.parseFrFloat(), creditStr.parseFrFloat(), detailsStr)
        }

        private fun String.parseFrFloat() = if (isEmpty()) Float.NaN else replace(".", "").replace(',', '.').toFloat()
        private fun String.parseFloat() = if (isEmpty()) Float.NaN else toFloat()
        private fun Float.formatCsv() = if (isNaN()) "" else toString()

        @Suppress("SpellCheckingInspection")
        const val CSV_HEADER = "Année fiscale,Date,Catégorie,Sous-catégorie,Débit,Crédit,Détails"
        private val AMOUNT_FORMAT = DecimalFormat("###,###.00")

        fun loadCsv(file: Path): List<AccountLine> {
            fun create(line: Array<String>): AccountLine {
                val date = LocalDate.of(line[0].parseInt(0, 4), line[0].parseInt(5, 7), line[0].parseInt(8, 10))
                return AccountLine(date, AccountLineSubCategory.fromDescription(line[1]), line[2].parseFloat(), line[3].parseFloat(), line[4])
            }

            val results = mutableListOf<AccountLine>()
            // atrocious custom CSV parsing... I don't want to pull an external library for this, I'll just recode a clean CSV parser one day
            val currentLine = Array(5) { "" }
            var header = true
            file.forEachLine { line ->
                if (header) {
                    header = false
                    return@forEachLine
                }
                if (currentLine[0].isEmpty()) {
                    val idx = line.indexOf('"')
                    val split = line.substring(5, idx).split(',')
                    currentLine[0] = split[0]
                    currentLine[1] = split[2]
                    currentLine[2] = split[3]
                    currentLine[3] = split[4]
                    if (idx == -1) {
                        currentLine[4] = split[5]
                        results += create(currentLine)
                        currentLine.fill("")
                    } else {
                        if (!line.endsWith('"')) {
                            currentLine[4] = line.substring(idx + 1)
                        } else {
                            currentLine[4] = line.substring(idx + 1, line.length - 1)
                            results += create(currentLine)
                            currentLine.fill("")
                        }
                    }
                } else {
                    if (!line.endsWith('"')) {
                        currentLine[4] += "\n" + line
                    } else {
                        currentLine[4] += "\n" + line.substring(0, line.length - 1)
                        results += create(currentLine)
                        currentLine.fill("")
                    }
                }
            }
            return results
        }
    }
}
