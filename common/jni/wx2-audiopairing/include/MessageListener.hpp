#ifndef __Alto__MessageListener__
#define __Alto__MessageListener__

class MessageListener
{
public:
	virtual ~MessageListener() {};
    virtual void onMessage(const unsigned char * message, int length) = 0;
};

#endif /* defined(__Alto__MessageListener__) */
