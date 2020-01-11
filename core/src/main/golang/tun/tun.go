package tun

import (
	"strconv"

	"github.com/Dreamacro/clash/dns"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/proxy/tun"
)

var tunInstance *tun.TunAdapter
var dnsAddress string

func StartTunDevice(fd, mtu int, dns string) error {
	if tunInstance != nil {
		return nil
	}

	t, err := tun.NewTunProxy("fd://" + strconv.Itoa(fd) + "?mtu=" + strconv.Itoa(mtu))
	if err != nil {
		return err
	}

	tunInstance = &t
	dnsAddress = dns + ":53"

	ResetDnsRedirect()

	log.Infoln("Android tun started")

	return nil
}

func StopTunDevice() {
	t := tunInstance
	if t == nil {
		return
	}

	(*t).Close()
}

func ResetDnsRedirect() {
	if tunInstance == nil {
		return
	}

	(*tunInstance).ReCreateDNSServer(dns.DefaultResolver, dnsAddress)
}
