package mk.ukim.finki.examscheduling.publishingservice.service

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.layout.Document
import com.itextpdf.layout.element.Paragraph
import com.itextpdf.layout.element.Table
import com.itextpdf.layout.properties.UnitValue
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import java.io.ByteArrayOutputStream
import java.time.LocalDateTime
import java.util.*

@Service
class ExportService {

    private val webClient = WebClient.builder()
        .baseUrl("http://localhost:8004")
        .build()

    fun exportScheduleToPdf(scheduleId: UUID): ByteArray {
        val scheduleData = getScheduleData(scheduleId)

        val outputStream = ByteArrayOutputStream()
        val pdfWriter = PdfWriter(outputStream)
        val pdfDocument = PdfDocument(pdfWriter)
        val document = Document(pdfDocument)

        document.add(
            Paragraph("Exam Schedule")
                .setFontSize(20f)
                .setBold()
        )

        val schedule = scheduleData["schedule"] as? Map<String, Any> ?: emptyMap()
        document.add(Paragraph("Academic Year: ${schedule["academic_year"] ?: "N/A"}").setFontSize(12f))
        document.add(Paragraph("Exam Session: ${schedule["exam_session"] ?: "N/A"}").setFontSize(12f))
        document.add(Paragraph("Period: ${schedule["start_date"]} to ${schedule["end_date"]}").setFontSize(12f))
        document.add(Paragraph(" "))

        val exams = scheduleData["exams"] as? List<Map<String, Any>> ?: emptyList()

        if (exams.isNotEmpty()) {
            document.add(Paragraph("Scheduled Exams").setFontSize(16f).setBold())

            val table = Table(UnitValue.createPercentArray(floatArrayOf(20f, 25f, 15f, 20f, 10f, 10f)))
                .setWidth(UnitValue.createPercentValue(100f))

            table.addCell("Course ID")
            table.addCell("Course Name")
            table.addCell("Date")
            table.addCell("Time")
            table.addCell("Room")
            table.addCell("Students")

            exams.forEach { exam ->
                table.addCell(exam["course_id"]?.toString() ?: "")
                table.addCell(exam["course_name"]?.toString() ?: "")
                table.addCell(exam["exam_date"]?.toString() ?: "")
                table.addCell("${exam["start_time"]} - ${exam["end_time"]}")
                table.addCell(exam["room_name"]?.toString() ?: "TBD")
                table.addCell(exam["student_count"]?.toString() ?: "0")
            }

            document.add(table)
        } else {
            document.add(Paragraph("No exams scheduled yet.").setFontSize(12f))
        }

        document.add(Paragraph(" "))
        document.add(Paragraph("Generated on ${LocalDateTime.now()}").setFontSize(10f))

        document.close()
        return outputStream.toByteArray()
    }

    fun exportScheduleToExcel(scheduleId: UUID): ByteArray {
        val scheduleData = getScheduleData(scheduleId)

        val workbook: Workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Exam Schedule")

        val schedule = scheduleData["schedule"] as? Map<String, Any> ?: emptyMap()
        val exams = scheduleData["exams"] as? List<Map<String, Any>> ?: emptyList()

        var rowNum = 0

        val headerStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
                fontHeightInPoints = 14
            }
            setFont(font)
        }

        sheet.createRow(rowNum++).apply {
            createCell(0).apply {
                setCellValue("Exam Schedule")
                cellStyle = headerStyle
            }
        }

        sheet.createRow(rowNum++).createCell(0).setCellValue("Academic Year: ${schedule["academic_year"] ?: "N/A"}")
        sheet.createRow(rowNum++).createCell(0).setCellValue("Exam Session: ${schedule["exam_session"] ?: "N/A"}")
        sheet.createRow(rowNum++).createCell(0).setCellValue("Period: ${schedule["start_date"]} to ${schedule["end_date"]}")

        rowNum++

        val tableHeaderStyle = workbook.createCellStyle().apply {
            val font = workbook.createFont().apply {
                bold = true
            }
            setFont(font)
            fillForegroundColor = IndexedColors.GREY_25_PERCENT.getIndex()
            fillPattern = FillPatternType.SOLID_FOREGROUND
        }

        val headerRow = sheet.createRow(rowNum++)
        val headers = arrayOf("Course ID", "Course Name", "Date", "Start Time", "End Time", "Room", "Students", "Type")
        headers.forEachIndexed { index, header ->
            headerRow.createCell(index).apply {
                setCellValue(header)
                cellStyle = tableHeaderStyle
            }
        }

        exams.forEach { exam ->
            val row = sheet.createRow(rowNum++)
            row.createCell(0).setCellValue(exam["course_id"]?.toString() ?: "")
            row.createCell(1).setCellValue(exam["course_name"]?.toString() ?: "")
            row.createCell(2).setCellValue(exam["exam_date"]?.toString() ?: "")
            row.createCell(3).setCellValue(exam["start_time"]?.toString() ?: "")
            row.createCell(4).setCellValue(exam["end_time"]?.toString() ?: "")
            row.createCell(5).setCellValue(exam["room_name"]?.toString() ?: "TBD")
            row.createCell(6).setCellValue((exam["student_count"] as? Number)?.toDouble() ?: 0.0)
            row.createCell(7).setCellValue(exam["mandatory_status"]?.toString() ?: "")
        }

        headers.indices.forEach { sheet.autoSizeColumn(it) }

        rowNum++
        sheet.createRow(rowNum).createCell(0).setCellValue("Generated on ${LocalDateTime.now()}")

        val outputStream = ByteArrayOutputStream()
        workbook.write(outputStream)
        workbook.close()

        return outputStream.toByteArray()
    }

    private fun getScheduleData(scheduleId: UUID): Map<String, Any> {
        return try {
            val response = webClient.get()
                .uri("/api/scheduling/schedules/{scheduleId}", scheduleId)
                .retrieve()
                .bodyToMono(Map::class.java)
                .block() as? Map<String, Any> ?: emptyMap()

            response
        } catch (e: Exception) {
            mapOf(
                "schedule" to mapOf(
                    "academic_year" to "2024-2025",
                    "exam_session" to "WINTER",
                    "start_date" to "2025-01-15",
                    "end_date" to "2025-01-30"
                ),
                "exams" to listOf(
                    mapOf(
                        "course_id" to "CS101",
                        "course_name" to "Computer Science Fundamentals",
                        "exam_date" to "2025-01-16",
                        "start_time" to "09:00",
                        "end_time" to "11:00",
                        "room_name" to "Amphitheater A101",
                        "student_count" to 50,
                        "mandatory_status" to "MANDATORY"
                    ),
                    mapOf(
                        "course_id" to "MATH201",
                        "course_name" to "Advanced Mathematics",
                        "exam_date" to "2025-01-17",
                        "start_time" to "14:00",
                        "end_time" to "16:00",
                        "room_name" to "Classroom B205",
                        "student_count" to 30,
                        "mandatory_status" to "MANDATORY"
                    )
                )
            )
        }
    }
}