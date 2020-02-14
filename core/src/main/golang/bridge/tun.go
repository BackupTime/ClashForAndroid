package bridge

import (
	"github.com/kr328/cfa/tun"
)

type TunCallback interface {
	OnStop()
}

var callback TunCallback

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
