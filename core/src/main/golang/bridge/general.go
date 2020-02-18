package bridge

import (
	"github.com/Dreamacro/clash/hub/executor"
	"github.com/Dreamacro/clash/tunnel"
)

type TunnelGeneral struct {
	Mode         string
	HTTPPort     int
	SocksPort    int
	RedirectPort int
}

func QueryGeneral() *TunnelGeneral {
	result := &TunnelGeneral{}

	g := executor.GetGeneral()
	m := tunnel.Mode()

	result.Mode = m.String()
	result.HTTPPort = g.Port
	result.SocksPort = g.SocksPort
	result.RedirectPort = g.RedirPort

	return result
}

func SetProxyMode(mode string) {
	switch mode {
	case "Direct":
		tunnel.SetMode(tunnel.Direct)
	case "Global":
		tunnel.SetMode(tunnel.Global)
	case "Rule":
		tunnel.SetMode(tunnel.Rule)
	}
}
