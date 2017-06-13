/*
  Small utility to create symbol files ap_0.raw .. ap_f.raw
  48000 16 bit raw audio files.
  
  Build:
    OS X: clang symbol_file_creator.cpp -o symbol_file_creator -lstdc++ -std=c++11
  Run:
    ./symbol_file_creator
 */

#include <iostream>
#include <math.h>
#include <fstream>
#include <sstream>

#define SAMPLERATE 48000
#define BAUD 100
#define SAMPLES_PER_BIT (SAMPLERATE/BAUD)
#define SAMPLES_PER_WINDOW (2*SAMPLES_PER_BIT)

class Frequency {
public:
    Frequency(float f1, float amplify) : Frequency(f1, f1, amplify) {
        
    }
    Frequency(float f1, float f2, float amplify) {
        float window [SAMPLES_PER_WINDOW];
        window[SAMPLES_PER_BIT] = 1.0f;
        for(int i = 0; i < SAMPLES_PER_BIT; i++) {
            window[i] = window[SAMPLES_PER_WINDOW -i -1] = ((float)i)/SAMPLES_PER_BIT; // Triangular window
        }
        
        for(int i = 0; i < SAMPLES_PER_WINDOW; i++) {
            float frequency = f1 + ((f2-f1)*i)/SAMPLES_PER_WINDOW;
            float x = cosf(M_PI * 2.0f * i * frequency / SAMPLERATE);
            samples[i] = x * amplify * window[i];
        }
    }
    float samples [SAMPLES_PER_WINDOW];
};

void createSymbol(int symbol, float * samples);
void floatToShort(float * f, short * s, int l);

int main (int argc, char* argv[]) {
    std::cout << "Creating audio files: ";

    // Triangle fade.
    // Band count? 3?
    // Band frequency width?
    // Band frequency distance?
    // phase shift? 2 phases? combine with on-off?
    
    int l = SAMPLES_PER_BIT;
    
    float samples [l * 3];
    short pcm16samples [l*3];
    
    for(int s = 0; s < 16; s++) {
        for(int i = 0; i < l*3; i++)
            samples[i] = 0.0f;
        
        createSymbol(s, samples);
        
        floatToShort(samples, pcm16samples, l*3);
        
        std::stringstream strstr;
        strstr << "ap_" << std::hex << s << ".raw";
        std::ofstream f(strstr.str(), std::ios::out | std::ios::binary);
        f.write(reinterpret_cast<const char * >(pcm16samples), l * 3 * sizeof(short));
        f.close();
        
        std::cout << strstr.str() << (s < 15 ? ", " : ".");
    }
    std::cout << std::endl;
    
    return 0;
}

void createSymbol(int symbol, float * samples)
{
    const int samplerate = SAMPLERATE;
    const int baud = BAUD;
    
    int l = samplerate/baud;
    for(int i = 0; i < l*3; i++)
        samples[i] = 0.0f;

//    Audible: (by setting BAUD to 5)
//    Frequency f0(400, 450, .65);
//    Frequency f1(450, 500, .75);
//    Frequency f2(500, 550, .80);
    
    // Optimize for logging. 62.5 fra 20kHz
    // Frequency numbers: 2,5,8  Hz: 20125, 20312, 20500
//    Frequency f0(20125, .65);
//    Frequency f1(20312, .75);
//    Frequency f2(20500, .80);
    Frequency f0(20062.5, 20187.5, .65);
    Frequency f1(20250, 20375, .75);
    Frequency f2(20437.5, 20562.5, .80);
    // Log symbol 2, and compare sine with sweep.
    // Log symbol f, both sine and sweep
    
    // Optimize for listening. 44100 / 512 = 86.13.

    // 232   -> 19982,8125
    // 232.5 -> 20025,87891
    // 233   -> 20068,94531
    // 233.5 ->20112,01172
    // 234   ->20155,07813
    // 234.5 ->20198,14453
    // 235   ->20241,21094
    // 235.5 ->20284,27734
    // 236   ->20327,34375
    // 236.5 ->20370,41016
    // 237   ->20413,47656
    // 237.5 ->20456,54297
    // 238   ->20499,60938

//    // Frequencies in between:
//    Frequency f0(20112.01172, .65);
//    Frequency f1(20284.27734, .75);
//    Frequency f2(20456.54297, .80);
    
//    // Frequencies spot on:
//    Frequency f0(20155.07813, .65);
//    Frequency f1(20327.34375, .75);
//    Frequency f2(20499.60938, .80);
    

    if (symbol & 8) {
        for(int i = l; i < l * 2; i++) {
            samples[i-l] += f0.samples[i];
        }
    }
    if(symbol & 4) {
        for(int i = 0; i < 2 * samplerate/baud; i++) {
            samples[i] += f2.samples[i];
        }
    }
    if(symbol & 2) {
        for(int i = 0; i < l*2; i++) {
            samples[i + l] += f1.samples[i];
        }
    }
    if (symbol & 1) {
        for(int i = 0; i < l; i++) {
            samples[i + l*2] += f0.samples[i];
        }
    }
}

void floatToShort(float * f, short * s, int l)
{
    for (int i=0; i<l; ++i) {
        s[i] = f[i] * 32768;
    }
}
