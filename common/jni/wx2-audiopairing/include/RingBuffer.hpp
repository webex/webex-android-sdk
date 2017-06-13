#ifndef __Alto__RingBuffer__
#define __Alto__RingBuffer__

#include <cstddef>

class RingBuffer
{
public:
    RingBuffer(size_t capacity);
    ~RingBuffer();
    void write(const char * data, size_t length);
    void peak(char * dest, size_t length) const;
    void advance(size_t length);
    size_t size() const;
private:
    const size_t capacity_;
    char * buffer_;
    char * read_;
    char * write_;
};
#endif /* defined(__Alto__RingBuffer__) */
