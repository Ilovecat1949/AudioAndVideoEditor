//
// Created by deng on 2024/10/31.
//

#include "FFAudioFifo.h"
#include "../../utils/logger.h"

int FFAudioFifo::init(AVCodecContext *codec_ctx) {
    /* Create the FIFO buffer based on the specified output sample format. */
    codec_context=codec_ctx;
    if (!(fifo = av_audio_fifo_alloc(codec_context->sample_fmt,
                                     codec_context->channels, 1))) {
//        fprintf(stderr, "Could not allocate FIFO\n");
        LOGI(TAG,"Could not allocate FIFO\n")
        return AVERROR(ENOMEM);
    }
    return 0;
}
int FFAudioFifo::getSize() {
    return av_audio_fifo_size(fifo);
}
int FFAudioFifo::add_samples_to_fifo(uint8_t **converted_input_samples, const int frame_size) {
    int error;

    /* Make the FIFO as large as it needs to be to hold both,
     * the old and the new samples. */
    if ((error = av_audio_fifo_realloc(fifo, av_audio_fifo_size(fifo) + frame_size)) < 0) {
//        fprintf(stderr, "Could not reallocate FIFO\n");
        LOGI(TAG,"Could not reallocate FIFO\n")
        return error;
    }

    /* Store the new samples in the FIFO buffer. */
    if (av_audio_fifo_write(fifo, (void **)converted_input_samples,
                            frame_size) < frame_size) {
//        fprintf(stderr, "Could not write data to FIFO\n");
        LOGI(TAG,"Could not write data to FIFO\n")
        return AVERROR_EXIT;
    }
    return 0;
}
int FFAudioFifo::init_frame(AVFrame **frame, int frame_size) {
    int error;

    /* Create a new frame to store the audio samples. */
    if (!(*frame = av_frame_alloc())) {
//        fprintf(stderr, "Could not allocate output frame\n");
        LOGI(TAG,"Could not allocate output frame\n")
        return AVERROR_EXIT;
    }

    /* Set the frame's parameters, especially its size and format.
     * av_frame_get_buffer needs this to allocate memory for the
     * audio samples of the frame.
     * Default channel layouts based on the number of channels
     * are assumed for simplicity. */
    (*frame)->nb_samples     = frame_size;
    (*frame)->channel_layout = codec_context->channel_layout;
    (*frame)->format         = codec_context->sample_fmt;
    (*frame)->sample_rate    = codec_context->sample_rate;
    (*frame)->channels    = codec_context->channels;
    /* Allocate the samples of the created frame. This call will make
     * sure that the audio frame can hold as many samples as specified. */
    if ((error = av_frame_get_buffer(*frame, 0)) < 0) {
//        fprintf(stderr, "Could not allocate output frame samples (error '%s')\n",
//                av_err2str(error));
        LOGI(TAG,"Could not allocate output frame samples (error '%s')\n",av_err2str(error))
        av_frame_free(frame);
        return error;
    }

    return 0;
}
int FFAudioFifo::getAVFrame(AVFrame* &frame) {
    /* Temporary storage of the output samples of the frame written to the file. */
    /* Use the maximum number of possible samples per frame.
     * If there is less than the maximum possible frame size in the FIFO
     * buffer use this number. Otherwise, use the maximum possible frame size. */
    const int frame_size = FFMIN(av_audio_fifo_size(fifo),
                                 codec_context->frame_size);
    /* Initialize temporary storage for one output frame. */
    if (init_frame(&frame, frame_size)) {
        return AVERROR_EXIT;
    }
    /* Read as many samples from the FIFO buffer as required to fill the frame.
     * The samples are stored in the frame temporarily. */
    if (av_audio_fifo_read(fifo, (void **)frame->data, frame_size) < frame_size) {
        fprintf(stderr, "Could not read data from FIFO\n");
        LOGI(TAG,"Could not read data from FIFO\n")
        av_frame_free(&frame);
        frame=NULL;
        return AVERROR_EXIT;
    }
    frame->pts=pts;
    pts+=frame->nb_samples;
    return 0;
}
FFAudioFifo::~FFAudioFifo() {
    av_audio_fifo_free(fifo);
}
