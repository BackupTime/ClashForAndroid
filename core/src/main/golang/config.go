package main

//#include "config.h"
import "C"

import (
	"github.com/kr328/cfa/config"
	"strings"
)

//export setDnsOverride
func setDnsOverride(override *C.dns_override_t) {
	dnsOverride := override.override_dns != 0
	appendNameservers := C.GoString(override.append_nameservers)

	if dnsOverride {
		config.DnsPatch = config.OptionalDnsPatch
	} else {
		config.DnsPatch = nil
	}

	if len(appendNameservers) == 0 {
		config.NameServersAppend = make([]string, 0)
	} else {
		config.NameServersAppend = strings.Split(appendNameservers, ",")
	}
}
