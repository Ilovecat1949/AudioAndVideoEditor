package com.example.audioandvideoeditor.viewmodel
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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
//                    Log.d(TAG,"visibleContent.size:${visibleContent.size}")
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

