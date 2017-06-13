package com.cisco.spark.android.media;


import com.cisco.spark.android.room.audiopairing.AudioDataListener;
import com.cisco.spark.android.room.audiopairing.PcmUtils;

import com.webex.wme.IWmeExternalRender;
import com.webex.wme.IWmeExternalRenderAnswerer;
import com.webex.wme.WmeAudioRawFormat;

import com.github.benoitdion.ln.Ln;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

public class MediaAudioPairingExternalRender implements IWmeExternalRender {

    private AudioDataListener audioDataListener;

    private int rawDataLength = 0;
    private FloatBuffer audioDataBuffer;

    public MediaAudioPairingExternalRender(AudioDataListener audioDataListener) {
        Ln.i("WmeAudioSampler, register audioDataListener: %s", audioDataListener);
        this.audioDataListener = audioDataListener;
    }

    /**
     * This method is called continuously while we have media on the local device
     *
     * @param timestamp Not implemented always 0
     * @param mediaType AudioRaw(0) or VideoRaw(2)
     * @param formatMetaDataObject AudioRaw or VideoRaw format based on mediaType
     * @param rawData
     * @param length size of rawData
     */
    @Override
    public void RenderMediaData(int timestamp, int mediaType, Object formatMetaDataObject, byte[] rawData,
                                int length) {
        if (mediaType == 0) {
            if (length != rawDataLength) {

                WmeAudioRawFormat audioFormat = (WmeAudioRawFormat) formatMetaDataObject;
                int sampleRate = audioFormat.GetSampleRate();
                int bitsPerSample = audioFormat.GetBitsPerSample();

                Ln.i("WmeAudioSampler, RawAudio size changed, create new buffer: %d bitsPerSample: %d sampleRate: %d", length, bitsPerSample, sampleRate);
                ByteBuffer bb = ByteBuffer.allocateDirect(length * 2);
                bb.order(ByteOrder.nativeOrder());
                audioDataBuffer = bb.asFloatBuffer();
                rawDataLength = length;
            }
            PcmUtils.toFloatData(rawData, length / 2, 16, audioDataBuffer);
            audioDataListener.audioDataAvailable(audioDataBuffer);
        }
    }
    @Override
    public long RegisterRequestAnswerer(IWmeExternalRenderAnswerer answerer) {
        return -1;
    }

    @Override
    public long UnregisterRequestAnswerer() {
        return -1;
    }

}
