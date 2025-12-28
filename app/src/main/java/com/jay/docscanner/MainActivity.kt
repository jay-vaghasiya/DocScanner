package com.jay.docscanner

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_JPEG
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF
import com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult
import com.jay.docscanner.ui.theme.DocScannerTheme
import java.io.File
import java.io.FileOutputStream
import java.io.IOException



class MainActivity : ComponentActivity() {

    // Scanner configuration - Lazy initialization for better performance
    private val scannerOptions by lazy {
        GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(500)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()
    }

    private val scanner by lazy {
        GmsDocumentScanning.getClient(scannerOptions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge display for modern Android look
        enableEdgeToEdge()

        setContent {
            DocScannerTheme(
                dynamicColor = true // Enable Material You dynamic colors on Android 12+
            ) {
                // Use Surface instead of Scaffold at root level
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DocScannerApp(
                        onSavePdf = { filePath, fileName, onSuccess, onError ->
                            savePdfToDocuments(filePath, fileName, onSuccess, onError)
                        }
                    )
                }
            }
        }
    }

    /**
     * Saves the scanned PDF to the Documents directory
     * Uses callbacks for better Compose integration
     */
    private fun savePdfToDocuments(
        filePath: String?,
        fileName: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (filePath == null) {
            onError("No PDF generated yet")
            return
        }

        // Check storage permission for Android 9 and below
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                    STORAGE_PERMISSION_REQUEST_CODE
                )
                onError("Storage permission required")
                return
            }
        }

        try {
            val sourceFile = File(filePath)
            val documentsDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOCUMENTS
            )

            // Create documents directory if it doesn't exist
            if (!documentsDir.exists()) {
                documentsDir.mkdirs()
            }

            val destFile = File(documentsDir, "$fileName.pdf")
            sourceFile.copyTo(destFile, overwrite = true)

            onSuccess()
            Toast.makeText(
                this,
                "PDF saved to Documents/$fileName.pdf",
                Toast.LENGTH_LONG
            ).show()
        } catch (e: IOException) {
            onError("Failed to save PDF: ${e.message}")
        }
    }

    companion object {
        private const val STORAGE_PERMISSION_REQUEST_CODE = 100
    }
}

/**
 * Main App Composable - Manages navigation and scanner state
 */
@Composable
fun DocScannerApp(
    onSavePdf: (String?, String, () -> Unit, (String) -> Unit) -> Unit
) {
    val navController = rememberNavController()

    // State for scanned documents
    var scannedImageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var pdfFilePath by remember { mutableStateOf<String?>(null) }

    // Scanner result launcher
    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val scanResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)

            // Extract image URIs from scanned pages
            scannedImageUris = scanResult?.pages?.map {
                it.imageUri.toString()
            } ?: emptyList()

            // Handle PDF result with unique filename
            scanResult?.pdf?.let { pdf ->
                val context = navController.context
                val pdfFile = File(
                    context.filesDir,
                    "scan_${System.currentTimeMillis()}.pdf"
                )
                context.contentResolver.openInputStream(pdf.uri)?.use { input ->
                    FileOutputStream(pdfFile).use { output ->
                        input.copyTo(output)
                    }
                }
                pdfFilePath = pdfFile.absolutePath
            }
        }
    }

    // Navigation Graph with all screens
    DocScannerNavGraph(
        navController = navController,
        onScanDocument = {
            // Start document scanning
            val context = navController.context as ComponentActivity
            GmsDocumentScanning.getClient(
                GmsDocumentScannerOptions.Builder()
                    .setScannerMode(SCANNER_MODE_FULL)
                    .setGalleryImportAllowed(true)
                    .setPageLimit(500)
                    .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
                    .build()
            ).getStartScanIntent(context)
                .addOnSuccessListener { intentSender ->
                    scannerLauncher.launch(
                        IntentSenderRequest.Builder(intentSender).build()
                    )
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        context,
                        "Failed to start scanner: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        },
        scannedImageUris = scannedImageUris,
        pdfFilePath = pdfFilePath,
        onClearScannedData = {
            // Clear state after successful save
            scannedImageUris = emptyList()
            pdfFilePath = null
        },
        onSavePdf = onSavePdf
    )
}