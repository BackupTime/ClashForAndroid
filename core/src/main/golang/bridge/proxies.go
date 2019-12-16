package bridge

import (
	"encoding/json"

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
