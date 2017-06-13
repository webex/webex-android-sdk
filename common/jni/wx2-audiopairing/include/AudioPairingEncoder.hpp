#ifndef AUDIOPAIRINGENCODER_HPP_
#define AUDIOPAIRINGENCODER_HPP_

#include <cstdint>
#include <string>
#include <vector>

namespace netaddr {
class netaddr;
}

namespace AudioPairing {

std::string generatePlayoutString(const std::vector<uint8_t>& data);
std::string generatePlayoutString(const std::string token_string);

}

#endif /* AUDIOPAIRINGENCODER_HPP_ */