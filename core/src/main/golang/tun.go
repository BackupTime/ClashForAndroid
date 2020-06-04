package main

//#include "buffer.h"
import "C"
import (
	"encoding/binary"
	"github.com/Dreamacro/clash/component/dialer"
	"github.com/kr328/cfa/tun"
	"io"
	"net"
	"os"
	"sync"
	"syscall"
)

var lock sync.Mutex
var oldNewSocketPipe *os.File = nil

//export startTunDevice
func startTunDevice(fd, mtu int, gateway, mirror, dns C.const_string_t, newSocket int) *C.char {
	lock.Lock()
	defer lock.Unlock()

	g := C.GoString(gateway)
	m := C.GoString(mirror)
	d := C.GoString(dns)

	err := tun.StartTunDevice(fd, mtu, g, m, d)
	if err != nil {
		return C.CString(err.Error())
	}

	if pipe := oldNewSocketPipe ; pipe != nil {
		_ = pipe.Close()
	}

	_ = syscall.SetNonblock(newSocket, true)
	pipe := os.NewFile(uintptr(newSocket), "socket")
	oldNewSocketPipe = pipe

	dialer.DialerHook = func(dialer *net.Dialer) error {
		dialer.Control = func(network, address string, c syscall.RawConn) error {
			return c.Control(func(fd uintptr) {
				lock.Lock()
				defer lock.Unlock()

				if pipe := oldNewSocketPipe ; pipe != nil {
					var fdBytes [4]byte

					binary.BigEndian.PutUint32(fdBytes[:], uint32(fd))

					_, _ = pipe.Write(fdBytes[:])
					_, _ = io.ReadFull(pipe, fdBytes[:])
				}
			})
		}
		return nil
	}

	return nil
}

//export stopTunDevice
func stopTunDevice() {
	lock.Lock()
	defer lock.Unlock()

	tun.StopTunDevice()

	if pipe := oldNewSocketPipe ; pipe != nil {
		_ = pipe.Close()
	}

	oldNewSocketPipe = nil
}