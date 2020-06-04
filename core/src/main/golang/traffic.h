#pragma once

#include <stdint.h>

typedef struct traffic_t {
    uint64_t upload;
    uint64_t download;
} traffic_t;