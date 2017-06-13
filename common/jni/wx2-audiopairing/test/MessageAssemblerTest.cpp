#include "MessageAssembler.hpp"
#include "MessageListenerStub.hpp"


#include "gtest/gtest.h"

class MessageAssemblerTest : public testing::Test
{
public:
    MessageAssemblerTest();
    ~MessageAssemblerTest();
    void addSymbols(std::string symbols);
    void autoCorrectionLevel(int bitFlips);

    MessageListenerStub listener;
    MessageAssembler * messageAssembler;
    int bitValue;
};

MessageAssemblerTest::MessageAssemblerTest() :
    listener(0, 0),
    messageAssembler(0),
    bitValue(1)
{
    autoCorrectionLevel(0);
}

MessageAssemblerTest::~MessageAssemblerTest()
{
    delete messageAssembler;
}

void MessageAssemblerTest::addSymbols (std::string symbols)
{
    for(size_t i = 0; i < symbols.size(); ++i)
    {
        std::stringstream ss;
        unsigned int symbol;
        ss << std::hex << symbols[i];
        ss >> symbol;

        messageAssembler->addBit((symbol&1) != 0? bitValue : -bitValue);
        messageAssembler->addBit((symbol&2) != 0? bitValue : -bitValue);
        messageAssembler->addBit((symbol&4) != 0? bitValue : -bitValue);
        messageAssembler->symbolAdded();
    }
}

void MessageAssemblerTest::autoCorrectionLevel(int bitFlips)
{
    delete messageAssembler;
    messageAssembler = new MessageAssembler(&listener, bitFlips, .86);
}

TEST_F(MessageAssemblerTest, receive_message)
{
    addSymbols("863f9a1c23bd8639a38479c5ffb");
    EXPECT_EQ(listener.messageCount_, 1);
}

TEST_F(MessageAssemblerTest, receive_message_with_alto_flag_set)
{
    addSymbols("f8641a5dc19d8639a38479c5fff");
    EXPECT_EQ(listener.messageCount_, 1);
}

TEST_F(MessageAssemblerTest, receive_message_with_zeroes_in_front)
{
    addSymbols("000863f9a1c23bd8639a38479c5ffb");
    EXPECT_EQ(listener.messageCount_, 1);
}

TEST_F(MessageAssemblerTest, receive_message_with_garbage_in_front_)
{
    addSymbols("badbad000863f9a1c23bd8639a38479c5ffb");
    EXPECT_EQ(listener.messageCount_, 1);
}

TEST_F(MessageAssemblerTest, receive_end_of_message_then_beginning_of_next)
{
    addSymbols("8639a38479c5ffb");
    addSymbols("863f9a1c23bd");
    EXPECT_EQ(listener.messageCount_, 1);
}

TEST_F(MessageAssemblerTest, message_start_is_alligned_with_symbol_start)
{
    std::string symbols = "863f9a1c23bd8639a38479c5ffb";
    for(size_t i = 0; i < symbols.size(); ++i)
    {
        std::stringstream ss;
        unsigned int symbol;
        ss << std::hex << symbols[i];
        ss >> symbol;
        int bitValue = 1;
        messageAssembler->addBit((symbol&1) != 0? bitValue : -bitValue);
        messageAssembler->addBit((symbol&2) != 0? bitValue : -bitValue);
        messageAssembler->symbolAdded();
        messageAssembler->addBit((symbol&4) != 0? bitValue : -bitValue);
    }
    EXPECT_EQ(listener.messageCount_, 0);
}

TEST_F(MessageAssemblerTest, receive_two_messages)
{
    addSymbols("863f9a1c23bd8639a38479c5ffb");
    EXPECT_EQ(listener.messageCount_, 1);
    listener.messageCount_ = 0;
    addSymbols("863f9a1c23bd8639a38479c5ffb");
    EXPECT_EQ(listener.messageCount_, 1);
}

TEST_F(MessageAssemblerTest, auto_correct_by_flipping_differing_bits)
{
    autoCorrectionLevel(2);
    //          ------|--------------------
    addSymbols("863f9a5c23bd8639a38479c5ffb");
    EXPECT_EQ(listener.messageCount_, 0);
    //          --|-----------------------
    addSymbols("862f9a1c23bd8639a38479c5ffb");
    EXPECT_EQ(listener.messageCount_, 1);
}

TEST_F(MessageAssemblerTest, auto_correct_by_flipping_weak_bits)
{
    autoCorrectionLevel(2);

    for(int i = 0; i < 2; i++)
    {
        addSymbols("863f9a1c");

        //Expected symbol 2
        messageAssembler->addBit(-bitValue);
        messageAssembler->addBit(bitValue);
        messageAssembler->addBit(bitValue * .5); // Wrong but weak. Should be corrected.
        messageAssembler->symbolAdded();

        addSymbols("3bd86");

        //Expected symbol 3
        messageAssembler->addBit(-bitValue * .5); // Wrong but weak. Should be corrected.
        messageAssembler->addBit(bitValue);
        messageAssembler->addBit(-bitValue);
        messageAssembler->symbolAdded();

        addSymbols("9a38479c5ffb");
    }
    EXPECT_EQ(listener.messageCount_, 1);
}
