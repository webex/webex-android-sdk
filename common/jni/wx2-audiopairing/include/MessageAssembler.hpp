#ifndef __Alto__MessageAssembler2__
#define __Alto__MessageAssembler2__

class MessageListener;

class MessageAssembler
{
public:
    MessageAssembler(MessageListener * listener, int bitFlips, float averageBitsConvergenceFactor);
    void addBit(float bit);
    void symbolAdded();
    unsigned int getErrorCorrectionCount();
    unsigned int getBitsSinceLastMessage();

private:
    static const unsigned int BITS_PER_SYMBOL = 3;
    static const unsigned int MESSAGE_BIT_LEN = 81;
    static const unsigned int MESSAGE_SYMBOL_LEN = (MESSAGE_BIT_LEN+2)/BITS_PER_SYMBOL;
    static const unsigned int MESSAGE_BYTE_LEN = (MESSAGE_BIT_LEN+7)/8;

    void testForMessage ();
    bool testBitArrayForMessage (float* bitArray);
    void sortIndexes();
    void flipBits(int mask);
    int compareBits(int i, int j);

    MessageListener * messageListener;
    float bits[MESSAGE_BIT_LEN];
    float oldBits[MESSAGE_BIT_LEN];
    float avgBits[MESSAGE_BIT_LEN];
    int idx;

    int indexes[MESSAGE_BIT_LEN];
    const int bitFlips_;
    unsigned int bitsSinceLastMessage;
    const float avgBitsConvFactor;
    unsigned int errorCorrectionCount;
};
#endif /* defined(__Alto__MessageAssembler2__) */
