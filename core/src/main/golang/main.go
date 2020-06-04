package main

import (
	"github.com/Dreamacro/clash/component/mmdb"
	"github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/cfa/config"
)

//#include "buffer.h"
import "C"

func main() {}

//export initialize
func initialize(database *C.buffer_t, home, version C.const_string_t) {
	databaseData := C.GoBytes(database.buffer, database.length)
	homeData := C.GoString(home)
	versionData := C.GoString(version)

	mmdb.LoadFromBytes(databaseData)
	constant.SetHomeDir(homeData)
	config.ApplicationVersion = versionData
}

//export reset
func reset() {
	config.LoadDefault()
	tunnel.DefaultManager.ResetStatistic()
}