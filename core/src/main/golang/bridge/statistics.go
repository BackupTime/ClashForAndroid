package bridge

import (
	"sync"

	"github.com/Dreamacro/clash/tunnel"
)

type EventPoll struct {
	stop sync.Once

	onStop func()
}

func (e *EventPoll) Stop() {
	e.stop.Do(func() {
		e.onStop()
	})
}

type Traffic struct {
	Download int64
	Upload   int64
}

type Logs interface {
	OnEvent(level, payload string)
}

func QueryBandwidth() *Traffic {
	upload := tunnel.DefaultManager.UploadTotal()
	download := tunnel.DefaultManager.DownloadTotal()

	return &Traffic{
		Upload:   upload,
		Download: download,
	}
}

func QueryTraffic() *Traffic {
	up, down := tunnel.DefaultManager.Now()

	return &Traffic{
		Upload:   up,
		Download: down,
	}
}
