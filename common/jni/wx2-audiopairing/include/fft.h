#ifndef FFT_H
#define FFT_H

#ifdef __cplusplus
extern "C" {
#endif

float fft_fftReal512Process(float * x, float * scratchpad);
float fft_fftReal512SparseProcess(float * x, float * scratchpad, const int kmin, const int kwidth);

#ifdef __cplusplus
}
#endif

#endif
