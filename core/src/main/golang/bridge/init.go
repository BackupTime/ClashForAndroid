package bridge

import (
	"github.com/Dreamacro/clash/component/mmdb"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/cfa/profile"
)

func LoadMMDB(data []byte) {
	mmdb.LoadFromBytes(data)
}

func Reset() {
	profile.LoadDefault()
	tunnel.DefaultManager.ResetStatistic()
}
