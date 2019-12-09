package tun

import (
	"fmt"
	"strconv"

	"github.com/Dreamacro/clash/dns"
	T "github.com/Dreamacro/clash/proxy/tun"
)

type handler struct {
	tunAdapter *T.TunAdapter
}

const dnsServerAddress = "172.19.0.2:53"

var (
	dnsHijacking bool = false
	instance     *handler
)

// StartTunProxy - start
func StartTunProxy(fd, mtu int) error {
	StopTunProxy()

	adapter, err := T.NewTunProxy("fd://" + strconv.Itoa(fd) + "?mtu=" + strconv.Itoa(mtu))
	if err != nil {
		return err
	}

	instance = &handler{
		tunAdapter: &adapter,
	}

	ResetDnsRedirect()

	fmt.Println("Android tun started")

	return nil
}

// StopTunProxy - stop
func StopTunProxy() {
	if instance != nil {
		(*instance.tunAdapter).Close()
		instance = nil
	}
}

func ResetDnsRedirect() {
	if instance == nil {
		return
	}

	if dnsHijacking {
		(*instance.tunAdapter).ReCreateDNSServer(dns.DefaultResolver, "0.0.0.0:53")
	} else {
		(*instance.tunAdapter).ReCreateDNSServer(dns.DefaultResolver, dnsServerAddress)
	}

}

func SetDnsHijacking(enabled bool) {
	dnsHijacking = enabled
}
