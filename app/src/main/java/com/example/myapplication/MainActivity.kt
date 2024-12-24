package com.example.myapplication
import org.json.JSONObject

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext // Import LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.BufferedOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : ComponentActivity() {
    private val REQUEST_CODE_READ_EXTERNAL_STORAGE = 1001

    private val pickAudioLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { uploadFile(it) { response, success ->
            // Handle the upload completion
            isLoading = false
            isResultReady = true
            resultResponse = response
            uploadStatus = if (success) "Файл успешно загружен!" else "Ошибка загрузки!"
        } }
    }

    private var isLoading by mutableStateOf(false)
    private var uploadStatus by mutableStateOf("Готово к загрузке") // "Ready to upload" in Russian
    private var isResultReady by mutableStateOf(false) // Track if the result is ready
    private var resultResponse by mutableStateOf("") // Store the response to show later

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyApplicationTheme {
                Scaffold { innerPadding ->
                    UploadAudioScreen(
                        modifier = Modifier.padding(innerPadding),
                        onUploadClick = {
                            checkPermissions { openFileChooser() }
                        },
                        isLoading = isLoading,
                        uploadStatus = uploadStatus,
                        isResultReady = isResultReady,
                        resultResponse = resultResponse
                    )
                }
            }
        }
    }

    private fun checkPermissions(onPermissionGranted: () -> Unit) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_READ_EXTERNAL_STORAGE)
        } else {
            onPermissionGranted() // Permission already granted
        }
    }

    private fun openFileChooser() {
        pickAudioLauncher.launch("audio/*")
    }

    private fun uploadFile(fileUri: Uri, onUploadComplete: (String, Boolean) -> Unit) {
        val inputStream = contentResolver.openInputStream(fileUri) ?: return

        // Update the UI state
        isLoading = true
        uploadStatus = "Загрузка..." // "Uploading..." in Russian
        isResultReady = false // Track if the result is ready
        resultResponse = "" // Store the response

        Thread {
            try {
                val connection = (URL("https://api-inference.huggingface.co/models/jonatasgrosman/wav2vec2-large-xlsr-53-russian").openConnection() as HttpURLConnection).apply {
                    doOutput = true
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer i deleteted pre publish repo")
                    setRequestProperty("Content-Type", "audio/flac")
                }

                BufferedOutputStream(connection.outputStream).use { os ->
                    val buffer = ByteArray(4096)
                    var bytesRead: Int
                    inputStream.use { fis ->
                        while (fis.read(buffer).also { bytesRead = it } != -1) {
                            os.write(buffer, 0, bytesRead)
                        }
                    }
                }

                // Read the server response
                val responseCode = connection.responseCode
                resultResponse = connection.inputStream.bufferedReader().use { it.readText() }

                runOnUiThread {
                    isLoading = false
                    isResultReady = true // Set to true when the result is ready
                    uploadStatus = if (responseCode == HttpURLConnection.HTTP_OK) {
                        "Файл успешно загружен!" // "File uploaded successfully!" in Russian
                    } else {
                        "Ошибка загрузки! Код: $responseCode" // "Upload failed! Code:" in Russian
                    }
                    onUploadComplete(resultResponse, responseCode == HttpURLConnection.HTTP_OK)
                }
            } catch (e: IOException) {
                e.printStackTrace()
                runOnUiThread {
                    isLoading = false
                    isResultReady = false // Set to false on error
                    uploadStatus = "Ошибка: ${e.message}" // "Error:" in Russian
                    onUploadComplete(uploadStatus, false)
                }
            } finally {
                inputStream.close()
            }
        }.start()
    }
}

@Composable
fun UploadAudioScreen(
    modifier: Modifier = Modifier,
    onUploadClick: () -> Unit,
    isLoading: Boolean,
    uploadStatus: String,
    isResultReady: Boolean,
    resultResponse: String
) {
    var showResult by remember { mutableStateOf("") } // State to hold the result to be displayed

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(color = Color(0xFFEFEFEF), shape = RoundedCornerShape(8.dp)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = uploadStatus,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black,
            modifier = Modifier.padding(8.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onUploadClick) {
            Text(text = "Загрузить аудио") // "Upload Audio" in Russian
        }
        if (isLoading) {
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator()
        } else if (isResultReady) {
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = {
                // Extract the text from the JSON string
                val extractedText = extractTextFromJson(resultResponse)
                showResult = "Предсказанный текст: $extractedText" // Update this line
            }) {
                Text(text = "Показать результат") // "Show Result" in Russian
            }
        }

        // Display the result below the button
        if (showResult.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = showResult,
                fontSize = 16.sp,
                color = Color.Black,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

// Function to extract text from the JSON response
private fun extractTextFromJson(json: String): String {
    return try {
        val jsonObject = JSONObject(json)
        jsonObject.getString("text")
    } catch (e: Exception) {
        "Ошибка извлечения текста" // "Error extracting text" in Russian
    }
}
