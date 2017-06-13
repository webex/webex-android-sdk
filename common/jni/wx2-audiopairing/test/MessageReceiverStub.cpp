#include "MessageReceiverStub.hpp"

#include <memory.h>

MessageReceiverStub::MessageReceiverStub(const unsigned char * expect, size_t size) : size_(size), expected_(new unsigned char[size]), messageCount_(0), errorCount_(0)
{
    memcpy(expected_, expect, size);
}

MessageReceiverStub::~MessageReceiverStub()
{
    delete[]expected_;
}

void MessageReceiverStub::onMessage(const unsigned char * message, int length, ReceptionInfo receptionInfo)
{
    bool fail = false;
    for(size_t i = 0; i < size_; i++)
        if(message[i] != expected_[i])
            fail = true;
    if(fail)
        errorCount_++;
    else
        messageCount_++;
    latestReceptionInfo = receptionInfo;
}
