#ifndef __Alto__AudioAnalyzer2__
#define __Alto__AudioAnalyzer2__

struct Frequency
{
    int startIdx;
    int width;
};

struct AudioParams {

public:
    AudioParams(float sampleRateHz = 44100.0f) :
    sampleRateHz(sampleRateHz),
    slidingAvgFactor(0.0345f),
    slidingAvgAddition(0.0028f),
    avgSignalStrengthConvFactor(0.0657f),
    offsetAdjustmentFactor(21.0f),
    magnitudeWeightDiff(0.15f),
    warmup(13),
    reflectionFactor(0.09f),
    ma_averageBitsConvFactor(.82f)
    {
    }

    float sampleRateHz;
    float slidingAvgFactor;
    float slidingAvgAddition;
    float avgSignalStrengthConvFactor;
    float offsetAdjustmentFactor;
    float magnitudeWeightDiff;
    int warmup;
    float reflectionFactor;
    
    float ma_averageBitsConvFactor;
};

class AudioAnalyzer {
public:
    AudioAnalyzer (int fft_length, const AudioParams& audioParams);
    virtual ~AudioAnalyzer();
    void analyze (float * data);
    bool is_data_ready () const;
    float get_bit_in_band (int band) const;
    int get_offset_adjustment () const;
    void reset();
    float get_ultrasound_signal_level () const;
    float get_ultrasound_noise_level () const;
    void setFrequencies(int from0, int width0, int from1, int width1, int from2, int width2);

private:
    void set_band (int idx, int start_freq_hz, int width_hz);
    void fade(float * samples);
    void updateSignalStrength();
    void updateOffsetAdjustment();
    void selectPrimaryLane();
    float energy(float * data, int count);

    const int length;
    const float sliding_avg_conv_factor_base;
    const float sliding_avg_conv_factor_addition;
    float hzPerIndex;
    Frequency bands[3];
    int laneIdx;
    int offsetAdjustment;
    int signalLane;
    float signalLaneBand[3][3];
    float avgSignalLaneBand[3][3];
    float avgSignalStrengthLane[3];

    const float avg_signal_strength_conv_factor;
    const float offset_adjustment_factor;
    const float magnitude_weight_diff;
    const int warmupStart;
    int warmup;
    float * fadeTable;
    float reflectionFactor;
    static const int MAX_OFFSET_ADJUSTMENT = 10;

};
#endif /* defined(__Alto__AudioAnalyzer2__) */
