package profile

import (
	"github.com/Dreamacro/clash/component/fakeip"
	"github.com/Dreamacro/clash/config"
)

var (
	OptionalDnsPatch  *config.RawDNS
	DnsPatch          *config.RawDNS
	NameServersAppend []string

	cachedPool *fakeip.Pool
)

func patchRawConfig(rawConfig *config.RawConfig) {
	if d := DnsPatch; d != nil {
		rawConfig.DNS = *d
	} else if d := OptionalDnsPatch; d != nil {
		if !rawConfig.DNS.Enable {
			rawConfig.DNS = *d
		}
	}

	if append := NameServersAppend; len(append) > 0 {
		d := &rawConfig.DNS
		nameservers := make([]string, len(append)+len(d.NameServer))
		copy(nameservers, append)
		copy(nameservers[len(append):], d.NameServer)

		d.NameServer = nameservers
	}
}

func patchConfig(config *config.Config) {
	if config.DNS.FakeIPRange != nil {
		if c := cachedPool; c != nil {
			if config.DNS.FakeIPRange.Gateway().String() == c.Gateway().String() {
				config.DNS.FakeIPRange = c
			}
		} else {
			cachedPool = config.DNS.FakeIPRange
		}
	}
}
