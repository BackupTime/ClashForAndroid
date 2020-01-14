package bridge

import (
	"sync"

	"github.com/Dreamacro/clash/adapters/outbound"
	"github.com/Dreamacro/clash/adapters/outboundgroup"
	"github.com/Dreamacro/clash/adapters/provider"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
)

type ProxyItem struct {
	Name  string
	Type  string
	Delay int
}

type ProxyGroupItem struct {
	Name    string
	Type    string
	Current string
	Delay   int

	providers []provider.ProxyProvider
}

type ProxyGroupCollection interface {
	Add(proxy *ProxyGroupItem) bool
}

type ProxyCollection interface {
	Add(proxy *ProxyItem) bool
}

func (p *ProxyGroupItem) QueryAllProxies(collection ProxyCollection) {
	for _, v := range p.providers {
		for _, p := range v.Proxies() {
			collection.Add(
				&ProxyItem{
					Name:  p.Name(),
					Type:  p.Type().String(),
					Delay: int(p.LastDelay()),
				},
			)
		}
	}
}

func StartUrlTest(group string, callback DoneCallback) {
	go func() {
		defer callback.Done()

		p := tunnel.Instance().Proxies()[group]

		pa, ok := p.(*outbound.Proxy)
		if !ok {
			return
		}

		var providers []provider.ProxyProvider

		switch group := pa.ProxyAdapter.(type) {
		case *outboundgroup.Fallback:
			providers = group.GetProviders()
		case *outboundgroup.URLTest:
			providers = group.GetProviders()
		case *outboundgroup.LoadBalance:
			providers = group.GetProviders()
		case *outboundgroup.Selector:
			providers = group.GetProviders()
		default:
			return
		}

		wg := &sync.WaitGroup{}
		wg.Add(len(providers))

		for _, v := range providers {
			go func(p provider.ProxyProvider) {
				p.HealthCheck()
				wg.Done()
			}(v)
		}

		wg.Wait()
	}()
}

func QueryAllProxyGroups(collection ProxyGroupCollection) {
	ps := tunnel.Instance().Proxies()

	for _, p := range ps {
		pa, ok := p.(*outbound.Proxy)
		if !ok {
			continue
		}

		switch group := pa.ProxyAdapter.(type) {
		case *outboundgroup.Fallback:
			collection.Add(
				&ProxyGroupItem{
					Name:      group.Name(),
					Type:      group.Type().String(),
					Current:   group.Now(),
					Delay:     int(p.LastDelay()),
					providers: group.GetProviders(),
				},
			)
		case *outboundgroup.URLTest:
			collection.Add(
				&ProxyGroupItem{
					Name:      group.Name(),
					Type:      group.Type().String(),
					Current:   group.Now(),
					Delay:     int(p.LastDelay()),
					providers: group.GetProviders(),
				},
			)
		case *outboundgroup.LoadBalance:
			collection.Add(
				&ProxyGroupItem{
					Name:      group.Name(),
					Type:      group.Type().String(),
					Current:   "",
					Delay:     int(p.LastDelay()),
					providers: group.GetProviders(),
				},
			)
		case *outboundgroup.Selector:
			collection.Add(
				&ProxyGroupItem{
					Name:      group.Name(),
					Type:      group.Type().String(),
					Current:   group.Now(),
					Delay:     int(p.LastDelay()),
					providers: group.GetProviders(),
				},
			)
		default:
			continue
		}
	}
}

func SetSelectedProxy(name, proxy string) bool {
	p := tunnel.Instance().Proxies()[name]
	if p == nil {
		return false
	}

	pb, ok := p.(*outbound.Proxy)
	if !ok {
		return false
	}

	selector, ok := pb.ProxyAdapter.(*outboundgroup.Selector)
	if !ok {
		return false
	}

	if err := selector.Set(proxy); err != nil {
		return false
	}

	log.Infoln("Set " + name + " -> " + proxy)

	return true
}
