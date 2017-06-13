#ifndef __Alto__MessageReceiver__
#define __Alto__MessageReceiver__

struct ReceptionInfo {
    unsigned int bitsSinceLastMessage;
    unsigned int errorCorrectionCount;
    float noiseLevel;
    float signalLevel;
};

class MessageReceiver
{
public:
	virtual ~MessageReceiver() {};
    virtual void onMessage(const unsigned char * message, int length, ReceptionInfo receptionInfo) = 0;
};

#endif /* defined(__Alto__MessageReceiver__) */
