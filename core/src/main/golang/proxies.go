package main

import (
	"github.com/Dreamacro/clash/adapters/outbound"
	"github.com/Dreamacro/clash/adapters/outboundgroup"
	"github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
)

/*
#cgo CFLAGS: -Werror
#include "buffer.h"
#include "proxies.h"

#include <malloc.h>

static proxy_group_list_t *new_proxy_group_list_t(int size) {
	proxy_group_list_t *result = (proxy_group_list_t *) malloc(sizeof(proxy_group_list_t) + size * sizeof(proxy_group_t*));

	result->size = size;

	return result;
}

static proxy_group_t *new_proxy_group_t(int proxies_size) {
	proxy_group_t *result = (proxy_group_t *) malloc(sizeof(proxy_group_t) + proxies_size * sizeof(proxy_t));

	result->proxies_size = proxies_size;

	return result;
}

static proxy_t *proxy_group_get_proxy(proxy_group_t *group, int index) {
	return &group->proxies[index];
}

static void proxy_group_list_set(proxy_group_list_t *list, int index, proxy_group_t *group) {
	list->groups[index] = group;
}
 */
import "C"

//export setSelector
func setSelector(group C.const_string_t, selected C.const_string_t) C.int {
	g := C.GoString(group)
	s := C.GoString(selected)

	p := tunnel.Proxies()[g]
	if p == nil {
		log.Warnln("Set selector failure: %s not found", g)

		return -1
	}

	pw, ok := p.(*outbound.Proxy)
	if !ok {
		log.Warnln("Set selector failure: %s not valid group", g)

		return -1
	}

	adapter, ok := pw.ProxyAdapter.(outboundgroup.ProxyGroup)
	if !ok {
		log.Warnln("Set selector failure: %s not valid group", g)

		return -1
	}

	selector, ok := adapter.(*outboundgroup.Selector)
	if !ok {
		log.Warnln("Set selector failure: %s not selector", g)

		return -1
	}

	if err := selector.Set(s); err != nil {
		log.Warnln("Set selector failure: %s not in %s", s, g)

		return -1
	}

	log.Infoln("Set %s -> %s", g, s)

	return 0
}

//export queryProxyGroups
func queryProxyGroups() *C.proxy_group_list_t {
	proxies := tunnel.Proxies()
	groups := make([]outboundgroup.ProxyGroup, 0, len(proxies))

	for _, p := range proxies {
		pw, ok := p.(*outbound.Proxy)
		if !ok {
			continue
		}

		adapter, ok := pw.ProxyAdapter.(outboundgroup.ProxyGroup)
		if !ok {
			continue
		}

		groups = append(groups, adapter)
	}

	result := C.new_proxy_group_list_t(C.int(len(groups)))
	groupIndex := 0
	for _, group := range groups {
		ps := make([]constant.Proxy, 0)
		for _, provider := range group.GetProxyProviders() {
			ps = append(ps, provider.Proxies()...)
		}

		g := C.new_proxy_group_t(C.int(len(proxies)))

		g.base.name = C.CString(group.Name())
		g.base.proxy_type = typeToProxyTypeC(group.Type())
		g.base.delay = 0

		proxyIndex := 0
		for _, proxy := range proxies {
			p := C.proxy_group_get_proxy(g, C.int(proxyIndex))

			p.name = C.CString(proxy.Name())
			p.proxy_type = typeToProxyTypeC(proxy.Type())
			p.delay = C.long(proxy.LastDelay())

			if proxy.Name() == group.Now() {
				g.now = C.int(proxyIndex)
			}

			proxyIndex++
		}

		C.proxy_group_list_set(result, C.int(groupIndex), g)

		groupIndex++
	}

	return result
}

func typeToProxyTypeC(t constant.AdapterType) C.proxy_type_t {
	switch t {
	case constant.Direct:
		return C.Direct
	case constant.Reject:
		return C.Reject

	case constant.Shadowsocks:
		return C.Shadowsocks
	case constant.Snell:
		return C.Snell
	case constant.Socks5:
		return C.Socks5
	case constant.Http:
		return C.Http
	case constant.Vmess:
		return C.Vmess
	case constant.Trojan:
		return C.Trojan

	case constant.Relay:
		return C.Relay
	case constant.Selector:
		return C.Selector
	case constant.Fallback:
		return C.Fallback
	case constant.URLTest:
		return C.URLTest
	case constant.LoadBalance:
		return C.LoadBalance

	default:
		return C.Unknown
	}
}