package com.example.events.ui.screens

import android.Manifest
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.example.events.data.api.EventsService
import com.example.events.data.api.UserService
import com.example.events.data.model.Event
import kotlinx.coroutines.launch
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Warning
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.Dialog
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import java.io.File
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Environment
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.core.content.FileProvider
import coil.compose.rememberImagePainter
import com.example.events.data.api.AuthService
import com.example.events.data.model.EventItem
import com.example.events.data.model.User
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import okhttp3.*

import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import kotlinx.coroutines.delay


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    eventsService: EventsService,
    userService: UserService
) {
    val coroutineScope = rememberCoroutineScope()
    var events by remember { mutableStateOf<List<Event>>(emptyList()) }

    var showCreateEventDialog by remember {
        mutableStateOf(false)
    }

    var selectedEvent by remember { mutableStateOf<Event?>(null) }
    // Nuevo estado para controlar la visibilidad del diálogo de edición de ítems
    var showEditItemDialog =  remember { mutableStateOf(false) }


    LaunchedEffect(Unit) {
        coroutineScope.launch {
            events = eventsService.fetchEvents()
        }
    }

    fun updateEvent(updatedEvent: Event) {
        events = events.map { event ->
            if (event.id == updatedEvent.id) {
                updatedEvent
            } else {
                event
            }
        }
    }
    //Nueva Función añadida
    fun deleteEvent(eventId: Int) {
        events = events.filter { it.id != eventId }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Eventos") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateEventDialog = true }) {
                Icon(Icons.Filled.Add, "Crear Evento")
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            items(events) { event ->
                EventCard(
                    event = event,
                    onEventModified = { updatedEvent ->
                        updateEvent(updatedEvent)
                    },
                    onDeleteEvent = { eventToDelete ->
                        deleteEvent(eventToDelete.id)
                    },
                    eventsService = eventsService,
                    userService = userService,
                    authService = AuthService(),
                    onShowEditItemDialog = {
                        showEditItemDialog.value = true
                        selectedEvent = event
                    },

                )
            }
        }
        }

        // Mostrar el diálogo de creación de eventos si `showCreateEventDialog` es true
        if (showCreateEventDialog) {
            CreateEventDialog(
                onDismissRequest = { showCreateEventDialog = false },
                onEventCreated = { newEvent ->
                    // Aquí manejas el nuevo evento (p.ej., agregarlo a la lista de eventos)
                    events = events + newEvent
                    showCreateEventDialog = false
                },
                eventsService = eventsService,
                userService = userService,
                authService = AuthService()
            )
        }

        // Mostrar el diálogo de edición de ítems
        if (showEditItemDialog.value && selectedEvent != null) {
            AddItemDialog(
                eventId = selectedEvent!!.id,
                onDismissRequest = {
                    showEditItemDialog.value = false
                    selectedEvent = null
                },
                onItemAdded = { newItem ->
                    val updatedEvent = selectedEvent!!.copy(
                        itemList = selectedEvent!!.itemList.copy(
                            items = selectedEvent!!.itemList.items + newItem
                        )
                    )
                    updateEvent(updatedEvent)
                    showEditItemDialog.value = false
                    selectedEvent = null
                },
                eventsService = eventsService,
                userService = userService
            )
        }
    }


@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun CreateEventDialog(
    onDismissRequest: () -> Unit,
    onEventCreated: (Event) -> Unit,
    eventsService: EventsService,
    userService: UserService,
    authService: AuthService
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var organizerId by remember { mutableStateOf("1") } // Default organizer ID
    var participantIds by remember { mutableStateOf<List<Int>>(emptyList()) } // Lista de IDs de participantes seleccionados
    var allUsers by remember { mutableStateOf<List<com.example.events.data.model.User>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    var selectedImageUris by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var recordedAudioUri by remember { mutableStateOf<Uri?>(null) }
    val context = LocalContext.current
    var showParticipantDialog by remember { mutableStateOf(false) } // Controla la visibilidad del diálogo de participantes

    val cameraPermissionState = rememberPermissionState(android.Manifest.permission.CAMERA)
    var showCameraPermissionRationale by remember { mutableStateOf(false) } // Controla la visibilidad del rationale
    val audioPermissionState = rememberPermissionState(Manifest.permission.RECORD_AUDIO)
    var showAudioPermissionRationale by remember { mutableStateOf(false) }


    // Cargar la lista de usuarios al iniciar el diálogo
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            //Asumiendo que tienes un endpoint para traer todos los usuarios
            allUsers = userService.fetchAllUsers() // Implementa esta función en tu EventsService
        }
    }

    // Lanzador para seleccionar imágenes de la galería
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments(),
        onResult = { uris ->
            selectedImageUris = uris
        }
    )

    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // Lanzador para tomar fotos con la cámara
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture(),
        onResult = { success ->
            if (success && tempImageUri != null) {
                selectedImageUris = selectedImageUris + tempImageUri!!
            }
        }
    )

    fun createImageUri(context: Context): Uri {
        val imagesDir = File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "EventsImages")
        if (!imagesDir.exists()) {
            imagesDir.mkdirs()
        }
        val image = File(imagesDir, "event_image_${System.currentTimeMillis()}.jpg")
        return FileProvider.getUriForFile(
            context,
            "${context.packageName}.provider",
            image
        )
    }

    fun launchCamera() {
        if (cameraPermissionState.status.isGranted) {
            // Permiso concedido, lanzar la cámara
            tempImageUri = createImageUri(context)
            cameraLauncher.launch(tempImageUri!!)
        } else {
            if (cameraPermissionState.status.shouldShowRationale) {
                // Mostrar el rationale
                showCameraPermissionRationale = true
            } else {
                // Solicitar el permiso directamente
                cameraPermissionState.launchPermissionRequest()
            }

        }
    }

    // Estado y funciones para grabar audio
    var isRecording by remember { mutableStateOf(false) }
    val recorder = remember { MediaRecorder() }
    var audioFilePath by remember { mutableStateOf("") } // Almacena la ruta del archivo de audio
    // Función para iniciar la grabación de audio

    fun startRecording() {
        if (audioPermissionState.status.isGranted) {
            val audioDir = File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC), "EventsAudio")
            if (!audioDir.exists()) {
                audioDir.mkdirs()
            }
            val audioFile = File(audioDir, "event_audio_${System.currentTimeMillis()}.mp3")
            audioFilePath = audioFile.absolutePath

            try {
                recorder.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                    setOutputFile(audioFilePath)

                    prepare()
                    start()
                    isRecording = true
                }
            } catch (e: Exception) {
                Log.e("AudioRecord", "prepare() failed: ${e.message}")
                recorder.reset()
                recorder.release()
                // Manejar el error aquí (por ejemplo, mostrar un mensaje al usuario)
                isRecording = false
            }
        } else {
            if (audioPermissionState.status.shouldShowRationale) {
                showAudioPermissionRationale = true
            } else {
                audioPermissionState.launchPermissionRequest()
            }
        }
    }

    // Función para detener la grabación de audio
    fun stopRecording() {
        recorder.apply {
            if (isRecording) {
                try {
                    stop()
                    release()
                } catch (e: Exception) {
                    Log.e("AudioRecord", "stop() failed: ${e.message}")
                } finally {
                    isRecording = false
                    recordedAudioUri = Uri.fromFile(File(audioFilePath))
                }
            }
        }
    }

    if (showAudioPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showAudioPermissionRationale = false },
            title = { Text("Permiso de micrófono necesario") },
            text = { Text("Necesitamos permiso para acceder al micrófono para que puedas grabar notas de audio para el evento.") },
            confirmButton = {
                Button(onClick = {
                    showAudioPermissionRationale = false
                    audioPermissionState.launchPermissionRequest()
                }) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAudioPermissionRationale = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Crear Nuevo Evento",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Sección de información básica
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del Evento") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descripción") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        maxLines = 3
                    )

                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Ubicación") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = date,
                            onValueChange = { date = it },
                            label = { Text("Fecha (YYYY-MM-DD)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = time,
                            onValueChange = { time = it },
                            label = { Text("Hora (HH:MM)") },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                    }

                    OutlinedTextField(
                        value = organizerId,
                        onValueChange = { organizerId = it },
                        label = { Text("ID del Organizador") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sección de participantes
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { showParticipantDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Seleccionar Participantes")
                    }

                    if (participantIds.isNotEmpty()) {
                        Text(
                            text = "Participantes seleccionados: ${participantIds.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sección multimedia
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Multimedia",
                        style = MaterialTheme.typography.titleMedium
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch(arrayOf("image/*")) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Galería",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Galería")
                        }

                        OutlinedButton(
                            onClick = { launchCamera() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Cámara",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Cámara")
                        }
                    }

                    // Vista previa de imágenes
                    if (selectedImageUris.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.height(100.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(selectedImageUris) { uri ->
                                AsyncImage(
                                    model = uri,
                                    contentDescription = "Imagen seleccionada",
                                    modifier = Modifier
                                        .size(100.dp)
                                        .clip(RoundedCornerShape(8.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Grabación de audio
                    OutlinedButton(
                        onClick = {
                            if (isRecording) {
                                stopRecording()
                            } else {
                                startRecording()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isRecording) Icons.Default.Close else Icons.Default.PlayArrow,
                            contentDescription = if (isRecording) "Detener" else "Grabar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isRecording) "Detener grabación" else "Grabar nota de audio")
                    }

                    if (recordedAudioUri != null) {
                        Text(
                            text = "Audio grabado",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Botones de acción
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Cancelar")
                    }

                    Button(
                        onClick = {
                            // Validación básica
                            if (name.isBlank() || date.isBlank() || time.isBlank()) {
                                // Mostrar error
                                return@Button
                            }

                            val newEvent = Event(
                                id = 0,
                                name = name,
                                description = description,
                                location = location,
                                date = date,
                                time = time,
                                createdAt = "",
                                updatedAt = "",
                                organizer = com.example.events.data.model.User(
                                    id = organizerId.toInt(),
                                    username = "",
                                    email = "",
                                    firstName = "",
                                    lastName = ""
                                ),
                                participants = emptyList(),
                                images = emptyList(),
                                audioNotes = emptyList(),
                                itemList = com.example.events.data.model.ItemList(0, "", "", emptyList())
                            )

                            coroutineScope.launch {
                                val eventId = eventsService.createEvent(
                                    newEvent,
                                    organizerId.toInt(),
                                    participantIds
                                )

                                if (eventId != null) {
                                    // Subir imágenes y audio
                                    selectedImageUris.forEach { uri ->
                                        eventsService.uploadImage(eventId, uri, context, eventsService.token)
                                    }
                                    if (recordedAudioUri != null) {
                                        eventsService.uploadAudio(eventId, recordedAudioUri!!)
                                    }
                                    onEventCreated(newEvent.copy(id = eventId))
                                    onDismissRequest()
                                }
                            }
                        },
                        modifier = Modifier.height(48.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Crear Evento")
                    }
                }
            }
        }
    }


    // Diálogo para seleccionar participantes
    if (showParticipantDialog) {
        AlertDialog(
            onDismissRequest = { showParticipantDialog = false },
            title = { Text("Seleccionar Participantes") },
            text = {
                // Lista de checkboxes para cada usuario
                Column {
                    allUsers.forEach { user ->
                        var isChecked by remember { mutableStateOf(participantIds.contains(user.id)) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable {
                                    isChecked = !isChecked
                                    if (isChecked) {
                                        participantIds = participantIds + user.id
                                    } else {
                                        participantIds = participantIds - user.id
                                    }
                                }
                                .padding(4.dp)
                        ) {
                            Checkbox(
                                checked = isChecked,
                                onCheckedChange = null // Deshabilitar el checkbox directamente
                            )
                            Text("${user.firstName} ${user.lastName} (ID: ${user.id})")
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showParticipantDialog = false }) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showParticipantDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Rationale Dialog
    if (showCameraPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showCameraPermissionRationale = false },
            title = { Text("Permiso de cámara necesario") },
            text = { Text("Necesitamos permiso para acceder a la cámara para que puedas tomar fotos para el evento.") },
            confirmButton = {
                Button(onClick = {
                    showCameraPermissionRationale = false
                    cameraPermissionState.launchPermissionRequest()
                }) {
                    Text("Entendido")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCameraPermissionRationale = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun AudioPlayerComponent(audioUrl: String) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }

    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(audioUrl)))
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                        progress = 0f
                    }
                }

                override fun onIsPlayingChanged(isPlayingNow: Boolean) {
                    isPlaying = isPlayingNow
                }
            })
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            delay(200)
            progress = exoPlayer.currentPosition.toFloat() / exoPlayer.duration.toFloat()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        if (isPlaying) {
                            exoPlayer.pause()
                        } else {
                            exoPlayer.play()
                        }
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Close else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Nota de audio",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    )
                }
            }
        }
    }
}

@Composable
fun ItemListComponent(itemList: com.example.events.data.model.ItemList) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(1.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = itemList.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            itemList.description?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 12.dp)
                )
            }

            Divider(
                modifier = Modifier.padding(bottom = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemList.items.forEach { item ->
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small),
                        color = when (item.status) {
                            "completed" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            else -> MaterialTheme.colorScheme.surface
                        },
                        border = BorderStroke(
                            width = 1.dp,
                            color = when (item.status) {
                                "completed" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when (item.status) {
                                            "completed" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.surface
                                        }
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = when (item.status) {
                                            "completed" -> MaterialTheme.colorScheme.primary
                                            else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
                                        },
                                        shape = CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (item.status == "completed") {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Completado",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Responsable: ${item.responsible.firstName}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddItemDialog(
    eventId: Int,
    onDismissRequest: () -> Unit,
    onItemAdded: (EventItem) -> Unit,
    eventsService: EventsService,
    userService: UserService
) {
    var itemName by remember { mutableStateOf("") }
    var responsibleIdText by remember { mutableStateOf("") } // Campo para que el usuario ingrese su ID
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    // Obtener el token del EventsService
    val token = eventsService.token


    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Añadir Nuevo Ítem",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                TextField(
                    value = itemName,
                    onValueChange = { itemName = it },
                    label = { Text("Nombre del Ítem") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = responsibleIdText,
                    onValueChange = { responsibleIdText = it },
                    label = { Text("ID del Responsable") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    TextButton(onClick = onDismissRequest) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        coroutineScope.launch {
                            // Validar que responsibleIdText sea un número válido
                            val responsibleId = responsibleIdText.toIntOrNull()
                            if (responsibleId != null) {
                                val newItem = eventsService.createItem(
                                    eventId = eventId,
                                    itemName = itemName,
                                    responsibleId = responsibleId
                                )

                                if (newItem != null) {
                                    onItemAdded(newItem)
                                    onDismissRequest()
                                } else {
                                    // Manejar el error si la creación del ítem falla
                                    println("Error al crear el ítem")
                                }
                            } else {
                                // Manejar el caso en que responsibleIdText no es un número válido
                                println("ID del responsable inválido")
                            }
                        }
                    }) {
                        Text("Añadir Ítem")
                    }
                }
            }
        }
    }
}


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun EventCard(
    event: Event,
    onEventModified: (Event) -> Unit,
    eventsService: EventsService,
    userService: UserService,
    authService: AuthService,
    onShowEditItemDialog: () -> Unit,
    onDeleteEvent: (Event) -> Unit,
) {
    var expandedState by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current
    val notificationPermissionState = rememberPermissionState(Manifest.permission.POST_NOTIFICATIONS)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { expandedState = !expandedState },
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header con icono de evento
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Evento",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp))

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.name,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Text(
                            text = "${event.date} • ${event.time}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }

                            Icon(
                            imageVector = if (expandedState) Icons.Default.AddCircle else Icons.Default.ArrowDropDown,
                    contentDescription = if (expandedState) "Mostrar menos" else "Mostrar más",
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Descripción
            Text(
                text = event.description,
                modifier = Modifier.padding(vertical = 4.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )

            // Ubicación con icono
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Ubicación",
                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }

            // Imágenes (si existen)
            if (event.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.height(120.dp)
                ) {
                    items(event.images) { image ->
                        AsyncImage(
                            model = image.image,
                            contentDescription = image.caption ?: "Imagen del evento",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .width(160.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                    }
                }
            }

            // Contenido expandible
            if (expandedState) {
                // Notas de audio
                if (event.audioNotes.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Notas de Audio",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    event.audioNotes.forEach { audioNote ->
                        AudioPlayerComponent(audioUrl = audioNote.audioFile)
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }

                // Lista de elementos
                if (event.itemList.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ItemListComponent(itemList = event.itemList)
                }

                // Botones de acción
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    // Botón para añadir ítem
                    OutlinedButton(
                        onClick = { onShowEditItemDialog() },
                        modifier = Modifier.padding(end = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Añadir",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Añadir ítem")
                    }

                    // Botón de notificación
                    OutlinedButton(
                        onClick = {
                            if (notificationPermissionState.status.isGranted) {
                                eventsService.sendNotificationToParticipants(context, event)
                            } else {
                                notificationPermissionState.launchPermissionRequest()
                            }
                        },
                        modifier = Modifier.padding(end = 8.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notificar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Notificar")
                    }

                    // Botón para eliminar
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (eventsService.deleteEvent(event.id)) {
                                    onDeleteEvent(event)
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Eliminar",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Eliminar")
                    }
                }
            }
        }
    }
}