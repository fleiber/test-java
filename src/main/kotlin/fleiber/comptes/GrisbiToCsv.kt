package fleiber.comptes

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import java.nio.file.Path
import javax.xml.parsers.SAXParserFactory
import kotlin.io.path.inputStream


fun main(args: Array<String>) {
    if (args.isEmpty()) error("Usage: GrisbiToCsv grisbi_file_path")
    val filePath = Path.of(args[0])

    val accounts = mutableMapOf<Int, Account>()
    val parties = mutableMapOf<Int, Party>()
    val categories = mutableMapOf<Int, Category>()
    // We could do a unique file parsing and store the transactions in an internal format, which would be cleaner and allow other features,
    // like handling the account balance, but this is just a quick & dirty script
    val saxParser = SAXParserFactory.newDefaultInstance().newSAXParser()
    saxParser.parse(filePath.inputStream(), object : DefaultHandler() {
        override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
//            println("startElement(\"$qName\", \"${(0 until attrs.length).map { attrs.getQName(it) + " = " + attrs.getValue(it) }}\")")
            when (qName) {
                "Account" -> accounts[attrs.getInt("Number")] = Account(attrs["Name"], attrs.getDouble("Initial_balance"))
                "Party" -> parties[attrs.getInt("Nb")] = Party(attrs["Na"])
                "Category" -> categories[attrs.getInt("Nb")] = Category(attrs["Na"])
                "Sub_category" -> categories[attrs.getInt("Nbc")]!!.subCategories[attrs.getInt("Nb")] = attrs["Na"]
            }
        }
    })
    println("Accounts = \n\t${accounts.values.joinToString("\n\t")}")
    println("Parties = \n\t${parties.values.joinToString("\n\t")}")
    println("Categories = \n\t${categories.values.joinToString("\n\t")}")

    saxParser.parse(filePath.inputStream(), object : DefaultHandler() {
        var breakdownComments = mutableMapOf<Int, String>()
        override fun startElement(uri: String, localName: String, qName: String, attrs: Attributes) {
            if (qName == "Transaction") {
                if (attrs["Trt"] != "0") return // inter-account transfer
                if (attrs["Br"] == "1") {
                    breakdownComments[attrs.getInt("Nb")] = attrs["No"]
                    return
                }
                println(accounts[attrs.getInt("Ac")]!!.name +
                        ',' + attrs["Dt"] +
                        ',' + (parties[attrs.getInt("Pa")]?.name ?: "") +
                        ',' + attrs.getDouble("Am") +
                        ',' + (categories[attrs.getInt("Ca")]?.name ?: "") +
                        ',' + (categories[attrs.getInt("Ca")]?.subCategories?.get(attrs.getInt("Sca")) ?: "") +
                        ',' + attrs["No"].ifEmpty { breakdownComments[attrs.getInt("Mo")] ?: "" }.quoted()
                )
            }
        }
    })
}


private fun Attributes.getInt(qName: String) = getValue(qName).toInt()
private fun Attributes.getDouble(qName: String) = getValue(qName).toDouble()
private operator fun Attributes.get(qName: String) = getValue(qName).let { if (it == "(null)") "" else it }
private fun String.quoted() = if (',' in this || '"' in this) '"' + replace("\"", "\"\"") + '"' else this


/**
 * <Account
 *     Name="Compte Courant Joint"
 *     Id="(null)"
 *     Number="1"
 *     Owner="(null)"
 *     Kind="0"                // kind of the account (bank, cash...)
 *     Currency="1"
 *     Path_icon="/usr/local/share/pixmaps/grisbi/gsb-banks-32.png"
 *     Bank="1"
 *     Bank_branch_code="(null)"
 *     Bank_account_number="(null)"
 *     Key="(null)"
 *     Bank_account_IBAN="(null)"
 *     Initial_balance="0.00"
 *     Minimum_wanted_balance="0.00"
 *     Minimum_authorised_balance="0.00"
 *     Closed_account="0"
 *     Show_marked="0"
 *     Show_archives_lines="0"
 *     Lines_per_transaction="3"
 *     Comment="(null)"
 *     Owner_address="(null)"
 *     Default_debit_method="87"
 *     Default_credit_method="86"
 *     Sort_by_method="0"
 *     Neutrals_inside_method="0"
 *     Sort_order="85/86/87"
 *     Ascending_sort="0"
 *     Column_sort="1"
 *     Sorting_kind_column="18-1-3-13-5-6-0"
 *     Bet_use_budget="0" />
 */
data class Account(
    val name: String,
    val initialBalance: Double = 0.0
) {
    override fun toString() = name
}

/**
 * <Party
 *     Nb="1"
 *     Na="MonVendeurPréféré"
 *     Txt="(null)"
 *     Search="(null)"
 *     IgnCase="0"
 *     UseRegex="0" />
 */
data class Party(
    val name: String
) {
    override fun toString() = name
}

/**
 * <Category
 *     Nb="1"
 *     Na="Revenus"
 *     Kd="0" />
 * <Sub_category
 *     Nbc="1"
 *     Nb="1"
 *     Na="Salaire" />
 */
data class Category(
    val name: String,
    val subCategories: MutableMap<Int, String> = mutableMapOf() // in real code we would make a builder
) {
    override fun toString() = "$name ${subCategories.values.sorted()}"
}

/**
 * <Transaction
 *     Ac="1"             // Account number
 *     Nb="123"           // Transaction number
 *     Id="(null)"        // Id (for ofx import)
 *     Dt="02/05/2022"    // Date
 *     Dv="(null)"        // Value date
 *     Cu="1"             // Currency number
 *     Am="1234.56"       // Amount
 *     Exb="0"            // Exchange between account and transaction (if 1 : 1 account_currency = (exchange_rate * amount) transaction_currency)
 *     Exr="0.00"         // Exchange rate
 *     Exf="0.00"         // Exchange fees
 *     Pa="1"             // Party number
 *     Ca="1"             // Category number
 *     Sca="1"            // Sub-category number
 *     Br="0"             // Breakdown
 *     No="(null)"        // Notes
 *     Pn="0"             // Payment method number
 *     Pc="(null)"        // Payment method content
 *     Ma="3"             // Marked transaction (0=nothing, 1=P, 2=T, 3=R)
 *     Ar="0"             // Archive number
 *     Au="1"             // Automatic transaction (0=manual, 1=automatic (scheduled transaction))
 *     Re="123"           // Reconciliation number
 *     Fi="0"             // Financial year number
 *     Bu="0"             // Budget allocation
 *     Sbu="0"            // Sub-budget allocation
 *     Vo="(null)"        // Voucher
 *     Ba="(null)"        // Bank references
 *     Trt="0"            // Transfer transaction number
 *     Mo="0" />          // Mother transaction number
 */
