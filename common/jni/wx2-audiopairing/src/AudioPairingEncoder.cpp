#include "AudioPairingEncoder.hpp"


#include <cassert>
#include <vector>
#include <fstream>
#include <sstream>
#include <iterator>

extern "C" {
    #include "crc.h"
}

#include <android/log.h>

#define TAG "AUDIOPAIRING_ENCODER"
#define LOGV(...) __android_log_print(ANDROID_LOG_VERBOSE, TAG, __VA_ARGS__)
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)



namespace {

std::vector<uint8_t> toBytes(uint32_t value)
{
    std::vector<uint8_t> bytes;
    bytes.push_back((value >> 24) & 0xff);
    bytes.push_back((value >> 16) & 0xff);
    bytes.push_back((value >> 8) & 0xff);
    bytes.push_back(value & 0xff);
    return bytes;
}

std::vector<uint8_t> pairing_crc32(const std::vector<uint8_t>& bytes)
{
    if (bytes.empty()) {
        return std::vector<uint8_t>();
    }

    uint32_t crc = crc32(0, &bytes[0], bytes.size());
    return toBytes(crc);
}


std::string symbolsToString(const std::vector<uint8_t>& symbols)
{
    std::stringstream ss;
    uint16_t size = symbols.size();
    for (uint16_t i = 0; i < size; ++i) {
        uint32_t value = symbols[i] + 8 * (symbols[(i+size-1) % size] & 1);
        ss << std::hex << value;
    }
    return ss.str();
}

std::string encode(const std::vector<uint8_t>& crc, const std::vector<uint8_t>& addr)
{
    const uint32_t BitsPerSymbol = 3;
    const uint32_t SymbolsPerMessage = 27;

    std::vector<uint8_t> bytes(addr);
    bytes.insert(bytes.begin(), crc.begin(), crc.end());

    uint8_t bitsInCurrentValue = 0;
    uint8_t currentValue = 0;
    std::vector<uint8_t> symbols;
    for (uint32_t i = 0; i < bytes.size(); ++i) {
        uint8_t bit = 1;
        for (uint8_t q = 0; q < 8; ++q) {
            if ((bytes[i] & bit) != 0) {
                currentValue |= (1 << bitsInCurrentValue);
            }
            if (++bitsInCurrentValue == BitsPerSymbol) {
                symbols.push_back(currentValue);
                currentValue = 0;
                bitsInCurrentValue = 0;
                if (symbols.size() == SymbolsPerMessage)
                    break;
            }
            bit <<= 1;
        }
    }

    return symbolsToString(symbols);
}


}



uint64_t parse_uint(const std::string& s, int base = 10)
{
    try {
        const unsigned long long ull = std::stoull(s, nullptr, base);
        return ull;
    }
    catch (const std::out_of_range&) {
        LOGE("out of range exception");
    }
    catch (const std::invalid_argument&) {
        LOGE("invalid argument exception");
    }
}


static std::vector<uint8_t> tokenToBytes(const std::string& token)
{
    const uint64_t ui = parse_uint(token, 16);

    std::vector<uint8_t> bits(6);
    bits[0] = static_cast<uint8_t>((ui >> 40) & 0xFF);
    bits[1] = static_cast<uint8_t>((ui >> 32) & 0xFF);
    bits[2] = static_cast<uint8_t>((ui >> 24) & 0xFF);
    bits[3] = static_cast<uint8_t>((ui >> 16) & 0xFF);
    bits[4] = static_cast<uint8_t>((ui >>  8) & 0xFF);
    bits[5] = static_cast<uint8_t>((ui >>  0) & 0xFF);

    return bits;
}

namespace AudioPairing {


std::string generatePlayoutString(const std::string token_string)
{
    const std::vector<uint8_t> bits = tokenToBytes(token_string);
    return generatePlayoutString(bits);
}

std::string generatePlayoutString(const std::vector<uint8_t>& data)
{
    assert(data.size() == 6);
    std::vector<uint8_t> bytes(data);
    bytes.push_back(0x00);
    const std::vector<uint8_t> crc = pairing_crc32(bytes);
    return encode(crc, bytes);
}

}