package tun

import (
	"net"
	"strconv"
	"syscall"

	"github.com/Dreamacro/clash/dns"
	"github.com/Dreamacro/clash/global"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/proxy/tun"
)

type Callback interface {
	OnNewSocket(fd int)
}

var tunInstance *tun.TUN
var dnsGateway net.IP

func StartTunDevice(fd, mtu int, gateway string, dns string, callback Callback) error {
	if tunInstance != nil {
		return nil
	}

	ip, n, err := net.ParseCIDR(gateway)
	if err != nil {
		return err
	}

	n.IP = ip.To4()

	global.DefaultDialer.Control = func(network, address string, c syscall.RawConn) error {
		return c.Control(func(fd uintptr) {
			callback.OnNewSocket(int(fd))
		})
	}
	global.DefaultListenConfig.Control = func(network, address string, c syscall.RawConn) error {
		return c.Control(func(fd uintptr) {
			callback.OnNewSocket(int(fd))
		})
	}

	t, err := tun.NewTunProxy("fd://"+strconv.Itoa(fd)+"?mtu="+strconv.Itoa(mtu), *n)

	if err != nil {
		global.DefaultDialer.Control = nil
		global.DefaultListenConfig.Control = nil

		return err
	}
	tunInstance = t

	dnsGateway = net.ParseIP(dns)

	ResetDnsRedirect()

	log.Infoln("Android tun started")

	return nil
}

func StopTunDevice() {
	t := tunInstance
	if t == nil {
		return
	}

	t.Close()

	global.DefaultDialer.Control = nil
	global.DefaultListenConfig.Control = nil

	tunInstance = nil
}

func ResetDnsRedirect() {
	if tunInstance == nil {
		return
	}

	tunInstance.ReCreateDNSServer(dns.DefaultResolver, dnsGateway)
}
