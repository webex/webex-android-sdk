#ifndef AUDIOPAIRINGFILEGENERATOR_HPP_
#define AUDIOPAIRINGFILEGENERATOR_HPP_

#include <cstdint>
#include <string>

namespace AudioPairing {
    int generatePairingSequenceFile(uint32_t loopInterval, const std::string& playoutString, const std::string playoutFileName, const std::string pairingSoundFilesPath);
}

#endif /* AUDIOPAIRINGFILEGENERATOR_HPP_ */
