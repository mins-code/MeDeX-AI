package com.example.healthchatbot

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.healthchatbot.adapter.ChatAdapter
import com.example.healthchatbot.databinding.ActivityMainBinding
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.public.extensions.addModelFromURL
import com.runanywhere.sdk.data.models.SDKEnvironment
import com.runanywhere.sdk.llm.llamacpp.LlamaCppServiceProvider
import kotlinx.coroutines.*
import java.io.PrintWriter
import java.io.StringWriter

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: HealthChatViewModel by viewModels()
    private lateinit var chatAdapter: ChatAdapter

    private val pdfPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processPdf(it) }
    }

    private val documentPicker =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let { processPdf(uri) }
        }

    private val storagePermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            documentPicker.launch("application/pdf")
        } else {
            Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    // Track SDK state
    private var isSDKInitialized = false
    private var isModelDownloaded = false
    private var isModelLoaded = false
    private var initializationJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            logError("UNCAUGHT EXCEPTION", exception)
            runOnUiThread {
                showError("App Error: ${exception.message}")
            }
        }

        try {
            Log.d("HealthChatbot", "=== APP STARTING ===")
            binding = ActivityMainBinding.inflate(layoutInflater)
            setContentView(binding.root)

            setupRecyclerView()
            setupClickListeners()
            observeViewModel()

            // Show initial status
            updateStatus("Health Chatbot Ready - Click Download to start")

            Log.d("HealthChatbot", "MainActivity onCreate completed successfully")

        } catch (e: Exception) {
            logError("onCreate Error", e)
            showError("Failed to initialize app: ${e.message}")
        }
    }

    private fun logError(tag: String, exception: Throwable) {
        val sw = StringWriter()
        val pw = PrintWriter(sw)
        exception.printStackTrace(pw)
        val stackTrace = sw.toString()

        Log.e("HealthChatbot", "=== $tag ===")
        Log.e("HealthChatbot", "Message: ${exception.message}")
        Log.e("HealthChatbot", "Cause: ${exception.cause}")
        Log.e("HealthChatbot", "Stack Trace: $stackTrace")
        Log.e("HealthChatbot", "=== END $tag ===")
    }

    private fun showError(message: String) {
        runOnUiThread {
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            updateStatus("Error: $message")
        }
    }

    private fun updateStatus(message: String) {
        runOnUiThread {
            binding.textViewStatus.text = message
            Log.d("HealthChatbot", "Status: $message")
        }
    }

    private fun setupRecyclerView() {
        try {
            chatAdapter = ChatAdapter()
            binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)
            binding.recyclerViewChat.adapter = chatAdapter
            Log.d("HealthChatbot", "RecyclerView setup completed")
        } catch (e: Exception) {
            logError("RecyclerView Setup Error", e)
        }
    }

    private fun setupClickListeners() {
        try {
            binding.buttonSend.setOnClickListener {
                try {
                    val message = binding.editTextMessage.text.toString().trim()
                    if (message.isNotEmpty()) {
                        Log.d("HealthChatbot", "Sending message: $message")
                        sendMessage(message)
                        binding.editTextMessage.text?.clear()
                    }
                } catch (e: Exception) {
                    logError("Send Button Error", e)
                    showError("Send failed: ${e.message}")
                }
            }

            binding.buttonUpload.setOnClickListener {
                try {
                    Log.d("HealthChatbot", "Upload button clicked")
                    checkStoragePermissionAndUpload()
                } catch (e: Exception) {
                    logError("Upload Button Error", e)
                    showError("Upload failed: ${e.message}")
                }
            }

            binding.buttonDownloadModel.setOnClickListener {
                try {
                    Log.d("HealthChatbot", "Download button clicked")
                    downloadModel()
                } catch (e: Exception) {
                    logError("Download Button Error", e)
                    showError("Download failed: ${e.message}")
                }
            }

            binding.buttonLoadModel.setOnClickListener {
                try {
                    Log.d("HealthChatbot", "Load button clicked")
                    loadModel()
                } catch (e: Exception) {
                    logError("Load Button Error", e)
                    showError("Load failed: ${e.message}")
                }
            }

            Log.d("HealthChatbot", "Click listeners setup completed")
        } catch (e: Exception) {
            logError("Click Listeners Setup Error", e)
        }
    }

    private fun observeViewModel() {
        try {
            lifecycleScope.launch {
                viewModel.messages.collect { messages ->
                    chatAdapter.submitList(messages)
                    if (messages.isNotEmpty()) {
                        binding.recyclerViewChat.smoothScrollToPosition(messages.size - 1)
                    }
                }
            }

            lifecycleScope.launch {
                viewModel.isLoading.collect { isLoading ->
                    binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    binding.buttonSend.isEnabled = !isLoading
                }
            }

            lifecycleScope.launch {
                viewModel.error.collect { error ->
                    error?.let {
                        Toast.makeText(this@MainActivity, it, Toast.LENGTH_LONG).show()
                    }
                }
            }

            Log.d("HealthChatbot", "ViewModel observers setup completed")
        } catch (e: Exception) {
            logError("ViewModel Observer Setup Error", e)
        }
    }

    private suspend fun initializeSDKIfNeeded(): Boolean {
        if (HealthChatbotApplication.isSDKInitialized) {
            Log.d("HealthChatbot", "SDK already initialized")
            return true
        }

        if (HealthChatbotApplication.isInitializing) {
            Log.w("HealthChatbot", "SDK initialization already in progress, waiting...")
            while (HealthChatbotApplication.isInitializing && !HealthChatbotApplication.isSDKInitialized) {
                delay(100)
            }
            return HealthChatbotApplication.isSDKInitialized
        }

        HealthChatbotApplication.isInitializing = true

        return try {
            withContext(Dispatchers.Main) {
                binding.textViewStatus.text = "⏳ Initializing RunAnywhere SDK..."
            }

            Log.d("HealthChatbot", "Starting REAL RunAnywhere SDK initialization...")

            withContext(Dispatchers.IO) {
                try {
                    Log.d("HealthChatbot", "Checking for required serialization dependencies...")
                    try {
                        Class.forName("kotlinx.serialization.json.JsonKt")
                        Class.forName("kotlinx.serialization.json.Json")
                        Log.d("HealthChatbot", "Kotlinx-serialization dependencies found")
                    } catch (serializationMissing: ClassNotFoundException) {
                        Log.e(
                            "HealthChatbot",
                            "Kotlinx-serialization dependencies missing",
                            serializationMissing
                        )
                        throw Exception("Required serialization libraries missing - cannot initialize SDK")
                    }

                    Log.d("HealthChatbot", "Initializing RunAnywhere SDK...")

                    RunAnywhere.initialize(
                        context = applicationContext,
                        apiKey = "dev-api-key",
                        environment = SDKEnvironment.DEVELOPMENT
                    )
                    Log.d("HealthChatbot", "RunAnywhere.initialize() completed")

                    delay(2000)

                    Log.d("HealthChatbot", "Registering LlamaCpp service provider...")
                    LlamaCppServiceProvider.register()
                    Log.d("HealthChatbot", "LlamaCppServiceProvider.register() completed")

                    delay(2000)

                    Log.d("HealthChatbot", "Adding model from URL...")
                    addModelFromURL(
                        url = "https://huggingface.co/prithivMLmods/SmolLM2-360M-GGUF/resolve/main/SmolLM2-360M.Q8_0.gguf",
                        name = "SmolLM2-360M-Q8_0",
                        type = "LLM"
                    )
                    Log.d("HealthChatbot", "addModelFromURL() completed")

                    delay(1000)

                    Log.d("HealthChatbot", "Scanning for downloaded models...")
                    RunAnywhere.scanForDownloadedModels()
                    Log.d("HealthChatbot", "scanForDownloadedModels() completed")

                    true
                } catch (e: Exception) {
                    Log.e("HealthChatbot", "SDK initialization failed", e)
                    false
                }
            }.also { success ->
                withContext(Dispatchers.Main) {
                    if (success) {
                        binding.textViewStatus.text = "✅ RunAnywhere SDK Initialized Successfully!"
                        HealthChatbotApplication.isSDKInitialized = true
                        HealthChatbotApplication.initializationError = null
                        Log.i("HealthChatbot", "REAL SDK initialization completed successfully!")
                    } else {
                        val errorMsg = "RunAnywhere SDK initialization failed"
                        binding.textViewStatus.text = "❌ $errorMsg"
                        HealthChatbotApplication.initializationError = errorMsg
                        Log.e("HealthChatbot", errorMsg)
                    }
                }
                success
            }
        } catch (t: Throwable) {
            Log.e("HealthChatbot", "SDK initialization crashed", t)
            withContext(Dispatchers.Main) {
                val errorMsg = "SDK initialization crashed: ${t.localizedMessage}"
                binding.textViewStatus.text = "❌ $errorMsg"
                HealthChatbotApplication.initializationError = errorMsg
            }
            false
        } finally {
            HealthChatbotApplication.isInitializing = false
        }
    }

    private fun sendMessage(text: String) {
        try {
            Log.d("HealthChatbot", "Sending message to ViewModel: $text")
            lifecycleScope.launch {
                try {
                    viewModel.sendMessage(text)
                } catch (e: Exception) {
                    logError("Send Message Error", e)
                    showError("Message send failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logError("Send Message Setup Error", e)
            showError("Message setup failed: ${e.message}")
        }
    }

    private fun downloadModel() {
        try {
            // Prevent multiple simultaneous operations
            if (initializationJob?.isActive == true) {
                Log.d("HealthChatbot", "Download already in progress")
                return
            }

            binding.buttonDownloadModel.isEnabled = false
            updateStatus("Starting download process...")

            initializationJob = lifecycleScope.launch {
                try {
                    Log.d("HealthChatbot", "=== REAL SDK DOWNLOAD - USING ACTUAL RUNANYWHERE ===")

                    // Step 1: Check SDK availability
                    updateStatus("Checking SDK availability...")
                    Log.d("HealthChatbot", "Checking RunAnywhere SDK...")

                    delay(1000)

                    try {
                        Class.forName("com.runanywhere.sdk.public.RunAnywhere")
                        Log.d("HealthChatbot", "✅ RunAnywhere SDK classes found")
                    } catch (e: ClassNotFoundException) {
                        throw Exception("RunAnywhere SDK not found: ${e.message}")
                    }

                    updateStatus("Initializing SDK...")
                    Log.d("HealthChatbot", "Starting SDK initialization...")

                    // Step 2: Initialize SDK on background thread
                    withContext(Dispatchers.IO) {
                        try {
                            RunAnywhere.initialize(
                                context = this@MainActivity,
                                apiKey = "dev-api-key",
                                environment = SDKEnvironment.DEVELOPMENT
                            )
                            Log.d("HealthChatbot", "✅ SDK initialization successful")
                            isSDKInitialized = true
                        } catch (e: Exception) {
                            throw Exception("SDK initialization failed: ${e.message}", e)
                        }
                    }

                    updateStatus("Scanning for models...")
                    Log.d("HealthChatbot", "Scanning for available models...")

                    // Step 3: List and add models if needed
                    withContext(Dispatchers.IO) {
                        try {
                            val models = listAvailableModels()
                            Log.d("HealthChatbot", "Found ${models.size} models")

                            if (models.isEmpty()) {
                                // Add a model if none exist
                                Log.d("HealthChatbot", "No models found, adding model URL...")
                                addModelFromURL(
                                    url = "https://huggingface.co/microsoft/DialoGPT-medium/resolve/main/pytorch_model.bin",
                                    name = "DialoGPT-medium",
                                    type = "LLM"
                                )

                                // Re-scan
                                val newModels = listAvailableModels()
                                Log.d(
                                    "HealthChatbot",
                                    "After adding URL, found ${newModels.size} models"
                                )
                            }
                        } catch (e: Exception) {
                            Log.w("HealthChatbot", "Model listing failed: ${e.message}")
                        }
                    }

                    updateStatus("Starting real model download...")
                    Log.d("HealthChatbot", "=== USING REAL RUNANYWHERE DOWNLOAD SERVICE ===")

                    // Step 4: ACTUAL RunAnywhere SDK download (no more bypass)
                    withContext(Dispatchers.IO) {
                        try {
                            val models = listAvailableModels()
                            if (models.isNotEmpty()) {
                                val modelId = models[0].id
                                Log.d("HealthChatbot", "Starting REAL download for model: $modelId")

                                withContext(Dispatchers.Main) {
                                    updateStatus("Downloading model...")
                                }

                                // REAL RunAnywhere SDK download with progress tracking
                                RunAnywhere.downloadModel(modelId).collect { progress ->
                                    val percentage = (progress * 100).toInt()
                                    withContext(Dispatchers.Main) {
                                        updateStatus("Downloading model... $percentage%")
                                        Log.d(
                                            "HealthChatbot",
                                            "Real download progress: $percentage%"
                                        )
                                    }
                                }

                                isModelDownloaded = true
                                Log.d(
                                    "HealthChatbot",
                                    "✅ REAL model download completed successfully"
                                )
                            } else {
                                throw Exception("No models available for download")
                            }
                        } catch (e: Exception) {
                            throw Exception("Real model download failed: ${e.message}", e)
                        }
                    }

                    // Success
                    withContext(Dispatchers.Main) {
                        updateStatus("✅ Model downloaded successfully! (Real AI Mode)")
                        binding.buttonDownloadModel.text = "Downloaded ✅"
                        binding.buttonLoadModel.isEnabled = true
                        Log.d("HealthChatbot", "=== REAL DOWNLOAD COMPLETED SUCCESSFULLY ===")
                    }

                } catch (e: Exception) {
                    logError("Real Download Process Error", e)
                    withContext(Dispatchers.Main) {
                        updateStatus("❌ Download failed - Check logs for details")
                        showError("Real download failed: ${e.message}")

                        // Keep real mode, don't fall back to test mode
                        binding.buttonDownloadModel.text = "Download Failed"
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.buttonDownloadModel.isEnabled = true
                    }
                }
            }

        } catch (e: Exception) {
            logError("Download Model Setup Error", e)
            showError("Download setup failed: ${e.message}")
            binding.buttonDownloadModel.isEnabled = true
        }
    }

    private fun loadModel() {
        try {
            if (!isSDKInitialized || !isModelDownloaded) {
                showError("Please download model first")
                return
            }

            binding.buttonLoadModel.isEnabled = false
            updateStatus("Loading model...")

            lifecycleScope.launch {
                try {
                    Log.d("HealthChatbot", "=== REAL SDK LOAD - USING ACTUAL RUNANYWHERE ===")

                    updateStatus("Preparing model for loading...")
                    Log.d("HealthChatbot", "Starting real model load...")

                    // Step 1: Check if models are available
                    withContext(Dispatchers.IO) {
                        try {
                            val models = listAvailableModels()
                            Log.d("HealthChatbot", "Found ${models.size} models for loading")

                            if (models.isEmpty()) {
                                throw Exception("No models available for loading")
                            }
                        } catch (e: Exception) {
                            throw Exception("Model listing failed: ${e.message}", e)
                        }
                    }

                    updateStatus("Loading AI model into memory...")
                    Log.d("HealthChatbot", "=== USING REAL RUNANYWHERE LOAD SERVICE ===")

                    // Step 2: ACTUAL RunAnywhere SDK load (no more bypass)
                    withContext(Dispatchers.IO) {
                        try {
                            val models = listAvailableModels()
                            if (models.isNotEmpty()) {
                                val modelId = models[0].id
                                Log.d("HealthChatbot", "Starting REAL load for model: $modelId")

                                withContext(Dispatchers.Main) {
                                    updateStatus("Loading neural network...")
                                }

                                // REAL RunAnywhere SDK load
                                val success = RunAnywhere.loadModel(modelId)

                                if (success) {
                                    isModelLoaded = true
                                    Log.d(
                                        "HealthChatbot",
                                        "✅ REAL model load completed successfully"
                                    )
                                } else {
                                    throw Exception("Model load returned false")
                                }
                            } else {
                                throw Exception("No models available for loading")
                            }
                        } catch (e: Exception) {
                            throw Exception("Real model load failed: ${e.message}", e)
                        }
                    }

                    // Success
                    withContext(Dispatchers.Main) {
                        updateStatus("✅ AI Model loaded and ready! (Real AI Mode)")
                        binding.buttonLoadModel.text = "Loaded ✅"
                        binding.editTextMessage.hint = "Ask me about your health..."
                        Log.d("HealthChatbot", "=== REAL LOAD COMPLETED SUCCESSFULLY ===")
                    }

                } catch (e: Exception) {
                    logError("Real Load Process Error", e)
                    withContext(Dispatchers.Main) {
                        updateStatus("❌ Load failed - Check logs for details")
                        showError("Real load failed: ${e.message}")

                        // Keep real mode, don't fall back to test mode
                        binding.buttonLoadModel.text = "Load Failed"
                    }
                } finally {
                    withContext(Dispatchers.Main) {
                        binding.buttonLoadModel.isEnabled = true
                    }
                }
            }

        } catch (e: Exception) {
            logError("Load Model Setup Error", e)
            showError("Load setup failed: ${e.message}")
            binding.buttonLoadModel.isEnabled = true
        }
    }

    private fun checkStoragePermissionAndUpload() {
        try {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    documentPicker.launch("application/pdf")
                }

                else -> {
                    storagePermissionRequest.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        } catch (e: Exception) {
            logError("Storage Permission Error", e)
            showError("Permission check failed: ${e.message}")
        }
    }

    private fun processPdf(uri: Uri) {
        try {
            Log.d("HealthChatbot", "Processing PDF: $uri")
            lifecycleScope.launch {
                try {
                    viewModel.processPdf(this@MainActivity, uri)
                    showError("PDF processed successfully!")
                } catch (e: Exception) {
                    logError("PDF Processing Error", e)
                    showError("PDF processing failed: ${e.message}")
                }
            }
        } catch (e: Exception) {
            logError("PDF Processing Setup Error", e)
            showError("PDF processing setup failed: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pdfPickerLauncher.launch("application/pdf")
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val connectivityManager =
                getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
            networkCapabilities != null && networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (e: Exception) {
            Log.w("HealthChatbot", "Failed to check network", e)
            false
        }
    }

    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}