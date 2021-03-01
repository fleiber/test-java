package fleiber.comptes

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.nio.file.Files
import java.nio.file.Path
import java.text.NumberFormat
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.writeText

/**
 * Small script merging Grisbi extracts in something more usable.
 */
@ExperimentalPathApi
fun main(args: Array<String>) {
    val folder = Path.of(args[0])
    val outputFormat = CSVFormat.newFormat('\t').withQuote('"').withRecordSeparator('\n').withHeader("Nom du compte", "Date", "Tiers", "Montant", "Solde", "Catégorie", "Sous-catégories", "Remarques")
    val outputBuffer = StringBuilder(1 shl 16)
    val outputPrinter = outputFormat.print(outputBuffer)
    val numberFormat = NumberFormat.getNumberInstance().apply {
        isGroupingUsed = true
    }
    fun parseNumber(str: String) = if (str == "--9223372036854775808.0") 0.0 else numberFormat.parse(str).toDouble()

    Files.list(folder).forEach { accountFile ->
        val fileName = accountFile.fileName.toString()
        if (!fileName.startsWith("Comptes-") || !fileName.endsWith(".csv")) return@forEach
        println("Parsing $fileName...")

        val account = fileName.substringAfter("Comptes-").substringBefore(".csv")
        val parser = CSVParser.parse(accountFile, Charsets.UTF_8, CSVFormat.newFormat('\t').withFirstRecordAsHeader().withQuote('"').withTrailingDelimiter())

        try {
            var currentCounterparty = ""
            var currentDate = ""
            var currentBalance = 0.0
            var currentComments = ""
            for (row in parser) {
                try {
                    val credit = parseNumber(row["Crédit"])
                    val debit = parseNumber(row["Débit"])
                    val category = row["Catégorie"]
                    val subCategory = row["Sous-catégories"]
                    val comments = row["Remarques"]
                    if (row["Ventilation"] == "V") {
                        outputPrinter.printRecord(account, currentDate, currentCounterparty, credit - debit, currentBalance, category, subCategory, if (comments.isEmpty()) currentComments else comments)
                    } else {
                        val balance = parseNumber(row["Solde"])
                        val counterparty = row["Tiers"].let { if (it == "Aucun tiers défini") "" else it }
                        if (counterparty.startsWith("Solde initial")) {
                            if (balance != 0.0) outputPrinter.printRecord(account, "01/01/2005", "", 0.0, balance, "Init", "", "")
                            continue
                        }
                        val date = row["Date"]
                        if (category == "Opération ventilée") {
                            currentCounterparty = counterparty
                            currentDate = date
                            currentBalance = balance
                            currentComments = comments
                            continue
                        }
                        outputPrinter.printRecord(account, date, counterparty, credit - debit, balance, category, subCategory, comments)
                    }
                } catch (e: Exception) {
                    println("Error when parsing $fileName line ${parser.currentLineNumber} $row:")
                    e.printStackTrace()
                }
            }
        } catch (e: Exception) {
            println("Error when parsing $fileName line ${parser.currentLineNumber}:")
            e.printStackTrace()
        }
    }
    outputPrinter.close()
    folder.resolve("All.csv").writeText(outputBuffer.toString())
}
