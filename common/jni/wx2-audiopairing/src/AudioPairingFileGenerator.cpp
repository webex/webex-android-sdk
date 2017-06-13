#include "AudioPairingFileGenerator.hpp"


#include <bitset>
#include <vector>
#include <fstream>
#include <iterator>
#include <stdlib.h>

#include <android/log.h>

#define TAG "AUDIOPAIRING_FILEGENERATOR"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)


static void uint32ToChar(uint32_t src, char * dest)
{
    dest[0] = static_cast<char>(src & 0xFF);
    dest[1] = static_cast<char>((src >> 8) & 0xFF);
    dest[2] = static_cast<char>((src >> 16) & 0xFF);
    dest[3] = static_cast<char>((src >> 24) & 0xFF);
}

static void writeWaveHeader(std::ofstream& file, uint32_t dataBytes)
{
    char chunkId[4] = {'R', 'I', 'F', 'F'};
    char chunkSize[4];
    uint32ToChar(dataBytes + 36, chunkSize); // rest of header contains 36 bytes
    char format[4] = {'W', 'A', 'V', 'E'};
    char subChunk1Id[4] = {'f', 'm', 't', ' '};
    char subChunk1Size[4] = {0x10, 0x00, 0x00, 0x00}; // 16 for PCM
    char audioFormat[2] = {0x01, 0x00}; // PCM
    char numChannels[2] = {0x01, 0x00}; // mono
    char sampleRate[4] = {char(0x80), char(0xbb), 0x00, 0x00}; // 48 kHz
    char byteRate[4] = {0x00, 0x77, 0x01, 0x00}; // 96000
    char blockAlign[2] = {0x02, 0x0}; // 2
    char bitsPerSample[2] = {0x10, 0x00}; // 16
    char subChunk2Id[4] = {'d', 'a', 't', 'a'};
    char subChunk2Size[4];
    uint32ToChar(dataBytes, subChunk2Size);

    file.write(chunkId, sizeof chunkId);
    file.write(chunkSize, sizeof chunkSize);
    file.write(format, sizeof format);
    file.write(subChunk1Id, sizeof subChunk1Id);
    file.write(subChunk1Size, sizeof subChunk1Size);
    file.write(audioFormat, sizeof audioFormat);
    file.write(numChannels, sizeof numChannels);
    file.write(sampleRate, sizeof sampleRate);
    file.write(byteRate, sizeof byteRate);
    file.write(blockAlign, sizeof blockAlign);
    file.write(bitsPerSample, sizeof bitsPerSample);
    file.write(subChunk2Id, sizeof subChunk2Id);
    file.write(subChunk2Size, sizeof subChunk2Size);
}

static bool readFile(const char *filename, std::vector<char> &data)
{
    std::ifstream file;
    static const std::vector<char>::size_type size_limit = 10000U;

    file.open(filename, std::ios::in|std::ios::binary);
    if (!file)
        return false;

    data.assign((std::istreambuf_iterator<char>(file)),
                (std::istreambuf_iterator<char>()));
    if (data.size() > size_limit)
        data.resize(size_limit);
    return true;
}

namespace AudioPairing {

int generatePairingSequenceFile(uint32_t loopInterval, const std::string& playoutString, const std::string playoutFileName, const std::string pairingSoundFilesPath)
{
    const int sampleFiles = 16;
    std::vector< std::vector<char> > sampleData(sampleFiles, std::vector<char>());

    for (size_t i = 0; i < sampleData.size(); i++) {
        char filename_user[40], filename_sounds[40];
        sprintf(filename_user, "%s/ap_%zx.raw", pairingSoundFilesPath.c_str(), i);
        if (!readFile(filename_user, sampleData[i])) {
            return -1;
        }
    }


    std::ofstream outputFile(playoutFileName.c_str(), std::ios::out|std::ios::binary);

    const unsigned int headerLength = 44U;
    if (!outputFile) {
        LOGE("AudioPairing.generatePairingSequenceFile, outFie is null, outputFileName = %s", playoutFileName.c_str());
        return -1;
    }
    outputFile.seekp(headerLength, std::ios::beg);

    uint32_t dataBytes = 0U;
    for (size_t i = 0; i < playoutString.size(); ++i) {
        const char charAsString[2] = { playoutString[i], '\0' };
        unsigned int j = strtoul(charAsString, NULL, 16);
        if (j >= sampleData.size()) {
            return -1;
        }
        outputFile.write(&sampleData[j][0], sampleData[j].size());
        dataBytes += sampleData[j].size();
    }

    // Add silence before repeat
    if (loopInterval) {
        const char silence[1] = {0x00};
        const int samplesPerMs = 48;
        const int bytesPerSample = 2;
        unsigned int intervalBytes = loopInterval * samplesPerMs * bytesPerSample;
        for (size_t i = 0; i < intervalBytes; i++)
            outputFile.write(silence, 1);
        dataBytes += intervalBytes;
    }
    outputFile.seekp(0, std::ios::beg);
    writeWaveHeader(outputFile, dataBytes);

    return 0;
}

}


