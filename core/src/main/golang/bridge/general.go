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
	t := tunnel.Instance()

	result.Mode = t.Mode().String()
	result.HTTPPort = g.Port
	result.SocksPort = g.SocksPort
	result.RedirectPort = g.RedirPort

	return result
}
