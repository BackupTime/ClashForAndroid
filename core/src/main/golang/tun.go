package main

//#include "buffer.h"
import "C"
import (
	"github.com/Dreamacro/clash/component/dialer"
	"github.com/kr328/cfa/tun"
	"net"
	"sync"
	"syscall"
	"unsafe"
)

var lock sync.Mutex

//export startTunDevice
func startTunDevice(fd, mtu int, gateway, mirror, dns C.const_string_t, onNewSocket nativeCcall, onStop nativeCcall) *C.char {
	stopTunDevice()

	lock.Lock()
	defer lock.Unlock()

	g := C.GoString(gateway)
	m := C.GoString(mirror)
	d := C.GoString(dns)

	l := &sync.Mutex{}
	c := false
	closed := &c

	dialer.DialerHook = func(d *net.Dialer) error {
		d.Control = func(network, address string, c syscall.RawConn) error {
			return c.Control(func(fd uintptr) {
				l.Lock()
				defer l.Unlock()

				if *closed {
					return
				}

				//noinspection GoVetUnsafePointer
				callCcall(onNewSocket, unsafe.Pointer(fd))
			})
		}
		return nil
	}

	err := tun.StartTunDevice(fd, mtu, g, m, d, func() {
		l.Lock()
		defer l.Unlock()

		*closed = true

		callCcall(onStop, nil)
	})
	if err != nil {
		return C.CString(err.Error())
	}

	return nil
}

//export stopTunDevice
func stopTunDevice() {
	lock.Lock()
	defer lock.Unlock()

	tun.StopTunDevice()
}