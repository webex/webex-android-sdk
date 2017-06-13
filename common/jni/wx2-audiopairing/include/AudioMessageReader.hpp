#ifndef __Alto__AudioMessageReader__
#define __Alto__AudioMessageReader__

#include "AudioAnalyzer.hpp"
#include "RingBuffer.hpp"
#include "MessageListener.hpp"
#include <cstddef>

class MessageReceiver;
class MessageAssembler;

class AudioMessageReader : public MessageListener {
public:
    AudioMessageReader(MessageReceiver * messageReceiver);
    AudioMessageReader(MessageReceiver * messageReceiver, const AudioParams& params);
    ~AudioMessageReader();
    void receiveAudioData(const void * data, size_t length);
    virtual void onMessage(const unsigned char * message, int length);
    float getUltrasoundSignalLevel () const;
    float getUltrasoundNoiseLevel () const;
    void setFrequencies(int from0, int width0, int from1, int width1, int from2, int width2);

private:
    void init(const AudioParams& params);
    AudioAnalyzer * audioAnalyzer;
    RingBuffer ringBuffer;
    MessageAssembler * messageAssembler;
    MessageReceiver * messageReceiver;
    float sampleRateHz;
};
#endif /* defined(__Alto__AudioMessageReader__) */
