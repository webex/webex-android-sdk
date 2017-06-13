#include "MessageReceiverStub.hpp"

#include "AudioMessageReader.hpp"
#include "MessageRetainer.hpp"

#include "gtest/gtest.h"

#include <iostream>
#include <fstream>

class AudioMessageReaderTest : public testing::Test
{
public:
    AudioMessageReaderTest();
    ~AudioMessageReaderTest();

    void processRecording(std::string fileName);
    void processRecording(std::string fileName, AudioMessageReader& audioMessageReader);

    void messagePattern(unsigned char * expected, size_t size);

    MessageReceiverStub * receiverStub;
};

AudioMessageReaderTest::AudioMessageReaderTest() : receiverStub(0)
{
    messagePattern(0,0);
}

AudioMessageReaderTest::~AudioMessageReaderTest()
{
    delete receiverStub;
}

void AudioMessageReaderTest::processRecording(std::string fileName)
{
    AudioMessageReader audioMessageReader(receiverStub);
    processRecording(fileName, audioMessageReader);
}

void AudioMessageReaderTest::processRecording(std::string fileName, AudioMessageReader& audioMessageReader)
{
    std::streampos size;
    char data[441];
    fileName.insert(0, "test/data/");
    std::ifstream file (fileName.c_str(), std::ios::in|std::ios::binary|std::ios::ate);
    ASSERT_TRUE(file.is_open());
    if (file.is_open())
    {
        size = file.tellg();
        file.seekg (0, std::ios::beg);

        size_t i;
        for(i = size; i > 441; i -= 441) {
            file.read(data, 441);
            audioMessageReader.receiveAudioData(data, 441);
        }
        file.read(data, i);
        audioMessageReader.receiveAudioData(data, i);

        file.close();
    }
}

void AudioMessageReaderTest::messagePattern(unsigned char * expected, size_t size)
{
    delete receiverStub;
    receiverStub = new MessageReceiverStub(expected, size);
}

TEST_F(AudioMessageReaderTest, test_recording_with_strong_signal)
{
    unsigned char expect[] = {10, 47, 26, 248, 0xb0, 0xff, 0x00};
    messagePattern(expect, 7);
    processRecording("close.raw");
    EXPECT_EQ(receiverStub->messageCount_, 5);
}

TEST_F(AudioMessageReaderTest, message_retainer_stores_message)
{
 	MessageRetainer retainer;
 	AudioMessageReader reader(&retainer);
 	EXPECT_FALSE(retainer.get_and_reset_message_flag());
 	processRecording ("close.raw", reader);
 	EXPECT_TRUE(retainer.get_and_reset_message_flag());

	EXPECT_EQ(retainer.messageLength(), 7);
	
	unsigned char expectedToken[] = {10, 47, 26, 248, 0xb0, 0xff, 0x00};
 	for(size_t i = 0; i < 7; i++) {
 		EXPECT_EQ(retainer.message()[i], expectedToken[i]);
	}
}

TEST_F(AudioMessageReaderTest, test_recording_with_varying_signal)
{
    unsigned char expect[] = {10, 47, 26, 248, 0xb0, 0xff, 0x00};
    messagePattern(expect, 7);
    processRecording("moving.raw");
    EXPECT_EQ(receiverStub->messageCount_, 33);
}

TEST_F(AudioMessageReaderTest, test_recording_rowan)
{
    unsigned char expect[] = {10, 16, 38, 98};
    messagePattern(expect, 4);
    processRecording("rowan.raw");
    EXPECT_EQ(receiverStub->messageCount_, 133);
}

TEST_F(AudioMessageReaderTest, test_complete_recording_set)
{
    processRecording("moving.raw");
    processRecording("rowan.raw");
    processRecording("close.raw");

    processRecording("a18_2_4s.raw");
    processRecording("a18_2_5.raw");
    processRecording("a18_2_ipad2.raw");
    processRecording("a18_4s.raw");
    processRecording("a18_5.raw");
    processRecording("a18_ipad2.raw");

    processRecording("babelb_ipad2.raw");
    processRecording("babelb5.raw");
    processRecording("babelb_iphone4s.raw");
    processRecording("babelh5.raw");
    processRecording("babelh_ipad2.raw");
    processRecording("babelh_iphone4s.raw");
    processRecording("babelv5.raw");
    processRecording("babelv_ipad2.raw");
    processRecording("babelv_iphone4s.raw");

    processRecording("botte_4s.raw");
    processRecording("botte_5.raw");
    processRecording("botte_ipad2.raw");
    processRecording("botte_2_4s.raw");
    processRecording("botte_2_5.raw");
    processRecording("botte_2_ipad2.raw");

    processRecording("caprino_4s.raw");
    processRecording("caprino_5.raw");
    processRecording("caprino_ipad2.raw");
    processRecording("caprinob5.raw");
    processRecording("caprinob_ipad2.raw");
    processRecording("caprinob_iphone4s.raw");
    processRecording("caprinof5.raw");
    processRecording("caprinof_ipad2.raw");
    processRecording("caprinof_iphone4s.raw");

    processRecording("carrerab5.raw");
    processRecording("carrerab_ipad2.raw");
    processRecording("carreraf5.raw");
    processRecording("carreraf_ipad2.raw");
    processRecording("carreraf_iphone4s.raw");
    processRecording("carrerab_iphone4s.raw");

    processRecording("edison_4s.raw");
    processRecording("edison_5.raw");
    processRecording("edison_ipad2.raw");

    processRecording("hanoi.4s");
    processRecording("hanoi.5");
    processRecording("hanoi.ipad2");
    processRecording("hanoi_5.raw");
    processRecording("hanoi_ipad2.raw");
    processRecording("hanoi_4s.raw");
    processRecording("toh_long.raw");

    processRecording("tt.4s");
    processRecording("tt.5");
    processRecording("tt.ipad2");
    processRecording("tt_one_speaker.5");
    processRecording("tt_one_speaker.ipad2");
    processRecording("ttankb_ipad2.raw");
    processRecording("ttankh5.raw");
    processRecording("ttankh_iphone4s.raw");
    processRecording("ttankv5.raw");
    processRecording("ttankv_ipad2.raw");
    processRecording("ttankv_iphone4s.raw");
    processRecording("ttankb5.raw");
    processRecording("ttankb_iphone4s.raw");

    EXPECT_EQ(receiverStub->messageCount_, 1354);
}

TEST_F(AudioMessageReaderTest, eavesdropping_babelfish)
{
    unsigned char expect[] = {10,  54,  74,  78,  12,  45,  0};
    messagePattern(expect, 7);
    processRecording ("spy.raw");
    EXPECT_EQ(receiverStub->messageCount_, 2);
}

TEST_F(AudioMessageReaderTest, signal_level_reporting)
{
    AudioMessageReader messageReader(receiverStub);
    processRecording ("close.raw", messageReader);

    float signal = messageReader.getUltrasoundSignalLevel();

    AudioMessageReader noiseReader(receiverStub);
    processRecording ("nosignal.raw", noiseReader);
    float nosignal = noiseReader.getUltrasoundSignalLevel();

    EXPECT_PRED_FORMAT2(::testing::FloatLE, 0.01, signal);
    EXPECT_PRED_FORMAT2(::testing::FloatLE, nosignal, 0.00001);
}

TEST_F(AudioMessageReaderTest, sample_rate_48_kHz)
{
    const unsigned char expected[] = {10, 47, 13, 113, 0x51, 0x18, 0x01};
    MessageReceiverStub receiver(expected, sizeof expected / sizeof *expected);
    AudioMessageReader reader(&receiver, AudioParams(48000.0f));
    processRecording("carbon48khz.raw", reader);
    EXPECT_EQ(receiver.errorCount_, 0);
    EXPECT_EQ(receiver.messageCount_, 6);
}
