// IFFmpegService.aidl
package com.example.audioandvideoeditor;

// Declare any non-default types here with import statements

interface IFFmpegService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
            double aDouble, String aString);

    /** Request the process ID of this service. */
    int getPid();
    int createAndStartTask(
       in int[] int_arr,
       in long[]  long_arr,
       in String[] str_arr,
       in float[] float_arr
    );
    int getTaskState( long taskID);
    void releaseTask(long taskID);
    void cancelTask(long taskID);
    float getProgress(long taskID);
}