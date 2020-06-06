#pragma once

#include <stdint.h>

#if __cplusplus
extern "C" {
#endif

typedef void (*ccall_function_t)(void *context, void *argument);

typedef struct ccall_t {
    ccall_function_t function;
    void *context;
} ccall_t;

#if __cplusplus
};
#endif