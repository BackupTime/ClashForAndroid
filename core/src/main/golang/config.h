#pragma once

typedef struct dns_override_t {
    int override_dns;
    const char *append_nameservers;
} dns_override_t;