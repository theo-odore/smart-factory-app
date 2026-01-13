package com.example.smartfactory

import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStream
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    private lateinit var tvProduction: TextView
    private lateinit var tvDowntime: TextView
    private lateinit var tvEfficiency: TextView
    private lateinit var etProdCount: TextInputEditText
    private lateinit var etDowntime: TextInputEditText
    private lateinit var btnAnalyze: Button
    private lateinit var btnMockData: Button
    private lateinit var btnUploadCsv: Button
    private lateinit var btnExportPdf: Button
    private lateinit var chartView: SimpleLineChart
    
    // Activity Result Launcher for CSV picking
    private val csvPickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { parseCsv(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialize Views
        tvProduction = findViewById(R.id.tvProduction)
        tvDowntime = findViewById(R.id.tvDowntime)
        tvEfficiency = findViewById(R.id.tvEfficiency)
        etProdCount = findViewById(R.id.etProdCount)
        etDowntime = findViewById(R.id.etDowntime)
        btnAnalyze = findViewById(R.id.btnAnalyze)
        btnMockData = findViewById(R.id.btnMockData)
        btnUploadCsv = findViewById(R.id.btnUploadCsv)
        btnExportPdf = findViewById(R.id.btnExportPdf)
        chartView = findViewById(R.id.chartView)

        btnAnalyze.setOnClickListener { calculateMetrics() }
        btnMockData.setOnClickListener { generateMockData() }
        
        btnUploadCsv.setOnClickListener {
            Toast.makeText(this, "Opening File Picker...", Toast.LENGTH_SHORT).show()
            // Launch file picker for CSV
            csvPickerLauncher.launch("*/*") 
        }
        
        btnExportPdf.setOnClickListener {
            exportPdf()
        }
    }
    
    private fun parseCsv(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).use { reader ->
                    // Read first line (ignore header if any, assume simple "production,downtime")
                    // If your CSV has header, readLine() twice.
                    var line = reader.readLine()
                    
                    // Simple heuristic: check if line contains digits, if not read next
                    if (line != null && !line.any { it.isDigit() }) {
                        line = reader.readLine()
                    }

                    if (line != null) {
                        val tokens = line.split(",")
                        if (tokens.size >= 2) {
                            val prod = tokens[0].trim()
                            val down = tokens[1].trim()
                            
                            etProdCount.setText(prod)
                            etDowntime.setText(down)
                            calculateMetrics()
                            Toast.makeText(this, "CSV Loaded Successfully", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error reading CSV", Toast.LENGTH_SHORT).show()
        }
    }

    private fun generateMockData() {
        val randProd = Random.nextInt(800, 1500)
        val randDown = Random.nextInt(0, 120)
        
        etProdCount.setText(randProd.toString())
        etDowntime.setText(randDown.toString())
        calculateMetrics()
    }

    private fun calculateMetrics() {
        val prodStr = etProdCount.text.toString()
        val downStr = etDowntime.text.toString()

        if (prodStr.isNotEmpty() && downStr.isNotEmpty()) {
            val production = prodStr.toInt()
            val downtime = downStr.toInt()

            var efficiency = 100.0 - (downtime * 0.5)
            if (efficiency < 0) efficiency = 0.0
            if (efficiency > 100) efficiency = 100.0

            tvProduction.text = production.toString()
            tvDowntime.text = "${downtime}m"
            tvEfficiency.text = "${efficiency.toInt()}%"
            
            // Random history for chart
            val history = List(3) { Random.nextInt(800, 1500).toFloat() } + production.toFloat()
            chartView.setData(history)
        }
    }
    
    private fun exportPdf() {
        val     prod = tvProduction.text.toString()
        if (prod == "0" || prod.isEmpty()) {
            Toast.makeText(this, "No data to export!", Toast.LENGTH_SHORT).show()
            return
        }

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size in points
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas
        val paint = Paint()

        // Background
        paint.color = Color.WHITE
        canvas.drawRect(0f, 0f, 595f, 842f, paint)

        // Title
        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Smart Factory Report", 50f, 80f, paint)

        // Content
        paint.textSize = 14f
        paint.isFakeBoldText = false
        val down = tvDowntime.text.toString()
        val eff = tvEfficiency.text.toString()
        
        canvas.drawText("Production Count: $prod", 50f, 130f, paint)
        canvas.drawText("Downtime: $down", 50f, 160f, paint)
        canvas.drawText("Efficiency: $eff", 50f, 190f, paint)
        
        canvas.drawText("Generated by Smart Factory App v1.0", 50f, 800f, paint)
        
        // --- Draw Chart ---
        canvas.save()
        canvas.translate(50f, 250f) // Position below text
        
        // Temporarily resize chart for PDF page width (approx 500pts)
        val originalInternalWidth = chartView.width
        val originalInternalHeight = chartView.height
        
        // Scale down to fit A4 width
        val pdfChartWidth = 500f
        val pdfChartHeight = 300f
        val scaleX = pdfChartWidth / originalInternalWidth
        val scaleY = pdfChartHeight / originalInternalHeight
        
        canvas.scale(scaleX, scaleY)
        
        // Switch to Light Mode for printing
        chartView.setTheme(isDark = false)
        chartView.draw(canvas)
        chartView.setTheme(isDark = true) // Revert
        
        canvas.restore()

        pdfDocument.finishPage(page)

        // Save to Downloads using MediaStore
        try {
            val fileName = "SmartFactory_Report_${System.currentTimeMillis()}.pdf"
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }

            val uri = contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
            
            if (uri != null) {
                val outputStream: OutputStream? = contentResolver.openOutputStream(uri)
                if (outputStream != null) {
                    pdfDocument.writeTo(outputStream)
                    outputStream.close()
                    Toast.makeText(this, "PDF Saved to Downloads", Toast.LENGTH_LONG).show()
                }
            } else {
                 Toast.makeText(this, "Error creating file", Toast.LENGTH_SHORT).show()
            }
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to export PDF", Toast.LENGTH_SHORT).show()
        }

        pdfDocument.close()
    }
}
