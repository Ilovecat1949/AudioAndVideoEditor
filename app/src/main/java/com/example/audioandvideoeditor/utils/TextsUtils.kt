package com.example.audioandvideoeditor.utils

object TextsUtils {
    fun getSizeText(byte_size:Long):String{
       return if(byte_size<1024){
               "${byte_size}B"
       }
       else if(byte_size<1024*1024){
           String.format("%.2fKB", byte_size*1f/1024)
       }
       else if(byte_size<1024*1024*1024){
           String.format("%.2fMB",byte_size*1f/(1024*1024))
       }
       else{
           String.format("%.2fGB",byte_size*1f/(1024*1024*1024))
       }
   }
    fun millisecondsToString(milliseconds: Long): String {
        // Calculate hours, minutes, seconds, and milliseconds
        val hours = milliseconds / (60 * 60 * 1000)
        val minutes = (milliseconds % (60 * 60 * 1000)) / (60 * 1000)
        val seconds = (milliseconds % (60 * 1000)) / 1000
        val millis = (milliseconds % 1000) / 10 // We need only two decimal places for milliseconds

        // Format the values into the desired string format
        return String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, millis)
    }
}