package bridge

import (
	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
)

type EventPoll struct {
	onStop func()
}

func (e *EventPoll) Stop() {
	e.onStop()
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

func PollLogs(logs Logs) *EventPoll {
	stopChannel := make(chan int, 1)
	sub := log.Subscribe()

	go func() {
		defer log.UnSubscribe(sub)
		defer close(stopChannel)
		defer log.Infoln("Logs Poll Stopped")

		for {
			select {
			case <-stopChannel:
				return
			case elm := <-sub:
				l := elm.(*log.Event)

				if l.LogLevel < log.Level() {
					break
				}

				logs.OnEvent(l.Type(), l.Payload)
			}
		}
	}()

	return &EventPoll{
		onStop: func() {
			stopChannel <- 0
		},
	}
}
