#pragma once

#include <stdint.h>

#if __cplusplus
extern "C" {
#endif

typedef struct traffic_t {
    uint64_t upload;
    uint64_t download;
} traffic_t;

#if __cplusplus
};
#endif