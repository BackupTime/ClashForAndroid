package bridge

import (
	"context"
	"encoding/json"
	"time"

	"github.com/Dreamacro/clash/adapters/outbound"
	"github.com/Dreamacro/clash/adapters/outboundgroup"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
)

type Proxy struct {
	Name  string
	Type  string   `json:"type"`
	All   []string `json:"all"`
	Now   string   `json:"now"`
	Delay int
}

type ProxyList struct {
	proxies []*Proxy
}

type UrlTestCallback interface {
	OnResult(name string, delay int)
}

func (p Proxy) GetAllLength() int {
	return len(p.All)
}

func (p Proxy) GetAllElement(index int) string {
	return p.All[index]
}

func (pl *ProxyList) GetProxiesLength() int {
	return len(pl.proxies)
}

func (pl *ProxyList) GetProxiesElement(index int) *Proxy {
	return pl.proxies[index]
}

func QueryAllProxies() (*ProxyList, error) {
	ps := tunnel.Instance().Proxies()
	result := make([]*Proxy, len(ps))
	currentIndex := 0

	for k, v := range ps {
		current := &Proxy{
			Name:  k,
			Type:  v.Type().String(),
			All:   []string{},
			Delay: int(v.LastDelay()),
		}

		data, err := v.MarshalJSON()
		if err != nil {
			return nil, err
		}

		json.Unmarshal(data, current)

		result[currentIndex] = current

		currentIndex++
	}

	return &ProxyList{proxies: result}, nil
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

func StartUrlTest(name, url string, timeout int, callback UrlTestCallback) {
	go func() {
		p := tunnel.Instance().Proxies()[name]

		if p == nil {
			callback.OnResult(name, -1)
			return
		}

		ctx, cancel := context.WithTimeout(context.Background(), time.Millisecond*time.Duration(timeout))
		defer cancel()

		delay, err := p.URLTest(ctx, url)
		if ctx.Err() != nil || err != nil {
			callback.OnResult(name, -1)
			return
		}

		callback.OnResult(name, int(delay))
	}()
}
