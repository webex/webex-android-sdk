#ifndef Alto_crc_h
#define Alto_crc_h
#include <stddef.h>

unsigned short crc16(unsigned char *data_p, int length);
int crc16_is_valid(unsigned char * data, int length);

unsigned int crc32(unsigned int crc, const void *buf, size_t size);
int crc32_is_valid(unsigned char * data, int length);

#endif
