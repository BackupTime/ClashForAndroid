package bridge

import (
	"github.com/Dreamacro/clash/component/mmdb"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/cfa/profile"
)

func LoadMMDB(data []byte) {
	dataClone := make([]byte, len(data))
	copy(dataClone, data)

	mmdb.LoadFromBytes(dataClone)
}

func SetHome(homeDir string) {
	C.SetHomeDir(homeDir)
}

func Reset() {
	profile.LoadDefault()
	tunnel.DefaultManager.ResetStatistic()
}

func SetApplicationVersion(version string) {
	profile.ApplicationVersion = version
}
