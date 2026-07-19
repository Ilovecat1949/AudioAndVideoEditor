import android.os.Build
import com.example.audioandvideoeditor.dao.TasksDao
import com.example.audioandvideoeditor.entity.Task
import com.example.audioandvideoeditor.entity.TaskInfo
import com.example.audioandvideoeditor.services.TasksBinder
import com.example.audioandvideoeditor.utils.FilesUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicLong

/**
 * 第二版·稳定优化：任务数据仓库
 * 职责：
 * 1. 统一封装 跨进程Service(TasksBinder) 调用
 * 2. 统一封装 数据库(TasksDao) 操作
 * 3. 自动完成 TaskInfo(IPC) ↔ Task(业务) 转换
 * 4. 对外只提供业务数据，屏蔽底层IPC细节
 */
/**
 * 第二版·稳定优化：任务数据仓库
 * 职责：
 * 1. 统一封装 跨进程Service(TasksBinder) 调用（实时任务队列）
 * 2. 统一封装 数据库(TasksDao) 操作（历史任务持久化）
 * 3. 自动完成 TaskInfo(IPC) ↔ Task(业务) 转换
 * 4. 对外只提供业务数据，屏蔽底层IPC/数据库细节
 */
/**
 * 屏幕录制业务的全局生命周期状态
 * 属于领域层（Repository）的统一业务语言，彻底与 Service 的底层实现解耦
 */
enum class ScreenRecordingState {
    IDLE,          // 闲置状态 / 未开始录制
    PENDING,       // 启动中：用户刚点了开始，前台正在等待系统权限弹窗允许（此时UI可展示转圈，防重复点击）
    RECORDING,     // 正在录制中
    PAUSED         // 已暂停
}
class TaskRepository(
    private var tasksBinder: TasksBinder?,
    private val tasksDao: TasksDao
) {
// ==================== 🌟 录屏架构重构：全局唯一状态信任源 ====================

    // 内部可变的状态流，由 Repository 牢牢把控修改权，保证数据单向流动 (UDF)
    private val _recordingState = kotlinx.coroutines.flow.MutableStateFlow(ScreenRecordingState.IDLE)

    // 对外暴露的只读状态流，供所有前台 ViewModel 安全订阅，永不丢失状态
    val recordingState = _recordingState.asStateFlow()

    /**
     * 更新全局录屏状态的唯一安全出口
     * 后台 RecordingService 触发关键生命周期、或者发生异常时，只需调用此方法向全网广播状态
     */
    fun updateRecordingState(state: ScreenRecordingState) {
        _recordingState.value = state
    }

    // ==================== ID 生成 ====================
    private val taskIdGenerator = AtomicLong(0)

    init {
        CoroutineScope(Dispatchers.IO).launch {
            val maxId = tasksDao.getMaxTaskId() ?: 0
            taskIdGenerator.set(maxId + 1)
        }
    }

    fun setBinder(binder: TasksBinder?) {
        this.tasksBinder = binder
    }

    // ==================== 1. 前端唯一调用：启动任务（只用 TaskInfo） ====================
    fun startNewTask(taskInfo: TaskInfo): Long {
        val taskId = taskIdGenerator.getAndIncrement()
        taskInfo.long_arr.add(0, taskId)
        tasksBinder?.startTask(taskInfo)
        return taskId
    }

    // ==================== 2. 任务完成保存（Service 调用） ====================
    suspend fun saveTaskComplete(taskInfo: TaskInfo, taskId: Long, taskState: Int) {
        withContext(Dispatchers.IO) {
            val task = convertToTask(taskInfo, taskId, taskState)
            tasksDao.insertTask(task)
        }
    }

    // ==================== 🔥 核心：私有转换方法（内部专用，不暴露） ====================
    private fun convertToTask(taskInfo: TaskInfo, taskId: Long, state: Int): Task {
        val dateFormat = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault(Locale.Category.FORMAT))
        } else {
            SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        }
        return Task(
            task_id = taskId,
            type = taskInfo.int_arr[0],
            status = state,
            file_name = FilesUtils.getNameFromPath(taskInfo.str_arr[0]),
            path = taskInfo.str_arr[0],
            log_path =
                if(taskInfo.int_arr[0]!=2){
                    taskInfo.str_arr[1]
                }
            else{
                    taskInfo.str_arr[0]
                }
                ,
            date = dateFormat.format(Date())
        )
    }

    // ==================== 3. 前端统一获取：运行中任务 → 直接返回 Task ====================
    fun getRunningTasks(): List<Task> {
        return tasksBinder?.getRunningTasksQueue()?.map { info ->
            Task(
                task_id = info.long_arr[0],
                type = info.int_arr[0],
                file_name = FilesUtils.getNameFromPath(info.str_arr[0]) ,
                status = getTaskState(info.long_arr[0]),
                path = info.str_arr[0],
                log_path = info.str_arr[1],
                date = ""
            )
        } ?: emptyList()
    }

    // ==================== 4. 前端统一获取：等待中任务 → 直接返回 Task ====================
    fun getWaitingTasks(): List<Task> {
        return tasksBinder?.getWaitingTasksQueue()?.map { info ->
            Task(
                task_id = info.long_arr[0],
                type = info.int_arr[0],
                file_name = FilesUtils.getNameFromPath(info.str_arr[0]),
                status = 3, // 等待中状态
                path = info.str_arr[0],
                log_path = info.str_arr[1],
                date = ""
            )
        } ?: emptyList()
    }

    // ==================== 5. 前端统一获取：历史任务 → 直接返回 Task ====================
    suspend fun getHistoryTasks(): List<Task> {
        return withContext(Dispatchers.IO) {
            tasksDao.getAllTasks() ?: emptyList()
        }
    }
    suspend fun deleteTaskById(task_id: Long){
        tasksDao.deleteTaskById(task_id)
    }

    // ==================== 6. 统一代理 Service 方法 ====================
    fun cancelTask(taskId: Long) {
        tasksBinder?.cancelTask(taskId)
    }

    fun getTaskState(taskId: Long): Int {
        return tasksBinder?.getTaskState(taskId) ?: -1
    }

    fun getTaskProgress(taskId: Long): Float {
        return tasksBinder?.getTaskProgress(taskId) ?: 0f
    }

    fun getAVInfo(path: String): String{
        return tasksBinder?.getAVInfo(path)?:""
    }
    fun getFFmpegInfo(info_type:Int):String{
        return tasksBinder?.getFFmpegInfo(info_type)?:""
    }

    fun loadTasksByType(type:Int): Flow<List<Task>>{
        return tasksDao.loadTasksByType(type)
    }
    suspend fun getMaxTaskId(): Long?{
        return tasksDao.getMaxTaskId()
    }
    suspend fun insertTask(task: Task){
        tasksDao.insertTask(task)
    }

// 🌟 在 TaskRepository 类内部新增此方法
    /**
     * 安全持久化录屏成果数据
     * 采用全局异步流，彻底避免 Service 意外销毁导致的数据丢失
     */
    fun saveRecordingResult(filePath: String, mediaUri: android.net.Uri?,fileName: String) {
        // 1. 借助全局长存的外部作用域（或在 Repository 内部规范管理的 scope）确保即使组件死了也能写完
        // 这里我们先用最稳妥的 Dispatchers.IO 开启持久化
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val id = taskIdGenerator.getAndIncrement()
                val date = Date(System.currentTimeMillis())
                val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())

                // 3. 构建统一实体并直接写入 Dao
                val task = if (mediaUri != null) {
                    Task(
                        task_id = id, type = 4, status = 1, file_name = fileName ,uri = mediaUri.toString(), date = formatter.format(date)
                    )
                } else {
                    Task(
                        task_id = id, type = 4, status = 1, file_name = fileName ,path = filePath, date = formatter.format(date)
                    )
                }

                tasksDao.insertTask(task)

                // 🌟 重点大局观：因为你的 tasksListState 观察的是 tasksDao.loadTasksByType(4) 的 Flow 流，
                // 只要这里 insertTask 一执行，前台 Compose 的视频列表会【全自动刷新】，根本不需要手动调刷新！

            } catch (e: Exception) {
                android.util.Log.e("TaskRepository", "录屏历史落盘失败", e)
            }
        }
    }
}