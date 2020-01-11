package bridge

import (
	"github.com/kr328/cfa/tun"
)

func StartTunDevice(fd, mtu int, dns string) error {
	return tun.StartTunDevice(fd, mtu, dns)
}

func StopTunDevice() {
	tun.StopTunDevice()
}
