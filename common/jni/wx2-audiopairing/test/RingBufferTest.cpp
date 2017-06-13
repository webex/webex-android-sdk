#include "RingBuffer.hpp"

#include "gtest/gtest.h"

void VerifyEqualBuffers(const char * actual, const char * expected, int length);

TEST(RingBufferTest, read_and_advance)
{
    RingBuffer ring(10);
    char bytes[] = {'a', 'b', 'c', 'd', 'e', 'f', 'a'};
    char copy[5];

    // append a,b,c,d,e,f
    ring.write(bytes, 6);

    // read and verify abc
    ring.peak(copy, 3);
    VerifyEqualBuffers(copy, bytes, 3);

    // advance 2 (setting buffer to cdef)
    ring.advance(2);

    // read and verify cde
    ring.peak(copy, 3);
    VerifyEqualBuffers(copy, bytes+2, 3);

    // advance 2 (setting buffer to ef
    ring.advance(2);

    // append abcdef
    ring.write(bytes, 6);

    // Available should now be efabcdef
    EXPECT_EQ(ring.size(), (size_t)8);

    // read and verify efa
    ring.peak(copy, 3);
    VerifyEqualBuffers(copy, bytes+4, 3);

    ring.advance(3);
    ring.peak(copy, 5);
    VerifyEqualBuffers(copy, bytes+1, 5);

    ring.advance(4);
    ring.peak(copy, 1);
    VerifyEqualBuffers(copy, bytes+5, 1);
}

void VerifyEqualBuffers(const char * actual, const char * expected, int length)
{
    EXPECT_EQ(strncmp(actual, expected, length), 0);
}
