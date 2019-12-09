package profile

import (
	"net"

	adapters "github.com/Dreamacro/clash/adapters/outbound"
	"github.com/Dreamacro/clash/component/auth"
	trie "github.com/Dreamacro/clash/component/domain-trie"
	"github.com/Dreamacro/clash/component/fakeip"
	"github.com/Dreamacro/clash/config"
	"github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/dns"
	"github.com/Dreamacro/clash/hub/executor"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"

	"github.com/kr328/cfa/tun"
)

// LoadDefault - load default configure
func LoadDefault() {
	defaultC := &config.Config{
		General: &config.General{
			Port:               0,
			SocksPort:          0,
			RedirPort:          0,
			Authentication:     []string{},
			AllowLan:           false,
			BindAddress:        "*",
			Mode:               tunnel.Direct,
			LogLevel:           log.SILENT,
			ExternalController: "",
			ExternalUI:         "",
			Secret:             "",
		},
		DNS: &config.DNS{
			Enable:     false,
			IPv6:       false,
			NameServer: []dns.NameServer{},
			Fallback:   []dns.NameServer{},
			FallbackFilter: config.FallbackFilter{
				GeoIP:  false,
				IPCIDR: []*net.IPNet{},
			},
			Listen:       "",
			EnhancedMode: dns.NORMAL,
			FakeIPRange:  nil,
		},
		Experimental: &config.Experimental{
			IgnoreResolveFail: false,
		},
		Hosts:   trie.New(),
		Rules:   []constant.Rule{},
		Users:   []auth.AuthUser{},
		Proxies: map[string]constant.Proxy{},
	}

	reject := adapters.NewProxy(adapters.NewReject())
	direct := adapters.NewProxy(adapters.NewDirect())
	global, _ := adapters.NewSelector("GLOBAL", []constant.Proxy{direct})

	defaultC.Proxies["DIRECT"] = direct
	defaultC.Proxies["REJECT"] = reject
	defaultC.Proxies["GLOBAL"] = adapters.NewProxy(global)

	tun.ResetDnsRedirect()

	executor.ApplyConfig(defaultC, true)
}

// LoadFromFile - load file
func LoadFromFile(path string) error {
	cfg, err := executor.ParseWithPath(path)
	if err != nil {
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
		pool, _ := fakeip.New(ipnet, 1000)

		var defaultDNSResolver = dns.New(dns.Config{
			Main: []dns.NameServer{
				dns.NameServer{Net: "tcp", Addr: "1.1.1.1:53"},
				dns.NameServer{Net: "tcp", Addr: "208.67.222.222:53"},
				dns.NameServer{Net: "", Addr: "119.29.29.29:53"},
				dns.NameServer{Net: "", Addr: "223.5.5.5:53"},
			},
			Fallback:     make([]dns.NameServer, 0),
			IPv6:         true,
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
