#ifndef __Alto__MessageReceiverStub__
#define __Alto__MessageReceiverStub__

#include "MessageAssembler.hpp"
#include "MessageReceiver.hpp"

#include <stddef.h>

class MessageReceiverStub : public MessageReceiver
{
public:
    MessageReceiverStub(const unsigned char * expect, size_t size);
    ~MessageReceiverStub();
    virtual void onMessage(const unsigned char * message, int length, ReceptionInfo receptionInfo);

    size_t size_;
    unsigned char* expected_;
    int messageCount_;
    int errorCount_;
    ReceptionInfo latestReceptionInfo;
};
#endif /* defined(__Alto__MessageReceiverStub__) */
