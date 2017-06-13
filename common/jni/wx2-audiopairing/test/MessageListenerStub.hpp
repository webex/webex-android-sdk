#ifndef __Alto__MessageListenerStub__
#define __Alto__MessageListenerStub__

#include "MessageAssembler.hpp"
#include "MessageListener.hpp"

#include <stddef.h>

class MessageListenerStub : public MessageListener
{
public:
    MessageListenerStub(const unsigned char * expect, size_t size);
    ~MessageListenerStub();
    virtual void onMessage(const unsigned char * message, int length);

    size_t size_;
    unsigned char* expected_;
    int messageCount_;
    int errorCount_;
};
#endif /* defined(__Alto__MessageListenerStub__) */
