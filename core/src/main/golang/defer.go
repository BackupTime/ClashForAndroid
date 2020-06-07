package main

import (
	"github.com/Dreamacro/clash/adapters/outbound"
	"github.com/Dreamacro/clash/adapters/outboundgroup"
	"github.com/Dreamacro/clash/adapters/provider"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/cfa/config"
	"sync"
	"unsafe"
)

//#include "buffer.h"
import "C"

//export downloadProfileFromFd
func downloadProfileFromFd(fd int, base C.const_string_t, output C.const_string_t, onSuccess nativeCcall, onFailure nativeCcall) {
	b := C.GoString(base)
	o := C.GoString(output)

	go func() {
		err := config.DownloadFd(fd, o, b)
		if err != nil {
			callCcall(onFailure, unsafe.Pointer(C.CString(err.Error())))
		} else {
			callCcall(onSuccess, nil)
		}
	}()
}

//export downloadProfileFromUrl
func downloadProfileFromUrl(url C.const_string_t, base C.const_string_t, output C.const_string_t, onSuccess nativeCcall, onFailure nativeCcall) {
	u := C.GoString(url)
	b := C.GoString(base)
	o := C.GoString(output)

	go func() {
		err := config.DownloadUrl(u, o, b)
		if err != nil {
			callCcall(onFailure, unsafe.Pointer(C.CString(err.Error())))
		} else {
			callCcall(onSuccess, nil)
		}
	}()
}

//export loadProfile
func loadProfile(path C.const_string_t, base C.const_string_t, onSuccess nativeCcall, onFailure nativeCcall) {
	p := C.GoString(path)
	b := C.GoString(base)

	go func() {
		err := config.LoadFromFile(p, b)
		if err != nil {
			callCcall(onFailure, unsafe.Pointer(C.CString(err.Error())))
		} else {
			callCcall(onSuccess, nil)
		}
	}()
}

//export performHealthCheck
func performHealthCheck(group C.const_string_t, onSuccess nativeCcall, onFailure nativeCcall) {
	g := C.GoString(group)

	go func() {
		p := tunnel.Proxies()[g]
		if p == nil {
			callCcall(onFailure, unsafe.Pointer(C.CString("No such proxy group")))

			log.Warnln("Perform health check failure: %s not found", g)

			return
		}

		pw, ok := p.(*outbound.Proxy)
		if !ok {
			callCcall(onFailure, unsafe.Pointer(C.CString("Invalid group")))

			log.Warnln("Perform health check failure: %s not valid group", g)

			return
		}

		adapter, ok := pw.ProxyAdapter.(outboundgroup.ProxyGroup)
		if !ok {
			callCcall(onFailure, unsafe.Pointer(C.CString("Invalid group")))

			log.Warnln("Perform health check failure: %s not valid group", g)

			return
		}

		providers := adapter.GetProxyProviders()
		wg := &sync.WaitGroup{}

		wg.Add(len(providers))

		for _, p := range providers {
			go func(p provider.ProxyProvider) {
				p.HealthCheck()

				wg.Done()
			}(p)
		}

		wg.Wait()

		callCcall(onSuccess, nil)
	}()
}