package bridge

import (
	"github.com/kr328/cfa/tun"
)

type TunCallback interface {
	OnNewSocket(fd int)
}

func StartTunDevice(fd, mtu int, gateway string, dns string, callback TunCallback) error {
	return tun.StartTunDevice(fd, mtu, gateway, dns, callback)
}

func StopTunDevice() {
	tun.StopTunDevice()
}
