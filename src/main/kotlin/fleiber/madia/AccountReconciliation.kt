package fleiber.madia

import fleiber.levenshteinDistance
import fleiber.parseInt
import java.time.LocalDate
import kotlin.io.path.Path
import kotlin.io.path.readLines
import kotlin.math.abs


fun main(args: Array<String>) {
    if (args.size != 3) error("Usage: AccountReconciliation path_to_FR_extract_csv path_to_ressources_db_extract fiscal_year")

    val extractFile = Path(args[0])
    val dbResourcesFile = Path(args[1])
    val fiscalYear = args[2].toInt()

    println("\nReading bank account lines from $extractFile...\n")
    val accountLines = AccountLine.loadCsv(extractFile)
    println(accountLines.joinToString("\n"))

    println("\nReading resource reconciliation lines from $dbResourcesFile...\n")
    val resourceLines = dbResourcesFile.readLines().let { it.subList(1, it.size) }.map { line ->
        val split = line.split('\t')
        AccountLine(
            LocalDate.of(split[1].parseInt(6, 10), split[1].parseInt(3, 5), split[1].parseInt(0, 2)),
            when (split[2]) {
                "VIREMENT" -> AccountLineSubCategory.VIREMENT_RECU
                "PRELEVEMENT" -> AccountLineSubCategory.PRELEVEMENT
                "DEPOT CHEQUE" -> AccountLineSubCategory.DEPOT_CHEQUE
                "DEPOT ESPECES" -> AccountLineSubCategory.DEPOT_ESPECES
                else -> error(line)
            },
            Float.NaN, split[5].toFloat(),
            arrayOf(split[3], split[4]).filter { it.isNotBlank() }.joinToString(" ")
        )
    }
    println(resourceLines.joinToString("\n"))

    val remainingAccountLines = accountLines
        .filter { it.fiscalYear == fiscalYear && it.subCategory.category === AccountLineCategory.DON }
        .groupBy { if (it.subCategory !== AccountLineSubCategory.PRELEVEMENT) it else it.date }
        .values
        .mapTo(mutableListOf()) { list -> if (list.size == 1) list.single() else AccountLine(list[0].date, AccountLineSubCategory.PRELEVEMENT, Float.NaN, list.sumOf { it.credit.toDouble() }.toFloat(), list.joinToString("    ") { it.detailsText }) }
    val matchedLines = mutableListOf<Pair<AccountLine, AccountLine>>()
    val unmatchedResourceLines = mutableListOf<AccountLine>()

    resourceLines.forEach { line ->
        val candidates = remainingAccountLines.filter { it.subCategory === line.subCategory && abs(it.date.toEpochDay() - line.date.toEpochDay()) < 7 && it.credit == line.credit }
        if (candidates.isEmpty()) {
            unmatchedResourceLines += line
            return@forEach
        }

        val best = candidates.minByOrNull { candidate ->
            // very crude ranking based on date diff and string distance
            val txt = line.detailsText.replace(" ", "").replace("SOCIETE", "").replace("HOTELIERE", "").replace("HOTEL", "")
            val candidateTxt = candidate.detailsText.let {
                if ("\nDE: " !in it) it
                else it.substringAfter("\nDE: ").substringBefore("\nMOTIF: ").replace("SOCIETE", "").replace("HOTELIERE", "").replace("HOTEL", "")
            }
            abs(candidate.date.toEpochDay() - line.date.toEpochDay()) - (if (txt in candidateTxt || candidateTxt in txt) 5.0 else 0.0) - 100.0 / (1 + levenshteinDistance(candidateTxt, txt))
        }!!
        matchedLines += line to best
        remainingAccountLines -= best
    }

    println("\nMatched:\n${matchedLines.joinToString("\n") { "${it.first.toString().padEnd(140)}  ${it.second.date.toString().padEnd(12)} / ${it.second.detailsText.replace("\n", "    ")}" } }")
    println("\nUnmatched:\n${unmatchedResourceLines.joinToString("\n")}")
    println("\nRemaining:\n${remainingAccountLines.joinToString("\n")}")
}
