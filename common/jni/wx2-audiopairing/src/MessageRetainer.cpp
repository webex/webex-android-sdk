#include "MessageRetainer.hpp"

#include <memory>

MessageRetainer::MessageRetainer() : message_{0}, flag_(false), length_(0)
{
}

void MessageRetainer::onMessage(const unsigned char * message, int length, ReceptionInfo reception_info)
{
    memcpy(message_, message, length);
    flag_ = true;
    length_ = length;
    reception_info_ = reception_info;
}

bool MessageRetainer::get_and_reset_message_flag() {
    bool tmp = flag_;
    flag_ = false;
    return tmp;
}

const unsigned char * MessageRetainer::message() const {
    return message_;
}

int MessageRetainer::messageLength() const {
    return length_;
}

ReceptionInfo MessageRetainer::reception_info() const {
    return reception_info_;
}