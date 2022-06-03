package fleiber.madia

import com.itextpdf.kernel.geom.Vector
import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.canvas.parser.EventType
import com.itextpdf.kernel.pdf.canvas.parser.PdfCanvasProcessor
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo
import com.itextpdf.kernel.pdf.canvas.parser.listener.IEventListener
import java.nio.file.Path
import java.time.LocalDate
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeLines
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.roundToInt


fun main(args: Array<String>) {
    if (args.size != 1) error("Usage: MergeAccountStatementPdfs path_to_folder_with_pdfs")

    val folder = Path.of(args[0])
    val pdfFiles = folder.listDirectoryEntries("*.pdf").sorted()
    println("Will parse ${pdfFiles.size} PDFs from $folder")

    var firstFormat: StatementPdfFormat? = null
    var lastFormat: StatementPdfFormat? = null
    val mergedLines = mutableListOf<AccountLine>()
    pdfFiles.forEach { file ->
        val format = StatementPdfFormat.create(file.name).also {
            if (firstFormat == null) firstFormat = it
            lastFormat = it
        }
        val parsingListener = PdfParsingListener()
        val parser = PdfCanvasProcessor(parsingListener)
        val lines = mutableListOf<AccountLine>()
        PdfDocument(PdfReader(file.inputStream())).use { pdfDoc ->
            println("\nParsing ${file.name} with ${pdfDoc.numberOfPages} pages...\n")
            for (p in 1..pdfDoc.numberOfPages) {
                parser.reset()
                parsingListener.reset()
                parser.processPageContent(pdfDoc.getPage(p))
                format.extractLines(parsingListener, lines)
            }
        }
        println(lines.joinToString("\n"))
        mergedLines += lines
    }

    // Export result as CSV
    folder.resolve("Relevés ${firstFormat!!.accountKey} ${firstFormat!!.date.format(DateTimeFormatter.ofPattern("yyyyMM"))}-${lastFormat!!.date.format(DateTimeFormatter.ofPattern("yyyyMM"))}_${ZonedDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"))}.csv")
        .writeLines(listOf(AccountLine.CSV_HEADER) + mergedLines.map { it.toCsv() })
}

private class StatementPdfFormat(
    val accountKey: String,
    val date: LocalDate,
    val detectionColumnStarts: FloatArray, // Detect interesting lines by exact absciss of first two columns which are left-justified
    val otherColumnStarts: FloatArray,  // Approximate starting absciss of last columns, since they can be right-justified
    val dateColumn: Int,
    val detailsColumn: Int,
    val detailsFollowingLinesColumn: Int,
    val debitColumn: Int,
    val creditColumn: Int,
) {

    private fun createAccountLine(array: Array<String>) = AccountLine.fromBankStatement(date, array[dateColumn], array[detailsColumn], array[debitColumn], array[creditColumn])

    fun extractLines(parsingListener: PdfParsingListener, lines: MutableList<AccountLine>) {
        var prevLine: Array<String>? = null
        parsingListener.extract(detectionColumnStarts, otherColumnStarts) { line ->
            if (line[dateColumn].isEmpty()) {
                prevLine!![detailsColumn] += "\n" + line[detailsFollowingLinesColumn]
            } else {
                prevLine?.also { lines += createAccountLine(it) }
                prevLine = line
            }
        }
        prevLine?.also { lines += createAccountLine(it) }
    }

    companion object {
        private val sgFrFileNameRegexp = """releve_\d{11}_(\d{8})\.pdf""".toRegex()
        private val sgMgFileNameRegexp = """EXT_(\d{8})_\d{5}_\d{11}_M0\d{1,2}\.pdf""".toRegex()
        private val dateFormat = DateTimeFormatter.ofPattern("yyyyMMdd")

        fun create(fileName: String): StatementPdfFormat =
            sgFrFileNameRegexp.matchEntire(fileName)?.let {
                StatementPdfFormat("FR",
                    LocalDate.parse(it.groupValues[1], dateFormat),
                    floatArrayOf(31.2f, 124.08f), floatArrayOf(400.0f, 500.0f),
                    dateColumn = 0, detailsColumn = 1, detailsFollowingLinesColumn = 1, debitColumn = 2, creditColumn = 3
                )
            } ?: sgMgFileNameRegexp.matchEntire(fileName)?.let {
                StatementPdfFormat("MG",
                    LocalDate.parse(it.groupValues[1], dateFormat),
                    floatArrayOf(37.5f, 73.02f, 90.03f), floatArrayOf(354.81f, 390.0f, 490.0f),
                    dateColumn = 0, detailsColumn = 1, detailsFollowingLinesColumn = 2, debitColumn = 4, creditColumn = 5
                )
            } ?: error("Unknown file name format: $fileName")
    }
}

/**
 * Inspired from itext's LocationTextExtractionStrategy and associated classes.
 *
 * Subscribes to the processed text elements, stores them along with their position,
 * and when a page is finished, the extract function groups them by line/words to make sense of the mess.
 */
private class PdfParsingListener : IEventListener {

    private val textChunks = mutableListOf<TextChunk>()

    override fun getSupportedEvents() = setOf(EventType.RENDER_TEXT)

    override fun eventOccurred(data: IEventData?, type: EventType) {
        val renderInfo = data as TextRenderInfo
        check(renderInfo.rise == 0.0f) // check no super/subscript render operations
        val segment = renderInfo.baseline
        val startPoint = segment.startPoint
        val endPoint = segment.endPoint
        if (startPoint != endPoint /* ignore marks */)
            textChunks += TextChunk(renderInfo.text, startPoint, endPoint, renderInfo.singleSpaceWidth)
    }

    fun extract(detectionColumnStarts: FloatArray, otherColumnStarts: FloatArray, callback: (line: Array<String>) -> Unit) {
        textChunks.sortWith(TextChunk.COMPARATOR)

        val sb = StringBuilder()
        val line = Array(detectionColumnStarts.size + otherColumnStarts.size) { "" }
        var prevColumnIdx = -1
        var prevChunk: TextChunk? = null
        for (chunk in textChunks) {
            if (prevChunk != null && !chunk.sameLine(prevChunk)) {
                // Starting new line: finish previous line if one was active
                if (prevColumnIdx != -1) {
                    line[prevColumnIdx] = sb.toString()
                    sb.setLength(0)
                    callback(line.copyOf())
                    line.fill("")
                    prevColumnIdx = -1
                }
                prevChunk = null
            }

            val x = chunk.startLocation[0]
            val detectionColumnIdx = detectionColumnStarts.binarySearch(x)
            if (prevChunk == null) {
                if (detectionColumnIdx >= 0) {
                    // Only start new active line if first chunk exactly starts on one of the detection columns
                    sb.append(chunk.text)
                    prevColumnIdx = detectionColumnIdx
                }
            } else if (prevColumnIdx >= 0) {
                val columnIdx = when {
                    detectionColumnIdx >= 0 -> detectionColumnIdx // start new detectable column
                    x < otherColumnStarts[0] -> prevColumnIdx     // continue prev detectable column
                    else -> otherColumnStarts.binarySearch(x).let { if (it >= 0) it else -it - 2 } + detectionColumnStarts.size // detect other column
                }
                if (columnIdx != prevColumnIdx) {
                    // Finish prev column if starting a new one
                    line[prevColumnIdx] = sb.toString()
                    sb.setLength(0)
                    prevColumnIdx = columnIdx
                } else if (chunk.isAtWordBoundary(prevChunk) &&
                          (chunk.text.isEmpty() || chunk.text[0] != ' ') &&
                          (prevChunk.text.isEmpty() || prevChunk.text.last() != ' ')
                ) {
                    // Add a space if chunk is far enough from previous chunk
                    sb.append(' ')
                }
                sb.append(chunk.text)
            }
            prevChunk = chunk
        }
    }

    fun reset() { textChunks.clear() }
}

private class TextChunk(
    val text: String,
    val startLocation: Vector,
    val endLocation: Vector,
    private val charSpaceWidth: Float,
) {

    /** The orientation as a scalar for quick sorting. */
    private val orientationMagnitude: Int
    /** Perpendicular distance to the orientation unit vector (i.e. the Y position in an unrotated coordinate system). */
    private val distPerpendicular: Float
    /** Distance of the start of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system). */
    private val distParallelStart: Float
    /** Distance of the end of the chunk parallel to the orientation unit vector (i.e. the X position in an unrotated coordinate system). */
    private val distParallelEnd: Float

    init {
        val orientationVector = endLocation.subtract(startLocation).normalize()
        this.orientationMagnitude = (atan2(orientationVector[1], orientationVector[0]) * 1000).roundToInt()

        // see http://mathworld.wolfram.com/Point-LineDistance2-Dimensional.html
        // the two vectors we are crossing are in the same plane, so the result will be purely
        // in the z-axis (out of plane) direction, so we just take the I3 component of the result
        this.distPerpendicular = startLocation[0] * orientationVector[1] - startLocation[1] * orientationVector[0]
        this.distParallelStart = orientationVector.dot(startLocation)
        this.distParallelEnd = orientationVector.dot(endLocation)
    }

    fun sameLine(other: TextChunk): Boolean = abs(distPerpendicular - other.distPerpendicular) < MIN_LINE_DISTANCE

    fun isAtWordBoundary(previous: TextChunk): Boolean {
        var dist = distParallelStart - previous.distParallelEnd
        if (dist < 0.0f) {
            dist = previous.distParallelStart - distParallelEnd
            if (dist < 0.0f) return false // chunks intersect
        }
        return dist > charSpaceWidth / 2.0f
    }

    override fun toString(): String = "$text (${startLocation[0]}, ${startLocation[1]})"


    companion object {
        private const val MIN_LINE_DISTANCE = 0.6f // yes, in some PDF some words are not exactly on the same line...
        val COMPARATOR = Comparator<TextChunk> { c1, c2 ->
            c1.orientationMagnitude.compareTo(c2.orientationMagnitude).also { if (it != 0) return@Comparator it }
            val d = c1.distPerpendicular - c2.distPerpendicular
            if (abs(d) >= MIN_LINE_DISTANCE) return@Comparator c1.distPerpendicular.compareTo(c2.distPerpendicular)
            c1.distParallelStart.compareTo(c2.distParallelStart)
        }
    }
}
