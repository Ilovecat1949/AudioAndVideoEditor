package com.example.audioandvideoeditor.viewmodel
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
private val TAG="FileReadingViewModel"

data class ContentItem(val id: Long, val text: String)
class FileReadingViewModel: ViewModel(){

    private var randomAccessFile: RandomAccessFile? = null
    private var file: File? = null
    private var fileSize: Long = 0
    private var currentPosition: Long = 0
    private val visibleBufferSize = 1024L // Adjust to visible content size
    private val bufferSize = visibleBufferSize * 3 // Adjust as needed, more buffer for smoother scrolling
    private var isAtEnd = false
    private var lastLoadedFileSize: Long = 0

    val visibleContent = mutableStateListOf<ContentItem>()
    var itemIdCounter by mutableStateOf(0L)
    var itemIdFlag by mutableStateOf(0L)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val readingProgress = mutableStateOf(0f)
    private val contentMutex = Mutex()
    fun openFile(file: File, readFromEnd: Boolean) {
        this.file = file
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    randomAccessFile = RandomAccessFile(file, "r")
                    fileSize = randomAccessFile?.length() ?: 0
                    currentPosition = if (readFromEnd) fileSize - visibleBufferSize.coerceAtMost(fileSize) else 0
                    if (currentPosition < 0) currentPosition = 0
                    isAtEnd = readFromEnd
                    lastLoadedFileSize = fileSize
                    loadVisibleContent()
                    isLoading.value = true
                }
                startFileMonitor()
            } catch (e: Exception) {
                errorMessage.value = "Error opening file: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun startFileMonitor() {
        viewModelScope.launch(Main) {
            while (true) {
                delay(1000)
                file?.let {
                    val currentFileSize = it.length()
                    if (currentFileSize > lastLoadedFileSize ) {
                        fileSize = currentFileSize
                        lastLoadedFileSize = currentFileSize
                        readingProgress.value = currentPosition.toFloat() / fileSize.toFloat()
                        if(isAtEnd){
                            loadNewContent()
                        }
                    }
                }
            }
        }
    }

    private fun loadNewContent() {
        try {
//                    val tempCurrentPosition = currentPosition
            currentPosition = fileSize - visibleBufferSize.coerceAtMost(fileSize)
            if (currentPosition < 0) currentPosition = 0
            loadVisibleContent()
//                    currentPosition = tempCurrentPosition
        } catch (e: Exception) {
            errorMessage.value = "Error loading new content: ${e.message}"
        }
//        viewModelScope.launch {
//            try {
//                withContext(Dispatchers.IO) {
////                    val tempCurrentPosition = currentPosition
//                    currentPosition = fileSize - visibleBufferSize.coerceAtMost(fileSize)
//                    if (currentPosition < 0) currentPosition = 0
//                    loadVisibleContent()
////                    currentPosition = tempCurrentPosition
//                }
//            } catch (e: Exception) {
//                errorMessage.value = "Error loading new content: ${e.message}"
//            }
//        }
    }

    fun loadMoreContent(isScrollingDown: Boolean) {
        try {
            if (isScrollingDown) {
                if (currentPosition + visibleBufferSize < fileSize) {
                    currentPosition += visibleBufferSize
                    loadVisibleContent()
                }
                else if(currentPosition!=fileSize-visibleBufferSize){
                    currentPosition=fileSize-visibleBufferSize
                    loadVisibleContent()
                }
            } else {
                if (currentPosition - visibleBufferSize >= 0) {
                    currentPosition -= visibleBufferSize
                    loadVisibleContent()
                }
                else if(currentPosition!=0L){
                    currentPosition=0
                    loadVisibleContent()
                }
            }
            isAtEnd = currentPosition + visibleBufferSize >= fileSize
        } catch (e: Exception) {
            errorMessage.value = "Error reading file: ${e.message}"
        }
//        viewModelScope.launch {
//            try {
//                withContext(Dispatchers.IO) {
//                    if (isScrollingDown) {
//                        if (currentPosition + visibleBufferSize < fileSize) {
//                            currentPosition += visibleBufferSize
//                            loadVisibleContent()
//                        }
//                    } else {
//                        if (currentPosition - visibleBufferSize >= 0) {
//                            currentPosition -= visibleBufferSize
//                            loadVisibleContent()
//                        }
//                    }
//                    isAtEnd = currentPosition + visibleBufferSize >= fileSize
//                }
//            } catch (e: Exception) {
//                errorMessage.value = "Error reading file: ${e.message}"
//            }
//        }
    }

    fun jumpToPosition(progress: Float) {
        try {
            val targetPosition = (fileSize * progress).toLong()
            currentPosition = targetPosition - visibleBufferSize.coerceAtMost(targetPosition)
            if (currentPosition < 0) currentPosition = 0
            loadVisibleContent()
            isAtEnd = currentPosition + visibleBufferSize >= fileSize
        } catch (e: Exception) {
            errorMessage.value = "Error jumping to position: ${e.message}"
        }
//        viewModelScope.launch {
//            try {
//                withContext(Dispatchers.IO) {
//                    val targetPosition = (fileSize * progress).toLong()
//                    currentPosition = targetPosition - visibleBufferSize.coerceAtMost(targetPosition)
//                    if (currentPosition < 0) currentPosition = 0
//                    loadVisibleContent()
//                    isAtEnd = currentPosition + visibleBufferSize >= fileSize
//                }
//            } catch (e: Exception) {
//                errorMessage.value = "Error jumping to position: ${e.message}"
//            }
//        }
    }

    private fun loadVisibleContent() {
        randomAccessFile?.let { raf ->
            try {
                raf.seek(currentPosition)
                val buffer = ByteArray(bufferSize.toInt())
                val bytesRead = raf.read(buffer)
                if (bytesRead > 0) {
                    val content = String(buffer, 0, bytesRead)
                    val lines = content.split("\n")
//                    contentMutex.withLock {
                    visibleContent.clear()
                    visibleContent.addAll(lines.map { ContentItem(itemIdCounter++, it) })
                    itemIdFlag= itemIdCounter
//                    }
                    Log.d(TAG,"visibleContent.size:${visibleContent.size}")
                    if(currentPosition==0L){
                        readingProgress.value=0f
                    }
                    else if(currentPosition+visibleBufferSize>=fileSize){
                        readingProgress.value=1f
                    }
                    else{
                        readingProgress.value = currentPosition.toFloat() / fileSize.toFloat()
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Error reading file: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        randomAccessFile?.close()
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class FileReadingViewModel_20250614: ViewModel(){

    private var randomAccessFile: RandomAccessFile? = null
    private var file: File? = null
    private var fileSize: Long = 0
    private var currentPosition: Long = 0
    var contextPosition= MutableStateFlow(0L)
    private var content:String?=null
    private val visibleBufferSize = 1024L // Adjust to visible content size
    private val bufferSize = visibleBufferSize * 3 // Adjust as needed, more buffer for smoother scrolling
    private var isAtEnd = false
    private var lastLoadedFileSize: Long = 0

    val visibleContent = mutableStateListOf<ContentItem>()
    var itemIdCounter by mutableStateOf(0L)
    var itemIdFlag by mutableStateOf(0L)
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val readingProgress = mutableStateOf(0f)
    private val contentMutex = Mutex()
    init {
        contextPosition
            .debounce(200L) // 防抖：在连续发出事件时，等待 200 毫秒没有新事件才处理
            .distinctUntilChanged() // 只有当页码真正改变时才向下传递
            .flatMapLatest { position ->
                flowOf(
                    if(randomAccessFile!=null){
                        withContext(Dispatchers.IO) {
                            val buffer = ByteArray(bufferSize.toInt())
                            val bytesRead = randomAccessFile!!.read(buffer)
                            if (bytesRead > 0) {
                                content = String(buffer, 0, bytesRead)
                                true
                            }
                            else{
                                false
                            }
                        }
                    }
                    else{
                        false
                    }
                )
            }
            .onEach {
                if(it){
                    val lines = content!!.split("\n")
                    visibleContent.clear()
                    visibleContent.addAll(lines.map { ContentItem(itemIdCounter++, it) })
                    itemIdFlag= itemIdCounter
                    Log.d(TAG,"visibleContent.size:${visibleContent.size}")
                    if(currentPosition==0L){
                        readingProgress.value=0f
                    }
                    else if(currentPosition+visibleBufferSize>=fileSize){
                        readingProgress.value=1f
                    }
                    else{
                        readingProgress.value = currentPosition.toFloat() / fileSize.toFloat()
                    }
                }
            }
            .launchIn(viewModelScope)
    }

    fun openFile(file: File, readFromEnd: Boolean) {
        this.file = file
        try {
                randomAccessFile = RandomAccessFile(file, "r")
                fileSize = randomAccessFile?.length() ?: 0
                currentPosition = if (readFromEnd) fileSize - visibleBufferSize.coerceAtMost(fileSize) else 0
                if (currentPosition < 0) currentPosition = 0
                isAtEnd = readFromEnd
                lastLoadedFileSize = fileSize
                loadVisibleContent()
                isLoading.value = true
                startFileMonitor()
            } catch (e: Exception) {
                errorMessage.value = "Error opening file: ${e.message}"
            } finally {
                isLoading.value = false
        }
    }

    private fun startFileMonitor() {
        viewModelScope.launch(Main) {
            while (true) {
                delay(1000)
                file?.let {
                    val currentFileSize = it.length()
                    if (currentFileSize > lastLoadedFileSize ) {
                        fileSize = currentFileSize
                        lastLoadedFileSize = currentFileSize
                        readingProgress.value = currentPosition.toFloat() / fileSize.toFloat()
                        if(isAtEnd){
                            loadNewContent()
                        }
                    }
                }
            }
        }
    }

    private fun loadNewContent() {
        try {
            currentPosition = fileSize - visibleBufferSize.coerceAtMost(fileSize)
            if (currentPosition < 0) currentPosition = 0
            loadVisibleContent()
        } catch (e: Exception) {
            errorMessage.value = "Error loading new content: ${e.message}"
        }
    }

    fun loadMoreContent(isScrollingDown: Boolean) {
        try {
            if (isScrollingDown) {
                if (currentPosition + visibleBufferSize < fileSize) {
                    currentPosition += visibleBufferSize
                    loadVisibleContent()
                }
                else if(currentPosition!=fileSize-visibleBufferSize){
                    currentPosition=fileSize-visibleBufferSize
                    loadVisibleContent()
                }
            } else {
                if (currentPosition - visibleBufferSize >= 0) {
                    currentPosition -= visibleBufferSize
                    loadVisibleContent()
                }
                else if(currentPosition!=0L){
                    currentPosition=0
                    loadVisibleContent()
                }
            }
            isAtEnd = currentPosition + visibleBufferSize >= fileSize
        } catch (e: Exception) {
            errorMessage.value = "Error reading file: ${e.message}"
        }
    }

    fun jumpToPosition(progress: Float) {
        try {
            val targetPosition = (fileSize * progress).toLong()
            currentPosition = targetPosition - visibleBufferSize.coerceAtMost(targetPosition)
            if (currentPosition < 0) currentPosition = 0
            loadVisibleContent()
            isAtEnd = currentPosition + visibleBufferSize >= fileSize
        } catch (e: Exception) {
            errorMessage.value = "Error jumping to position: ${e.message}"
        }
    }
    private fun loadVisibleContent(){
        contextPosition.value= currentPosition
    }
    private fun loadVisibleContent_bf() {
        randomAccessFile?.let { raf ->
            try {
                raf.seek(currentPosition)
                val buffer = ByteArray(bufferSize.toInt())
                val bytesRead = raf.read(buffer)
                if (bytesRead > 0) {
                    val content = String(buffer, 0, bytesRead)
                    val lines = content.split("\n")
                    visibleContent.clear()
                    visibleContent.addAll(lines.map { ContentItem(itemIdCounter++, it) })
                    itemIdFlag= itemIdCounter
                    Log.d(TAG,"visibleContent.size:${visibleContent.size}")
                    if(currentPosition==0L){
                        readingProgress.value=0f
                    }
                    else if(currentPosition+visibleBufferSize>=fileSize){
                        readingProgress.value=1f
                    }
                    else{
                        readingProgress.value = currentPosition.toFloat() / fileSize.toFloat()
                    }
                }
            } catch (e: Exception) {
                errorMessage.value = "Error reading file: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        randomAccessFile?.close()
    }
}

class FileReadingViewModel_2: ViewModel() {

    private var randomAccessFile: RandomAccessFile? = null
    private var file: File? = null
    private var fileSize: Long = 0
    private var currentPosition: Long = 0
    private val bufferSize = 100L // Adjust as needed
    private var isAtEnd = false // Track if we're at the end of the file

    val fileContent = mutableStateOf("")
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val readingProgress = mutableStateOf(0f)

    fun openFile(file: File, readFromEnd: Boolean) {
        this.file = file // Store the file
        viewModelScope.launch {
            try {
                isLoading.value = true
                withContext(Dispatchers.IO) {
                    randomAccessFile = RandomAccessFile(file, "r")
                    fileSize = randomAccessFile?.length() ?: 0
                    currentPosition = if (readFromEnd) fileSize - bufferSize.coerceAtMost(fileSize) else 0

                    if (currentPosition < 0) currentPosition = 0
                    isAtEnd = readFromEnd // set the initial state

                    readFileContent()
                }
                startFileMonitor() // Start monitoring for file changes
            } catch (e: Exception) {
                errorMessage.value = "Error opening file: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun startFileMonitor() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1000) // Check every 1 second (adjust as needed)
                file?.let {
                    val currentFileSize = it.length()
                    if (currentFileSize > fileSize) {
                        fileSize = currentFileSize
                        readingProgress.value = currentPosition.toFloat() / fileSize.toFloat()
                        if(isAtEnd) {
                            currentPosition = fileSize - bufferSize
                            loadNewContent()
                        }
                    }
                }
            }
        }
    }

    private fun loadNewContent() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    readFileContent()
                }
            } catch (e: Exception) {
                errorMessage.value = "Error loading new content: ${e.message}"
            }
        }
    }

    fun loadMoreContent(isScrollingDown: Boolean) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    if (isScrollingDown) {
                        if (currentPosition + bufferSize < fileSize) {
                            currentPosition += bufferSize
                            readFileContent()
                        }
                    } else {
                        if (currentPosition - bufferSize >= 0) {
                            currentPosition -= bufferSize
                            readFileContent()
                        }
                    }
                    isAtEnd = currentPosition + bufferSize >= fileSize
                }
            } catch (e: Exception) {
                errorMessage.value = "Error reading file: ${e.message}"
            }
        }
    }

    fun jumpToPosition(progress: Float) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val targetPosition = (fileSize * progress).toLong()
                    currentPosition = targetPosition - bufferSize.coerceAtMost(targetPosition)
                    if (currentPosition < 0) currentPosition = 0
                    readFileContent()
                    isAtEnd = currentPosition + bufferSize >= fileSize;
                }
            } catch (e: Exception) {
                errorMessage.value = "Error jumping to position: ${e.message}"
            }
        }
    }

    private fun readFileContent() {
        randomAccessFile?.let { raf ->
            try {
                raf.seek(currentPosition)
                val buffer = ByteArray(bufferSize.toInt())
                val bytesRead = raf.read(buffer)
                if (bytesRead > 0) {
                    val content = String(buffer, 0, bytesRead)
                    fileContent.value = content
                    readingProgress.value = currentPosition.toFloat() / fileSize.toFloat()
                }
            } catch (e: Exception) {
                errorMessage.value = "Error reading file: ${e.message}"
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        randomAccessFile?.close()
    }
}
class FileReadingViewModel_3 : ViewModel() {

    private var randomAccessFile: RandomAccessFile? = null
    private var file: File? = null
    private var fileSize: Long = 0
    private var currentPosition: Long = 0
    private val chunkSize = 4096L // Smaller chunk size for progressive loading
    private var isAtEnd = false
    private var lastLoadedFileSize: Long = 0

    val visibleContent = mutableStateListOf<ContentItem>()
    val isLoading = mutableStateOf(false)
    val errorMessage = mutableStateOf<String?>(null)
    val readingProgress = mutableStateOf(0f)

    private val contentMutex = Mutex()
    private var itemIdCounter: Long = 0
    private var loadJob: Job? = null
    private var scrollJob: Job? = null

    fun openFile(file: File, readFromEnd: Boolean) {
        this.file = file
        viewModelScope.launch {
            try {
                isLoading.value = true
                withContext(Dispatchers.IO) {
                    randomAccessFile = RandomAccessFile(file, "r")
                    fileSize = randomAccessFile?.length() ?: 0
                    currentPosition = if (readFromEnd) fileSize - chunkSize.coerceAtMost(fileSize) else 0

                    if (currentPosition < 0) currentPosition = 0
                    isAtEnd = readFromEnd
                    lastLoadedFileSize = fileSize
                    loadChunk()
                }
                startFileMonitor()
            } catch (e: Exception) {
                errorMessage.value = "Error opening file: ${e.message}"
            } finally {
                isLoading.value = false
            }
        }
    }

    private fun startFileMonitor() {
        viewModelScope.launch(Dispatchers.IO) {
            while (true) {
                delay(1000)
                file?.let {
                    val currentFileSize = it.length()
                    if (currentFileSize > lastLoadedFileSize && isAtEnd) {
                        fileSize = currentFileSize
                        lastLoadedFileSize = currentFileSize
                        loadNewContent()
                    }
                }
            }
        }
    }

    private fun loadNewContent() {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
//                    val tempCurrentPosition = currentPosition
                    currentPosition = fileSize - chunkSize.coerceAtMost(fileSize)
                    if (currentPosition < 0) currentPosition = 0
                    loadChunk()
//                    currentPosition = tempCurrentPosition
                }
            } catch (e: Exception) {
                errorMessage.value = "Error loading new content: ${e.message}"
            }
        }
    }

    fun loadMoreContent(isScrollingDown: Boolean) {
        scrollJob?.cancel() // Cancel previous scroll job
        scrollJob = viewModelScope.launch {
            delay(200) // Debounce scroll events
            if (isScrollingDown) {
                if (currentPosition + chunkSize < fileSize) {
                    currentPosition += chunkSize
                    loadChunk()
                }
            } else {
                if (currentPosition - chunkSize >= 0) {
                    currentPosition -= chunkSize
                    loadChunk()
                }
            }
            isAtEnd = currentPosition + chunkSize >= fileSize
        }
    }

    fun jumpToPosition(progress: Float) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val targetPosition = (fileSize * progress).toLong()
                    currentPosition = targetPosition - chunkSize.coerceAtMost(targetPosition)
                    if (currentPosition < 0) currentPosition = 0
                    loadChunk()
                    isAtEnd = currentPosition + chunkSize >= fileSize
                }
            } catch (e: Exception) {
                errorMessage.value = "Error jumping to position: ${e.message}"
            }
        }
    }

    private fun loadChunk() {
        loadJob?.cancel() // Cancel previous load job
        loadJob = viewModelScope.launch(Dispatchers.IO) {
            randomAccessFile?.let { raf ->
                try {
                    raf.seek(currentPosition)
                    val buffer = ByteArray(chunkSize.toInt())
                    val bytesRead = raf.read(buffer)
                    if (bytesRead > 0) {
                        val content = String(buffer, 0, bytesRead)
                        val lines = content.split("\n")
                        contentMutex.withLock {
                            visibleContent.addAll(lines.map { ContentItem(itemIdCounter++, it) })
                        }
                        readingProgress.value = currentPosition.toFloat() / fileSize.toFloat()
                    }
                } catch (e: Exception) {
                    errorMessage.value = "Error reading file: ${e.message}"
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        randomAccessFile?.close()
    }
}