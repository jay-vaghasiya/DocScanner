package com.jay.docscanner

import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandIn
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
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

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val options = GmsDocumentScannerOptions.Builder()
            .setScannerMode(SCANNER_MODE_FULL)
            .setGalleryImportAllowed(true)
            .setPageLimit(500)
            .setResultFormats(RESULT_FORMAT_JPEG, RESULT_FORMAT_PDF)
            .build()

        val scanner = GmsDocumentScanning.getClient(options)
        setContent {
            DocScannerTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    contentColor = MaterialTheme.colorScheme.background,
                    topBar = {
                        androidx.compose.material3.TopAppBar(
                            title = {
                                Text(
                                    text = "Document Scanner"
                                )
                            }
                        )

                    },
                ) { padding ->

                    var imageUris by remember {
                        mutableStateOf<List<Uri>>(emptyList())
                    }
                    var showDialog by remember { mutableStateOf(false) }
                    var fileName by remember { mutableStateOf("") }
                    var pdfFilePath by remember { mutableStateOf<String?>(null) }

                    val scannerLauncher =
                        rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.StartIntentSenderForResult(),
                            onResult = { resultActivity ->
                                if (resultActivity.resultCode == RESULT_OK) {
                                    val result =
                                        GmsDocumentScanningResult.fromActivityResultIntent(
                                            resultActivity.data
                                        )
                                    imageUris = result?.pages?.map { it.imageUri } ?: emptyList()

                                    result?.pdf?.let { pdf ->
                                        val fos = FileOutputStream(File(filesDir, "scan.pdf"))
                                        contentResolver.openInputStream(pdf.uri)?.use {
                                            it.copyTo(fos)
                                        }
                                        pdfFilePath = File(filesDir, "scan.pdf").absolutePath
                                    }
                                }

                            })

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding) // Add padding around the entire box
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 80.dp) // Space for the buttons
                            ,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            // Display images in a row with padding and a rounded corner effect
                            LazyVerticalStaggeredGrid(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp), // Add vertical padding around the LazyRow
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                columns = StaggeredGridCells.Fixed(3)
                            ) {
                                items(imageUris) { uri ->
                                    AsyncImage(
                                        model = uri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                }
                            }
                        }

                        // Row to contain buttons at the bottom
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .padding(12.dp), // 12.dp padding around the Row
                            horizontalArrangement = Arrangement.spacedBy(12.dp) // Space between buttons
                        ) {
                            Button(
                                onClick = {
                                    scanner.getStartScanIntent(this@MainActivity)
                                        .addOnSuccessListener {
                                            scannerLauncher.launch(
                                                IntentSenderRequest.Builder(it).build()
                                            )
                                        }.addOnFailureListener {
                                            Toast.makeText(
                                                applicationContext,
                                                it.message,
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                },
                                modifier = Modifier.weight(1f) // Make buttons equal width
                            ) {
                                Text(text = "Scan PDF")
                            }
                            AnimatedVisibility(
                                modifier = Modifier.weight(1f),
                                visible = imageUris.isNotEmpty(),
                                enter = androidx.compose.animation.fadeIn(
                                    animationSpec = tween(durationMillis = 300)
                                ),
                                exit = androidx.compose.animation.fadeOut(
                                    animationSpec = tween(durationMillis = 300)
                                )
                            ) {
                                Button(
                                    onClick = {
                                        showDialog = true
                                    },
                                    modifier = Modifier
                                ) {
                                    Text("Save PDF")
                                }
                            }
                        }

                        // Show dialog if the flag is true

                        AnimatedVisibility(
                            visible = showDialog,
                            enter = expandIn(animationSpec = tween(300)), // 300ms fade-in animation
                            exit = shrinkOut(animationSpec = tween(300))  // 300ms fade-out animation
                        ) {
                            FilenameDialog(
                                filename = fileName,
                                onFilenameChange = { fileName = it },
                                onDismiss = { showDialog = false },
                                onSave = {
                                    savePdf(pdfFilePath, fileName)
                                    showDialog = false // Close dialog after save
                                }
                            )
                        }

                    }


                }
            }
        }
    }

    @Composable
    fun FilenameDialog(
        filename: String,
        onFilenameChange: (String) -> Unit,
        onDismiss: () -> Unit,
        onSave: () -> Unit
    ) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(text = "Enter Filename") },
            text = {
                OutlinedTextField(
                    value = filename,
                    onValueChange = onFilenameChange,
                    label = { Text("Filename") }
                )
            },
            confirmButton = {
                Button(onClick = onSave) {
                    Text("Save")
                }
            },
            dismissButton = {
                Button(onClick = onDismiss) {
                    Text("Cancel")
                }
            }
        )
    }

    private fun savePdf(filePath: String?, fileName: String) {


        if (filePath == null) {
            Toast.makeText(this, "No PDF generated yet", Toast.LENGTH_SHORT).show()
            return
        }


        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.WRITE_EXTERNAL_STORAGE),
                100
            )
            return
        }


        val sourceFile = File(filePath)
        val documentsDir =
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
        val destFile = File(documentsDir, "$fileName.pdf")

        try {
            sourceFile.copyTo(destFile, overwrite = true)
            Toast.makeText(this, "PDF saved to $destFile", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to save PDF: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}

@Composable
@Preview(device = "id:pixel_4a", showBackground = true, backgroundColor = 0xFF3A2F6E)
private fun MainScreenPreview() {
    DocScannerTheme {
        MainActivity()
    }
}