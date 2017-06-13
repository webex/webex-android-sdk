#include "MessageListenerStub.hpp"

#include <memory.h>

MessageListenerStub::MessageListenerStub(const unsigned char * expect, size_t size) : size_(size), expected_(new unsigned char[size]), messageCount_(0), errorCount_(0)
{
    memcpy(expected_, expect, size);
}

MessageListenerStub::~MessageListenerStub()
{
    delete[]expected_;
}

void MessageListenerStub::onMessage(const unsigned char * message, int length)
{
    bool fail = false;
    for(size_t i = 0; i < size_; i++)
        if(message[i] != expected_[i])
            fail = true;
    if(fail)
        errorCount_++;
    else
        messageCount_++;
}
