package tun

import (
	"strconv"
	"sync"

	"github.com/Dreamacro/clash/component/resolver"
	"github.com/Dreamacro/clash/dns"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/proxy/tun"
)

var tunInstance *tun.TunAdapter
var dnsAddress string
var mutex sync.Mutex

func StartTunDevice(fd, mtu int, dns string) error {
	mutex.Lock()
	defer mutex.Unlock()

	if tunInstance != nil {
		return nil
	}

	t, err := tun.NewTunProxy("fd://" + strconv.Itoa(fd) + "?mtu=" + strconv.Itoa(mtu))
	if err != nil {
		return err
	}

	tunInstance = &t
	dnsAddress = dns

	ResetDnsRedirect()

	log.Infoln("Android tun started")

	return nil
}

func StopTunDevice() {
	mutex.Lock()
	defer mutex.Unlock()

	t := tunInstance
	if t == nil {
		return
	}

	(*t).Close()
	tunInstance = nil

	log.Infoln("Android tun stopped")
}

func ResetDnsRedirect() {
	if tunInstance == nil {
		return
	}

	(*tunInstance).ReCreateDNSServer(resolver.DefaultResolver.(*dns.Resolver), dnsAddress)
}
