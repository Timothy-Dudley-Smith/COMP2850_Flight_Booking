package com.flightsystem.service

class TicketPdfService {

    fun generateTicketPdf(
        bookingId: String,
        passengerName: String,
        route: String,
        date: String,
        seats: String,
        total: Double
    ): ByteArray {
        val lines = listOf(
            "ASTRAEUS AIRWAYS",
            "ELECTRONIC TICKET / BOARDING DOCUMENT",
            "================================================",
            "Passenger Name   : $passengerName",
            "Booking Ref      : $bookingId",
            "Route            : $route",
            "Travel Date      : $date",
            "Seat Assignment  : $seats",
            "Total Paid       : £${"%.2f".format(total)}",
            "================================================",
            "Important:",
            "Please arrive at the airport at least 2 hours",
            "before departure and bring a valid ID/passport.",
            "",
            "Thank you for booking with us!"
        )

        val contentStream = buildString {
            append("BT\n")
            append("/F1 18 Tf\n")
            append("50 780 Td\n")
            append("(${escapePdfText(lines[0])}) Tj\n")
            append("/F1 12 Tf\n")
            append("0 -30 Td\n")

            for (i in 1 until lines.size) {
                append("(${escapePdfText(lines[i])}) Tj\n")
                append("0 -18 Td\n")
            }

            append("ET")
        }

        val objects = mutableListOf<String>()

        objects += "<< /Type /Catalog /Pages 2 0 R >>"
        objects += "<< /Type /Pages /Kids [3 0 R] /Count 1 >>"
        objects += "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 5 0 R /Resources << /Font << /F1 4 0 R >> >> >>"
        objects += "<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"
        objects += "<< /Length ${contentStream.toByteArray(Charsets.UTF_8).size} >>\nstream\n$contentStream\nendstream"

        val pdf = StringBuilder()
        pdf.append("%PDF-1.4\n")

        val offsets = mutableListOf<Int>()
        for ((index, obj) in objects.withIndex()) {
            offsets += pdf.toString().toByteArray(Charsets.UTF_8).size
            pdf.append("${index + 1} 0 obj\n")
            pdf.append(obj)
            pdf.append("\nendobj\n")
        }

        val xrefOffset = pdf.toString().toByteArray(Charsets.UTF_8).size
        pdf.append("xref\n")
        pdf.append("0 ${objects.size + 1}\n")
        pdf.append("0000000000 65535 f \n")
        for (offset in offsets) {
            pdf.append(String.format("%010d 00000 n \n", offset))
        }

        pdf.append("trailer\n")
        pdf.append("<< /Size ${objects.size + 1} /Root 1 0 R >>\n")
        pdf.append("startxref\n")
        pdf.append(xrefOffset)
        pdf.append("\n%%EOF")

        return pdf.toString().toByteArray(Charsets.UTF_8)
    }

    private fun escapePdfText(text: String): String {
        return text
            .replace("\\", "\\\\")
            .replace("(", "\\(")
            .replace(")", "\\)")
    }
}