package com.example.audioandvideoeditor.dao

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.audioandvideoeditor.entity.VideoInfo
import kotlin.math.max

class VideosPagingSource: PagingSource<Int, VideoInfo>() {
    private val STARTING_KEY=0
    //    private var KEY_NUM=0
//    private var page=0
//    private var mPageSize=100
    private lateinit var  contentResolver: ContentResolver
    fun setContentResolver(contentResolver: ContentResolver){
        this.contentResolver=contentResolver
    }
    private fun ensureValidKey(key: Int) = max(STARTING_KEY, key)

    override fun getRefreshKey(state: PagingState<Int, VideoInfo>): Int? {
        // TODO("Not yet implemented")
        val anchorPosition = state.anchorPosition ?: return null
//        val article = state.closestItemToPosition(anchorPosition) ?: return null
        return ensureValidKey(key = anchorPosition- (state.config.pageSize / 2))
    }
    private val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
    private val projection = arrayOf(
        MediaStore.Video.Media._ID,
        MediaStore.Video.Media.DATA,
        MediaStore.Video.Media.DISPLAY_NAME,
        MediaStore.Video.Media.DURATION,
        MediaStore.Video.Media.SIZE
    )
    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, VideoInfo> {
        //TODO("Not yet implemented")
        // Start paging with the STARTING_KEY if this is the first load
        val list=ArrayList<VideoInfo>()
        val start = params.key ?: STARTING_KEY
        // Load as many items as hinted by params.loadSize
//        val range = start.until(start + params.loadSize)
        val queryArgs = Bundle()

        // 设置倒序
        queryArgs.putInt(
            ContentResolver.QUERY_ARG_SORT_DIRECTION,
            ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
        )
        // 设置倒序条件--文件添加时间
        queryArgs.putStringArray(
            ContentResolver.QUERY_ARG_SORT_COLUMNS,
            arrayOf(MediaStore.Files.FileColumns.DATE_ADDED)
        )
        // 分页设置
//        queryArgs.putInt(ContentResolver.QUERY_ARG_OFFSET, start)
//        queryArgs.putInt(ContentResolver.QUERY_ARG_LIMIT, params.loadSize)
        val query = contentResolver.query(
            collection,
            projection,
            queryArgs,
            null
        )
        var i=0
        var isLast=false
        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
            val durationColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            cursor.move(start)
            while (cursor.moveToNext() && i < params.loadSize) {
                // Get values of columns for a given video.
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val duration = cursor.getInt(durationColumn)
                val size = cursor.getInt(sizeColumn)
                val path = cursor.getString(pathColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                    id
                )
                list.add(
                    VideoInfo(
                        id,//start+i,
                        contentUri,
                        path,
                        name,
                        duration,
                        size
                    )
                )
                // Stores column values and the contentUri in a local object
                // that represents the media file.
                i++
            }
            if(cursor.count==start+i){
                isLast=true
            }
        }
        val loadResult=LoadResult.Page(
            data =list,
            // Make sure we don't try to load items behind the STARTING_KEY
            prevKey = when (start) {
                STARTING_KEY -> null
                else -> ensureValidKey(key = start - params.loadSize)
            },
            //start+i-1 <=start
            nextKey = if(isLast) null else start+i
        )
        return loadResult
    }
}