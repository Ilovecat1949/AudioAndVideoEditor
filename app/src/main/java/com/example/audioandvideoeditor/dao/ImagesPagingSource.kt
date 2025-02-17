package com.example.audioandvideoeditor.dao

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.example.audioandvideoeditor.entity.ImageInfo
import kotlin.math.max

class ImagesPagingSource: PagingSource<Int, ImageInfo>() {
    private  val TAG="ImagesPagingSource"
    private val STARTING_KEY=0
    //    private var KEY_NUM=0
//    private var page=0
//    private var mPageSize=100
    private lateinit var  contentResolver: ContentResolver
    fun setContentResolver(contentResolver: ContentResolver){
        this.contentResolver=contentResolver
    }
    private fun ensureValidKey(key: Int) = max(STARTING_KEY, key)

    override fun getRefreshKey(state: PagingState<Int, ImageInfo>): Int? {
        // TODO("Not yet implemented")
        val anchorPosition = state.anchorPosition ?: return null
//        val article = state.closestItemToPosition(anchorPosition) ?: return null
        return ensureValidKey(key = anchorPosition- (state.config.pageSize / 2))
    }
    private val collection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Images.Media.getContentUri(
                MediaStore.VOLUME_EXTERNAL
            )
        } else {
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        }
    private val projection = arrayOf(
        MediaStore.Images.Media._ID,
        MediaStore.Images.Media.DATA,
        MediaStore.Images.Media.DISPLAY_NAME,
        MediaStore.Images.Media.HEIGHT,
        MediaStore.Images.Media.WIDTH,
        MediaStore.Images.Media.SIZE
    )

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, ImageInfo> {
        //TODO("Not yet implemented")
        // Start paging with the STARTING_KEY if this is the first load
        val list=ArrayList<ImageInfo>()
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
        var count=0
        query?.use { cursor ->
            // Cache column indices.
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val nameColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            val heightColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val widthColumn =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val pathColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
//            while(cursor.moveToNext()&&i<start-1){
//                i++
//            }
            cursor.move(start)
            while (cursor.moveToNext() && i < params.loadSize) {
                // Get values of columns for a given image
                val id = cursor.getLong(idColumn)
                val name = cursor.getString(nameColumn)
                val height = cursor.getInt(heightColumn)
                val width = cursor.getInt(widthColumn)
                val size = cursor.getLong(sizeColumn)
                val path = cursor.getString(pathColumn)
                val contentUri: Uri = ContentUris.withAppendedId(
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                    id
                )
//                    var bitmap: Bitmap? = null
//                contentResolver.openFileDescriptor(contentUri, "r").use {
//                    if(it!=null){
//                        bitmap= BitmapFactory.decodeFileDescriptor(it.fileDescriptor)
//                    }
//                }

                list.add(
                    ImageInfo(
                        id,
                        contentUri,
                        path,
                        name,
                        height,
                        width,
                        size
//                            bitmap
                    )
                )
//                if(cursor.isLast){
//                    isLast=true
//                }
                // Stores column values and the contentUri in a local object
                // that represents the media file.

                i++
            }
            count=cursor.count
            if(cursor.count==start+i){
                isLast=true
            }
        }
        val loadResult= LoadResult.Page(
            data =list,
            // Make sure we don't try to load items behind the STARTING_KEY
            prevKey = when (start) {
                STARTING_KEY -> null
                else -> ensureValidKey(key = start - params.loadSize)
            },
            nextKey = if(isLast) null else start+i
        )
        Log.d(TAG,"start:${start},  start+i:${start+i}  count:${count}")
        return loadResult
    }
}