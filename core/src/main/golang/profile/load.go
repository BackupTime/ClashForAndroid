package profile

import (
	"io/ioutil"
	"net"

	"github.com/Dreamacro/clash/component/fakeip"
	"github.com/Dreamacro/clash/config"
	"github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/dns"
	"github.com/Dreamacro/clash/hub/executor"
	"github.com/kr328/cfa/tun"
)

const defaultConfig = `
mode: Direct
Proxy:
- name: "ss1"
  type: ss
  server: server
  port: 443
  cipher: chacha20-ietf-poly1305
  password: "password"
  # udp: true

Proxy Group:
- name: "select"
  type: select
  proxies: [DIRECT]

Rule:
- 'MATCH,DIRECT'
`

// LoadDefault - load default configure
func LoadDefault() {
	defaultC, _ := config.Parse([]byte(defaultConfig))

	tun.ResetDnsRedirect()

	executor.ApplyConfig(defaultC, true)
}

// LoadFromFile - load file
func LoadFromFile(path, baseDir string) error {
	data, err := ioutil.ReadFile(path)
	if err != nil {
		return err
	}

	rawCfg, err := config.UnmarshalRawConfig(data)
	if err != nil {
		return err
	}

	rawCfg.ExternalController = ""
	rawCfg.ExternalUI = ""

	fallbackBaseDir := constant.Path.HomeDir()
	constant.SetHomeDir(baseDir)

	cfg, err := config.ParseRawConfig(rawCfg)
	if err != nil {
		constant.SetHomeDir(fallbackBaseDir)
		return err
	}

	executor.ApplyConfig(cfg, true)

	if dns.DefaultResolver == nil && cfg.DNS.Enable {
		c := cfg.DNS

		r := dns.New(dns.Config{
			Main:         c.NameServer,
			Fallback:     c.Fallback,
			IPv6:         c.IPv6,
			EnhancedMode: c.EnhancedMode,
			Pool:         c.FakeIPRange,
			FallbackFilter: dns.FallbackFilter{
				GeoIP:  c.FallbackFilter.GeoIP,
				IPCIDR: c.FallbackFilter.IPCIDR,
			},
		})

		dns.DefaultResolver = r
	}

	if dns.DefaultResolver == nil {
		_, ipnet, _ := net.ParseCIDR("198.18.0.1/16")
		pool, _ := fakeip.New(ipnet, 1000, nil)

		var defaultDNSResolver = dns.New(dns.Config{
			Main: []dns.NameServer{
				dns.NameServer{Net: "tcp", Addr: "1.1.1.1:53"},
				dns.NameServer{Net: "tcp", Addr: "208.67.222.222:53"},
				dns.NameServer{Net: "", Addr: "119.29.29.29:53"},
				dns.NameServer{Net: "", Addr: "223.5.5.5:53"},
			},
			Fallback:     make([]dns.NameServer, 0),
			IPv6:         false,
			EnhancedMode: dns.FAKEIP,
			Pool:         pool,
			FallbackFilter: dns.FallbackFilter{
				GeoIP:  false,
				IPCIDR: make([]*net.IPNet, 0),
			},
		})

		dns.DefaultResolver = defaultDNSResolver
	}

	tun.ResetDnsRedirect()

	return nil
}
