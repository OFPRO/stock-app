package com.app2.feature.products

import android.Manifest
import android.content.pm.PackageManager
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app2.core.ui.components.StockCard
import com.app2.core.ui.components.StockTextField
import com.app2.core.ui.components.TextFieldVariant
import com.app2.core.ui.theme.Accent
import com.app2.core.ui.theme.Error
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors

val PRODUCT_CATEGORIES = listOf(
    "Papeterie" to "Papeterie",
    "Fournitures" to "Fournitures de bureau",
    "Informatique" to "Informatique",
    "Mobilier" to "Mobilier",
    "Impression" to "Impression",
    "Librairie" to "Librairie",
    "Hygiène" to "Hygiène & Nettoyage",
    "Emballage" to "Emballage",
    "Autre" to "Autre"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductFormScreen(
    editData: ProductDetailDisplayData? = null,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    viewModel: ProductFormViewModel = androidx.hilt.navigation.compose.hiltViewModel()
) {
    val name by viewModel.name.collectAsStateWithLifecycle()
    val sku by viewModel.sku.collectAsStateWithLifecycle()
    val barcode by viewModel.barcode.collectAsStateWithLifecycle()
    val price by viewModel.price.collectAsStateWithLifecycle()
    val purchasePrice by viewModel.purchasePrice.collectAsStateWithLifecycle()
    val category by viewModel.category.collectAsStateWithLifecycle()
    val description by viewModel.description.collectAsStateWithLifecycle()
    val minQuantity by viewModel.minQuantity.collectAsStateWithLifecycle()
    val maxQuantity by viewModel.maxQuantity.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    var showScanner by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    viewModel.initForm(editData)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (editData != null) "Modifier le produit" else "Nouveau produit")
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Fermer")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(12.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            SectionHeader(Icons.Default.Inventory2, "Identité du produit")
            StockCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { viewModel.onNameChanged(it) },
                        label = { Text("Nom du produit *") },
                        isError = name.isBlank(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sku,
                            onValueChange = { viewModel.onSkuChanged(it) },
                            label = { Text("SKU *") },
                            isError = sku.isBlank(),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = barcode,
                            onValueChange = { viewModel.onBarcodeChanged(it) },
                            label = { Text("Code-barres") },
                            singleLine = true,
                            trailingIcon = {
                                IconButton(onClick = { showScanner = true }) {
                                    Icon(
                                        Icons.Default.QrCodeScanner,
                                        contentDescription = "Scanner",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = PRODUCT_CATEGORIES.firstOrNull { it.first == category }?.second ?: category,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Catégorie") },
                            leadingIcon = {
                                Icon(Icons.Default.Category, contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary)
                            },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable)
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            PRODUCT_CATEGORIES.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        viewModel.onCategoryChanged(value)
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            SectionHeader(Icons.Default.PriceCheck, "Prix")
            StockCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = price,
                        onValueChange = { viewModel.onPriceChanged(it) },
                        label = { Text("Prix de vente *") },
                        isError = price.isBlank() || (price.isNotBlank() && price.toDoubleOrNull() == null),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = purchasePrice,
                        onValueChange = { viewModel.onPurchasePriceChanged(it) },
                        label = { Text("Prix d'achat") },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            SectionHeader(Icons.Default.Description, "Description")
            StockCard(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = description,
                    onValueChange = { viewModel.onDescriptionChanged(it) },
                    label = { Text("Description du produit") },
                    placeholder = { Text("Marque, modèle, caractéristiques...") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SectionHeader(Icons.Default.Warehouse, "Gestion de stock")
            StockCard(
                modifier = Modifier.fillMaxWidth(),
                accentColor = Accent
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Seuils d'alerte",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = minQuantity,
                            onValueChange = { viewModel.onMinQuantityChanged(it) },
                            label = { Text("Stock minimum") },
                            singleLine = true,
                            supportingText = { Text("Alerte si stock ≤ ce seuil") },
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = maxQuantity,
                            onValueChange = { viewModel.onMaxQuantityChanged(it) },
                            label = { Text("Stock maximum") },
                            singleLine = true,
                            supportingText = { Text("Capacité maximale") },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            errorMessage?.let { msg ->
                Text(
                    text = msg,
                    color = Error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Button(
                onClick = {
                    viewModel.save(
                        isEdit = editData != null,
                        onSuccess = onSaved
                    )
                },
                enabled = !isLoading && name.isNotBlank() && sku.isNotBlank() && price.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isLoading) "Enregistrement..." else if (editData != null) "Enregistrer" else "Créer le produit",
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }

    if (showScanner) {
        ModalBottomSheet(
            onDismissRequest = { showScanner = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            BarcodeScannerSheet(
                onBarcodeDetected = { code ->
                    viewModel.onBarcodeChanged(code)
                    showScanner = false
                },
                onDismiss = { showScanner = false }
            )
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun BarcodeScannerSheet(
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var manualCode by remember { mutableStateOf("") }
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    Icons.Default.QrCodeScanner,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Scanner un code-barres",
                    style = MaterialTheme.typography.titleLarge
                )
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Fermer")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (hasCameraPermission) {
            val lifecycleOwner = LocalLifecycleOwner.current
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .size(280.dp)
            ) {
                AndroidView(
                    factory = { ctx ->
                        PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                ) { previewView ->
                    val cameraProviderFuture = ProcessCameraProvider.getInstance(previewView.context)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()
                        val preview = Preview.Builder().build().also {
                            it.setSurfaceProvider(previewView.surfaceProvider)
                        }
                        @Suppress("DEPRECATION")
                        val imageAnalysis = ImageAnalysis.Builder()
                            .setTargetResolution(Size(1280, 720))
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .build()
                        val scanner = BarcodeScanning.getClient()
                        val analyzer: (ImageProxy) -> Unit = { imageProxy ->
                            val mediaImage = imageProxy.image
                            if (mediaImage != null) {
                                val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                scanner.process(inputImage)
                                    .addOnSuccessListener { barcodes ->
                                        for (barcode in barcodes) {
                                            barcode.rawValue?.let { value ->
                                                onBarcodeDetected(value)
                                            }
                                        }
                                    }
                                    .addOnCompleteListener { imageProxy.close() }
                            } else {
                                imageProxy.close()
                            }
                        }
                        imageAnalysis.setAnalyzer(Executors.newSingleThreadExecutor(), analyzer)
                        val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, preview, imageAnalysis)
                        } catch (_: Exception) {}
                    }, ContextCompat.getMainExecutor(previewView.context))
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
        }

        Text(
            text = "Ou saisir le code manuellement",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(Modifier.height(8.dp))

        StockTextField(
            value = manualCode,
            onValueChange = { manualCode = it },
            placeholder = "Code-barres",
            variant = TextFieldVariant.Barcode
        )

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.weight(1f)
            ) { Text("Annuler") }
            Button(
                onClick = {
                    if (manualCode.isNotBlank()) {
                        onBarcodeDetected(manualCode)
                        onDismiss()
                    }
                },
                modifier = Modifier.weight(1f)
            ) { Text("Valider") }
        }
    }
}
