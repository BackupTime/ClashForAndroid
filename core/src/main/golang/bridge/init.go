package bridge

import (
	"sync"

	"github.com/Dreamacro/clash/component/mmdb"
	C "github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
	"github.com/kr328/cfa/config"
)

var (
	logCallback  LogCallback
	logSubscribe sync.Once
)

type LogCallback interface {
	OnLogEvent(level, payload string)
}

func LoadMMDB(data []byte) {
	dataClone := make([]byte, len(data))
	copy(dataClone, data)

	mmdb.LoadFromBytes(dataClone)
}

func SetHome(homeDir string) {
	C.SetHomeDir(homeDir)
}

func Reset() {
	config.LoadDefault()
	tunnel.DefaultManager.ResetStatistic()
}

func SetApplicationVersion(version string) {
	config.ApplicationVersion = version
}

func SetLogCallback(callback LogCallback) {
	logSubscribe.Do(func() {
		go func() {
			sub := log.Subscribe()
			defer log.UnSubscribe(sub)

			for {
				elm := <-sub
				l := elm.(*log.Event)

				if l.LogLevel < log.Level() {
					continue
				}

				if cb := logCallback; cb != nil {
					cb.OnLogEvent(l.LogLevel.String(), l.Payload)
				}
			}
		}()
	})

	logCallback = callback
}
