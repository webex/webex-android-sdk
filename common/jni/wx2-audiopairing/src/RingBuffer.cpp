#include "RingBuffer.hpp"

#include <memory>
#include <cassert>

RingBuffer::RingBuffer (size_t capacity) :
    capacity_(capacity),
    buffer_(new char[capacity_]),
    read_ (buffer_),
    write_(buffer_)
{
}

RingBuffer::~RingBuffer()
{
    delete[] buffer_;
}

void RingBuffer::write(const char * data, size_t length)
{
    assert(size() + length < capacity_);
    if(write_ + length > buffer_ + capacity_) {
        size_t first_chunk = buffer_ + capacity_ - write_;
        memcpy(write_, data, first_chunk);
        memcpy(buffer_, data + first_chunk, length - first_chunk);
        write_ = buffer_ + length - first_chunk;
    } else {
        memcpy(write_, data, length);
        write_ += length;
    }
}

void RingBuffer::peak(char * dest, size_t length) const
{
    assert(length <= size());
    size_t first_chunk = buffer_ + capacity_ - read_;
    if (length > first_chunk) {

        memcpy (dest, read_, first_chunk);
        memcpy (dest + first_chunk, buffer_, length - first_chunk);
    } else
        memcpy(dest, read_, length);
}

void RingBuffer::advance(size_t length)
{
    assert(length <= size());
    if(length > (unsigned int)(buffer_ + capacity_ - read_))
        read_ = read_ + length - capacity_;
    else
        read_ += length;
}

size_t RingBuffer::size() const
{
    return (write_ - read_ + capacity_) % capacity_;
}
