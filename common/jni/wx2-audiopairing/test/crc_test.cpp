extern "C" {
#include "crc.h"
}

#include "gtest/gtest.h"

TEST(crc_test, expected_crc) {
    unsigned char input [] = {10, 47, 244, 74};
    EXPECT_EQ(0x9b31b292, crc32(0, input, sizeof(input)));
}

TEST(crc_test, valid_crc_is_valid) {
    unsigned char valid [] = {0xa0, 0x86, 0x44, 0x00, 10, 47, 29, 182};
    EXPECT_TRUE(crc32_is_valid(valid, 8));
}

TEST(crc_test, invalid_crc_is_not_valid) {
    unsigned char invalid[]= {0x01, 0x02, 0x03, 0x04, 10, 47, 29, 182};
    EXPECT_FALSE(crc32_is_valid(invalid, 8));
}