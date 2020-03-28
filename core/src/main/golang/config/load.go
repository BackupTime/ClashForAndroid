package config

import (
	"errors"
	"io/ioutil"

	"github.com/Dreamacro/clash/config"
	"github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/dns"
	"github.com/Dreamacro/clash/hub/executor"
	"github.com/Dreamacro/clash/log"
	"github.com/kr328/cfa/tun"
)

const tunAddress = "172.31.255.253/30"

const defaultConfig = `
log: debug
mode: Direct
Proxy:
- name: "broadcast"
  type: socks5
  server: 255.255.255.255
  port: 1080

Proxy Group:
- name: "select"
  type: select
  proxies: [DIRECT]

Rule:
- 'MATCH,DIRECT'
`

func init() {
	defaultNameServers := []string{
		"223.5.5.5",
		"119.29.29.29",
		"1.1.1.1",
		"208.67.222.222",
	}

	OptionalDnsPatch = &config.RawDNS{
		Enable:     true,
		IPv6:       true,
		NameServer: defaultNameServers,
		Fallback:   []string{},
		FallbackFilter: config.RawFallbackFilter{
			GeoIP:  false,
			IPCIDR: []string{},
		},
		Listen:            ":0",
		EnhancedMode:      dns.FAKEIP,
		FakeIPRange:       "198.18.0.0/16",
		FakeIPFilter:      []string{},
		DefaultNameserver: defaultNameServers,
	}
}

// LoadDefault - load default configure
func LoadDefault() {
	defaultC, err := parseConfig([]byte(defaultConfig), constant.Path.HomeDir())
	if err != nil {
		log.Warnln("Load Default Failure " + err.Error())
		return
	}

	DnsPatch = nil
	NameServersAppend = make([]string, 0)

	executor.ApplyConfig(defaultC, true)

	tun.InitialResolver()
}

// LoadFromFile - load file
func LoadFromFile(path, baseDir string) error {
	data, err := ioutil.ReadFile(path)
	if err != nil {
		return err
	}

	cfg, err := parseConfig(data, baseDir)
	if err != nil {
		return err
	}

	for _, ns := range cfg.DNS.NameServer {
		log.Infoln("DNS: %s", ns.Addr)
	}

	executor.ApplyConfig(cfg, true)

	tun.InitialResolver()

	log.Infoln("Profile " + path + " loaded")

	return nil
}

func parseConfig(data []byte, baseDir string) (*config.Config, error) {
	raw, err := config.UnmarshalRawConfig(data)
	if err != nil {
		return nil, err
	}

	patchRawConfig(raw)

	if len(raw.Proxy) == 0 && len(raw.ProxyProvider) == 0 &&
		len(raw.ProxyOld) == 0 && len(raw.ProxyProviderOld) == 0 {
		return nil, errors.New("Empty Profile")
	}

	cfg, err := config.ParseRawConfig(raw, baseDir)
	if err != nil {
		return nil, err
	}

	patchConfig(cfg)

	return cfg, nil
}
