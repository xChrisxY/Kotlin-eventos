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
import androidx.compose.foundation.clickable
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



    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {

            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Crear Nuevo Evento",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Campos de texto para la información del evento
                TextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del Evento") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = location,
                    onValueChange = { location = it },
                    label = { Text("Ubicación") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Fecha (YYYY-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = time,
                    onValueChange = { time = it },
                    label = { Text("Hora (HH:MM)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = organizerId,
                    onValueChange = { organizerId = it },
                    label = { Text("ID del Organizador") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))


                // Botón para abrir el diálogo de selección de participantes
                Button(onClick = { showParticipantDialog = true }) {
                    Text("Seleccionar Participantes")
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Muestra los participantes seleccionados (opcional)
                if (participantIds.isNotEmpty()) {
                    Text("Participantes seleccionados: ${participantIds.joinToString(", ")}")
                }

                // Sección para adjuntar imágenes
                Text(text = "Imágenes Adjuntas")
                Row {
                    Button(onClick = {
                        imagePickerLauncher.launch(arrayOf("image/*"))
                    }) {
                        Text("Seleccionar Imágenes")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        launchCamera() // Usar la función launchCamera para gestionar el permiso
                    }) {
                        Text("Tomar Foto")
                    }
                }
                LazyRow {
                    items(selectedImageUris) { uri ->
                        Image(
                            painter = rememberImagePainter(data = uri),
                            contentDescription = "Imagen seleccionada",
                            modifier = Modifier
                                .size(80.dp)
                                .padding(4.dp)
                        )
                    }
                }

                // Sección para grabar audio
                Text(text = "Nota de Audio")
                Row {
                    Button(onClick = {
                        if (isRecording) {
                            stopRecording()
                        } else {
                            startRecording()
                        }
                    }) {
                        Text(if (isRecording) "Detener Grabación" else "Grabar Audio")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    if (recordedAudioUri != null) {
                        Text("Audio grabado: ${recordedAudioUri?.lastPathSegment}")
                    }
                }

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


                        // Crear el objeto Event
                        val newEvent = Event(
                            id = 0, // o un valor temporal si tu API genera el ID
                            name = name,
                            description = description,
                            location = location,
                            date = date,
                            time = time,
                            createdAt = "",
                            updatedAt = "",
                            organizer = com.example.events.data.model.User(id = organizerId.toInt(), username = "", email = "", firstName = "", lastName = ""), // Asume que solo necesitas el ID
                            participants = emptyList(), // Los participantes se envían solo por ID
                            images = emptyList(),
                            audioNotes = emptyList(),
                            itemList = com.example.events.data.model.ItemList(0,"","", emptyList())
                        )

                        coroutineScope.launch {
                            // Llamar a la función para crear el evento en el servidor
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
                                onEventCreated(newEvent.copy(id = eventId)) // Notificar que el evento se creó correctamente
                                onDismissRequest() // Cerrar el diálogo
                            } else {
                                // Manejar el error si la creación del evento falla
                                println("Error al crear el evento")
                            }
                        }
                    }) {
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
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(audioUrl)))
            prepare()
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == Player.STATE_ENDED) {
                        isPlaying = false
                    }
                }
            })
        }
    }

    DisposableEffect(key1 = exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.1f))
            .padding(8.dp)
    ) {
        IconButton(
            onClick = {
                if (isPlaying) {
                    exoPlayer.pause()
                    isPlaying = false
                } else {
                    exoPlayer.play()
                    isPlaying = true
                }
            },
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Icon(
                imageVector = if (isPlaying) Icons.Default.CheckCircle else Icons.Default.PlayArrow,
                contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Text(
            text = "Nota de audio",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun ItemListComponent(itemList: com.example.events.data.model.ItemList) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color.Gray.copy(alpha = 0.1f))
            .padding(16.dp)
    ) {
        Text(
            text = itemList.title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        itemList.description?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        itemList.items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Ícono de estado
                Icon(
                    imageVector = when (item.status) {
                        "completed" -> Icons.Default.CheckCircle
                        "pending" -> Icons.Default.CheckCircle
                        else -> Icons.Default.Warning // Para manejar estados no reconocidos
                    },

                    contentDescription = "Estado del ítem",

                    tint = when (item.status) {
                        "completed" -> Color.Green
                        "pending" -> Color.Green
                        else -> Color.Gray
                    },
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Nombre del ítem
                Text(
                    text = item.name,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.weight(1f))

                // Responsable
                Text(
                    text = item.responsible.firstName,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
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

    fun launchNotification(){
        if (notificationPermissionState.status.isGranted) {
            // Permiso concedido, lanzar la cámara
            eventsService.sendNotificationToParticipants(context, event)

        } else {
            // Permiso no concedido, solicitarlo
            notificationPermissionState.launchPermissionRequest()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clickable { expandedState = !expandedState },
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = event.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${event.date} - ${event.time}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }

            Text(
                text = event.description,
                modifier = Modifier.padding(vertical = 8.dp),
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Ubicación: ${event.location}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )

            if (event.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(event.images) { image ->
                        Image(
                            painter = rememberAsyncImagePainter(model = image.image),
                            contentDescription = image.caption ?: "Imagen del evento",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            if (event.audioNotes.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Notas de Audio",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                event.audioNotes.forEach { audioNote ->
                    AudioPlayerComponent(audioUrl = audioNote.audioFile)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Sección de Lista de Elementos
            if (expandedState) {
                if (event.itemList.items.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Lista de Elementos",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    ItemListComponent(itemList = event.itemList)
                }
                Row {
                    Button(onClick = { onShowEditItemDialog() }) {
                        Text("Añadir Ítem")
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Botón para Eliminar Evento
                    Button(onClick = {
                        coroutineScope.launch {
                            val isDeleted = eventsService.deleteEvent(event.id)
                            if (isDeleted) {
                                // El evento se eliminó con éxito
                                // Notificar a la HomeScreen para que actualice la lista
                                // Aquí no necesitas enviar un evento modificado porque lo estás eliminando
                                onDeleteEvent(event)
                            } else {
                                // Manejar el error si la eliminación falla
                                Log.e("EventCard", "Error al eliminar el evento")
                                // Mostrar un mensaje al usuario
                            }
                        }
                    }) {
                        Text("Eliminar Evento")
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        launchNotification()
                    }) {
                        Text("Mandar Notificación")
                    }

                }
            }

        }
    }

}