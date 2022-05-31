package fleiber.madia

import fleiber.parseInt
import java.text.DecimalFormat
import java.time.LocalDate


@Suppress("SpellCheckingInspection")
enum class AccountLineType(
    val description: String,
    private val predicate: (firstLine: String, full: String) -> Boolean
) {

    // MG account
    VIREMENT_EUR_MGA          ("Virement EUR/MGA",                { firstLine, _ -> firstLine == "VIRT RECU DE : MADIA" }),

    PROJET_ORPHELINAT_BROUSSE ("Projet orphelinat brousse",       { firstLine, _ -> firstLine == "VIRT FAV: ORPHELINAT DE BROUSS" }),
    PROJET_BETONNIER          ("Projet Bétonnier",                { firstLine, _ -> firstLine == "VIRT FAV: RAZAFINDRAKOTO RENE" }),
    PROJET_FIANAR             ("Projet Fianarantsoa",             { firstLine, _ -> firstLine.replace("VIRT FAV :", "VIRT FAV:") == "VIRT FAV: AIC FIANARANTSOA" }),

    // FR account
    VIREMENT_RECU             ("Virement reçu",                   { firstLine, _ -> "VIR RECU" in firstLine || "VIR INST REC" in firstLine }),
    PRELEVEMENT               ("Prélèvement",                     { firstLine, _ -> "PRLV EUROPEEN EMIS" in firstLine }),
    PRELEVEMENT_FRAIS         ("Prélèvement - Frais",             { firstLine, _ -> firstLine.startsWith("FRAIS SUR PRLV EUROP. EMIS") }),
    DEPOT_CHEQUE              ("Dépôt chèques",                   { firstLine, _ -> firstLine.startsWith("REMISE CHEQUE") }),
    DEPOT_ESPECES             ("Dépôt espèces",                   { firstLine, _ -> firstLine.startsWith("VERSEMENT EXPRESS") }),

    VIREMENT_MG               ("Virement vers compte MG",         { firstLine, full -> if ("VIR INTL EMIS" in firstLine) { check(full.endsWith("CHEZ: BFAVMGMGXXX BFV-STE.GENERALE/TANANAR")) { "Destinataire virement inconnu: $full" }; true } else false }),
    VIREMENT_MG_FRAIS         ("Virement vers compte MG - Frais", { firstLine, _ -> firstLine.startsWith("FRAIS VIR INTL") }),

    // Common
    DEPENSE                   ("Dépense",                         { _, _ -> true }),
    ;


    companion object {
        private val ALL = values()
        fun fromBankStatement(firstLine: String, full: String): AccountLineType = ALL.first { it.predicate(firstLine, full) }
        fun fromDescription(description: String): AccountLineType = ALL.firstOrNull { it.description == description } ?: error("Could not find AccountLineType for $description")
    }
}

data class AccountLine(
    val date: LocalDate,
    val type: AccountLineType,
    val debit: Float,
    val credit: Float,
    val detailsText: String
) {

    fun toCsv() = "$date,${type.description},${debit.formatCsv()},${credit.formatCsv()},\"$detailsText\""

    override fun toString() = date.toString().padEnd(12) +
            ' ' + type.description.padEnd(32) +
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
            val type = AccountLineType.fromBankStatement(firstLine, detailsStr)
            return AccountLine(date, type, debitStr.parseFrFloat(), creditStr.parseFrFloat(), detailsStr)
        }
        private fun String.parseFrFloat() = if (isEmpty()) Float.NaN else replace(".", "").replace(',', '.').toFloat()
        private fun Float.formatCsv() = if (isNaN()) "" else toString()

        @Suppress("SpellCheckingInspection")
        const val CSV_HEADER = "Date,Type,Débit,Crédit,Détails"
        private val AMOUNT_FORMAT = DecimalFormat("###,###.00")
    }
}
