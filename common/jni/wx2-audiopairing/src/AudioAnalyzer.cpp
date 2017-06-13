#include "AudioAnalyzer.hpp"

#include "fft.h"

#include <cassert>
#include <cmath>
#include <memory>

AudioAnalyzer::AudioAnalyzer (int fft_length, const AudioParams& params) :
    length(fft_length),
    sliding_avg_conv_factor_base(params.slidingAvgFactor),
    sliding_avg_conv_factor_addition(params.slidingAvgAddition),
    hzPerIndex(0),
    laneIdx(0),
    offsetAdjustment(0),
    signalLane(0),
    signalLaneBand{{0}},
    avgSignalLaneBand{{0}},
    avgSignalStrengthLane{0},
    avg_signal_strength_conv_factor(params.avgSignalStrengthConvFactor),
    offset_adjustment_factor(params.offsetAdjustmentFactor),
    magnitude_weight_diff(params.magnitudeWeightDiff),
    warmupStart(params.warmup),
    warmup(0),
    fadeTable(new float[fft_length/2]),
    reflectionFactor(params.reflectionFactor)
{
    assert(fft_length == 512);
    hzPerIndex = params.sampleRateHz / fft_length;

    setFrequencies(20585, 345, 20930, 345, 21274, 345);
    reset();
    
    // Cosine windowing function. (Tested Hamming, Hann, Blackman, Triangle and more...)
    // TODO: Test Nuttall, flat top, Rife-Vincent, triangle/rectangle, combinations...
    for(int i = 0; i < length/2; i++)
        fadeTable[i] = sinf(.5 * M_PI * i / (length/2 - 1));
}

AudioAnalyzer::~AudioAnalyzer ()
{
    delete [] fadeTable;
}

void AudioAnalyzer::analyze (float * samples)
{
    fade(samples);
    double alignhack[length/2];
    float * scratchpad = reinterpret_cast<float *>(alignhack);
    const int fftStart = bands[0].startIdx;
    const int fftWidth = bands[2].startIdx + bands[2].width - bands[0].startIdx;
    fft_fftReal512SparseProcess(samples, scratchpad, fftStart, fftWidth);

    for (int band = 0; band < 3; band++) {
        float magnitudes[bands[band].width];
        for (int i = 0; i < bands[band].width; i++) {
            const float re = samples[2 * (bands[band].startIdx + i)];
            const float im = samples[2 * (bands[band].startIdx + i) + 1];
            magnitudes[i] = re * re + im * im;
        }

        float e = fminf(energy(magnitudes, bands[band].width), 1.0f);

        float factor = sliding_avg_conv_factor_base + warmup * sliding_avg_conv_factor_addition;
        avgSignalLaneBand[laneIdx][band] = avgSignalLaneBand[laneIdx][band] * (1.0F - factor) + e * factor;

        signalLaneBand[laneIdx][band] = e - 0.5 * avgSignalLaneBand[laneIdx][band]
                                          - 0.25 * (avgSignalLaneBand[(laneIdx+1)%3][band] +
                                                    avgSignalLaneBand[(laneIdx+2)%3][band]) -
                                        signalLaneBand[laneIdx][band] * reflectionFactor;
    }
    updateSignalStrength();
    updateOffsetAdjustment();
    selectPrimaryLane();
    
    laneIdx = (laneIdx+1)%3;
    if(warmup)
        warmup--;
}

bool AudioAnalyzer::is_data_ready () const
{
    return ((signalLane +1)%3) == laneIdx;
}

float AudioAnalyzer::get_bit_in_band (int band) const
{
    return signalLaneBand[(signalLane+3-band)%3][band];
}

int AudioAnalyzer::get_offset_adjustment () const
{
    return offsetAdjustment;
}

void AudioAnalyzer::setFrequencies(int from0, int width0, int from1, int width1, int from2, int width2)
{
    set_band(0, from0, width0);
    set_band(1, from1, width1);
    set_band(2, from2, width2);
}

void AudioAnalyzer::reset()
{
    laneIdx = 0;
    offsetAdjustment = 0;
    signalLane = 0;
    for(int lane = 0; lane < 3; lane++) {
        avgSignalStrengthLane[lane] = 0.0f;
        for(int b = 0; b < 3; b++) {
            signalLaneBand[lane][b] = 0.0f;
            avgSignalLaneBand[lane][b] = 0.0f;
        }
    }
    warmup = warmupStart;
}

float AudioAnalyzer::get_ultrasound_signal_level () const
{
    float signal = 0.0f;
    for(int band = 0; band < 3; band++) {
        signal +=
        avgSignalLaneBand[(signalLane+3-band)%3][band]*2
        -avgSignalLaneBand[(signalLane+4-band)%3][band]
        -avgSignalLaneBand[(signalLane+5-band)%3][band];
    }
    return signal;
}

float AudioAnalyzer::get_ultrasound_noise_level () const
{
    float signal = 0.0f;
    for(int band = 0; band < 3; band++) {
        signal += avgSignalLaneBand[0][band];
        signal += avgSignalLaneBand[1][band];
        signal += avgSignalLaneBand[2][band];
    }
    return signal;
}

void AudioAnalyzer::set_band (int idx, int start_freq_hz, int width_hz)
{
    bands[idx].startIdx = .5 + start_freq_hz/hzPerIndex;
    bands[idx].width = .5 + (start_freq_hz + width_hz)/hzPerIndex - bands[idx].startIdx;
}

void AudioAnalyzer::fade(float*data)
{
    for(int i = 0; i < length/2; i++) {
        data[i] *= fadeTable[i];
        data[-i+length-1] *= fadeTable[i];
    }
}

void AudioAnalyzer::updateSignalStrength()
{
    const float FACTOR = avg_signal_strength_conv_factor;
    
    float signalStrength = 0.0f;
    signalStrength += fabsf(signalLaneBand[laneIdx][0]);
    signalStrength += fabsf(signalLaneBand[(laneIdx+2)%3][1]);
    signalStrength += fabsf(signalLaneBand[(laneIdx+1)%3][2]);
    avgSignalStrengthLane[laneIdx] = avgSignalStrengthLane[laneIdx] * (1-FACTOR) + signalStrength * FACTOR;
}

void AudioAnalyzer::updateOffsetAdjustment()
{
     // TODO: improve adaption algorithm. Fast and precise adaption.
     // Predict optimal adjustment based on all 3 strength variables?
     // Modify avgSignalStrengthLane when adjusting offset to reflect what the vaues would have been if the adjustment was made earlier?

    if(laneIdx == signalLane) {
        // TODO: Can we use warmup to improve offset adjustment? (reset offset adjustment warmup counter on signal lane change?)
        float diff = avgSignalStrengthLane[(signalLane+1)%3] - avgSignalStrengthLane[(signalLane+2)%3];
        offsetAdjustment = diff * offset_adjustment_factor;

        if(offsetAdjustment > MAX_OFFSET_ADJUSTMENT)
            offsetAdjustment = MAX_OFFSET_ADJUSTMENT;
        else if(offsetAdjustment < -MAX_OFFSET_ADJUSTMENT)
            offsetAdjustment = -MAX_OFFSET_ADJUSTMENT;
    }
}

void AudioAnalyzer::selectPrimaryLane ()
{
    if(laneIdx == signalLane) {
        if(avgSignalStrengthLane[0] < avgSignalStrengthLane[1]) {
            if(avgSignalStrengthLane[1] < avgSignalStrengthLane[2])
                signalLane = 2;
            else
                signalLane = 1;
        }
        else {
            if(avgSignalStrengthLane[0]<avgSignalStrengthLane[2])
                signalLane = 2;
            else
                signalLane = 0;
        }
    }
}

float AudioAnalyzer::energy(float * data, int count)
{
    float sum = 0.0f;
    for (int i = 0; i<count; i++) {
        // Weighting of fft magnitudes gives a slight improvement.
        sum += data[i] * (1.0 - count*(.5*magnitude_weight_diff) + i * magnitude_weight_diff);
    }
    return sum/count;
}
