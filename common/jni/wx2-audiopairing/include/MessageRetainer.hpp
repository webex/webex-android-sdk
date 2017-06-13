#ifndef __Alto__MessageRetainer__
#define __Alto__MessageRetainer__

#include "MessageAssembler.hpp"
#include "MessageReceiver.hpp"

class MessageRetainer : public MessageReceiver {
public:
    MessageRetainer();
    virtual void onMessage(const unsigned char * message, int length, ReceptionInfo reception_info);
    bool get_and_reset_message_flag();
    const unsigned char * message() const;
    int messageLength() const;
    ReceptionInfo reception_info() const;
private:
    unsigned char message_[256];
    bool flag_;
    int length_;
    ReceptionInfo reception_info_;
};
#endif /* defined(__Alto__MessageRetainer__) */
