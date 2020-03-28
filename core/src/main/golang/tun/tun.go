package tun

import (
	adapters "github.com/Dreamacro/clash/adapters/inbound"
	"github.com/Dreamacro/clash/component/socks5"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/tun2socket"
	"github.com/kr328/tun2socket/binding"
	"github.com/kr328/tun2socket/redirect"
	"net"
	"os"
	"sync"
	"syscall"

	"github.com/Dreamacro/clash/log"
)

var adapter *tun2socket.Tun2Socket
var mutex sync.Mutex

func StartTunDevice(fd, mtu int, gateway, mirror, dnsAddress string) error {
	mutex.Lock()
	defer mutex.Unlock()

	if adapter != nil {
		adapter.Close()
		adapter = nil
	}

	file := os.NewFile(uintptr(fd), "/dev/tun")
	_ = syscall.SetNonblock(fd, true)

	adapter = tun2socket.NewTun2Socket(file, mtu, net.ParseIP(gateway).To4(), net.ParseIP(mirror).To4())

	adapter.SetTCPHandler(func(conn net.Conn, endpoint *binding.Endpoint) {
		if hijackTCPDNS(conn, endpoint) {
			return
		}

		addr := socks5.ParseAddrToSocksAddr(&net.TCPAddr{
			IP:   endpoint.Target.IP,
			Port: int(endpoint.Target.Port),
			Zone: "",
		})

		tunnel.Add(adapters.NewSocket(addr, conn, C.SOCKS, C.TCP))
	})
	adapter.SetUDPHandler(func(payload []byte, endpoint *binding.Endpoint, sender redirect.UDPSender) {
		if hijackDNS(payload, endpoint, sender) {
			return
		}

		addr := socks5.ParseAddrToSocksAddr(&net.TCPAddr{
			IP:   endpoint.Target.IP,
			Port: int(endpoint.Target.Port),
			Zone: "",
		})
		pkt := &udpPacket{
			payload:  payload,
			endpoint: endpoint,
			sender:   sender,
		}

		tunnel.AddPacket(adapters.NewPacket(addr, pkt, C.SOCKS))
	})

	setHijackAddress(net.ParseIP(dnsAddress))
	InitialResolver()

	adapter.Start()

	log.Infoln("Android tun started")

	return nil
}

func StopTunDevice() {
	mutex.Lock()
	defer mutex.Unlock()

	if adapter != nil {
		adapter.Close()
		adapter = nil
	}

	log.Infoln("Android tun stopped")
}