#include "MessageAssembler.hpp"
#include "MessageListener.hpp"
#include <cmath>

extern "C" {
    #include "crc.h"
}

namespace
{
    unsigned int hammingWeight(int n)
    {
        unsigned int c;
        for (c = 0; n; c++)
            n &= n - 1;
        return c;
    }
}

MessageAssembler::MessageAssembler(MessageListener * listener, int bitFlips, float averageBitsConvergenceFactor) :
		messageListener(listener),
		bits{0},
        oldBits{0},
        avgBits{0},
		idx(0),
		indexes{0},
		bitFlips_(bitFlips),
		bitsSinceLastMessage(0),
        avgBitsConvFactor(averageBitsConvergenceFactor),
        errorCorrectionCount(0)
{
    for(unsigned int i = 0; i < MESSAGE_BIT_LEN; i++)
        indexes[i] = i;
}

void MessageAssembler::addBit(float bit)
{
    bitsSinceLastMessage++;

    oldBits[idx] = bits[idx];
    bits[idx] = bit;
    avgBits[idx] = avgBits[idx]*avgBitsConvFactor + bit*(1-avgBitsConvFactor);
    idx = (idx +1)%(MESSAGE_BIT_LEN);
}

void MessageAssembler::symbolAdded()
{
    sortIndexes ();
    if(bitsSinceLastMessage >= MESSAGE_BIT_LEN)
        testForMessage();
}

unsigned int MessageAssembler::getErrorCorrectionCount() {
    return errorCorrectionCount;
}

unsigned int MessageAssembler::getBitsSinceLastMessage() {
    return bitsSinceLastMessage;
}

void MessageAssembler::testForMessage() {
    bool messageFound = false;
    for(int mask = 0; mask < 1<<bitFlips_ && !messageFound; mask++) {
        // TODO: test flipping on only one frequency.
        flipBits(mask);
        errorCorrectionCount = hammingWeight(mask);
        messageFound = testBitArrayForMessage(bits);
        flipBits(mask);
    }
    errorCorrectionCount = 0;
    if(!messageFound) {
        testBitArrayForMessage(avgBits);
    }
}

bool MessageAssembler::testBitArrayForMessage(float * bitArray)
{
    bool messageFound = false;
    unsigned char bytes[12] = {0,};
    for(unsigned int i = 0; i < MESSAGE_BIT_LEN; i++) {
        if(bitArray[i]>0)
            bytes[i/8] |= 1<<(i%8);
    }

    for(unsigned int j = MESSAGE_SYMBOL_LEN-1; ; j--) {
        if(crc32_is_valid(bytes, MESSAGE_BYTE_LEN)){
            messageListener->onMessage(bytes+4, MESSAGE_BYTE_LEN - 4);
            bitsSinceLastMessage = 0;
            messageFound = true;
            for(unsigned int i = 0; i < MESSAGE_BIT_LEN; i++)
                avgBits[i] = oldBits[i] = bits[i] = 0.0f;
            break;
        }

        if(!j)
            break;

        // Circular 3 bit shift of 81 bit buffer hand optimized
        unsigned int* p = reinterpret_cast<unsigned int*>(bytes);
        unsigned int tmp = *p&7;
        *p = *p>>3 | p[1]<<29;
        p++; // NOTE: "*p++ = ..." results in compiler warning: Unsequenced modification and access to 'p'
        *p = *p>>3 | p[1]<<29;
        p++;
        *p = *p>>3 | tmp<<14;
    }
    return messageFound;
}

void MessageAssembler::flipBits(int mask)
{
    for(int flip = 0; flip < bitFlips_; flip++)
        if((mask & (1<<flip)) == (1<<flip))
            bits[indexes[flip]] = -bits[indexes[flip]];
}

void MessageAssembler::sortIndexes()
{
    // Data size is small and data is close to presorted, so we use insertion sort. (Averages on 143 comparisons)
    for(unsigned int i = 0; i < MESSAGE_BIT_LEN-1; i++) {
        for(int j = i; j >= 0; j--) {
            if(compareBits(j, j+1) > 0) {
                int tmp = indexes[j];
                indexes[j] = indexes[j+1];
                indexes[j+1] = tmp;
            } else break;
        }
    }
}

int MessageAssembler::compareBits(int a, int b) {
    int r = 0;
    if(bits[indexes[a]]>0 == oldBits[indexes[a]]>0)
        r+=2;
    if(bits[indexes[b]]>0 == oldBits[indexes[b]]>0)
        r-=2;
    if(fabsf(bits[indexes[a]]) > fabsf(bits[indexes[b]]))
        r+=1;
    else
        r-=1;
    return r;
}
