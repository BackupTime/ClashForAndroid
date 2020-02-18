package bridge

import (
	"net"
	"syscall"

	"github.com/Dreamacro/clash/component/dialer"
	"github.com/kr328/cfa/tun"
)

type TunCallback interface {
	OnCreateSocket(fd int)
	OnStop()
}

var callback TunCallback

func init() {
	dialer.DialerHook = onNewDialer
	dialer.ListenConfigHook = onNewListenConfig
}

func onNewDialer(dialer *net.Dialer) {
	dialer.Control = onNewSocket
}

func onNewListenConfig(listen *net.ListenConfig) {
	listen.Control = onNewSocket
}

func onNewSocket(network, address string, c syscall.RawConn) error {
	if cb := callback; cb != nil {
		c.Control(func(fd uintptr) {
			cb.OnCreateSocket(int(fd))
		})
	}

	return nil
}

func StartTunDevice(fd, mtu int, dns string, cb TunCallback) error {
	callback = cb

	return tun.StartTunDevice(fd, mtu, dns)
}

func StopTunDevice() {
	tun.StopTunDevice()

	if c := callback; c != nil {
		c.OnStop()
	}

	callback = nil
}
