package bridge

import (
	"time"

	"github.com/Dreamacro/clash/log"
	"github.com/Dreamacro/clash/tunnel"
)

type EventPoll struct {
	onStop func()
}

func (e *EventPoll) Stop() {
	e.onStop()
}

type Traffic interface {
	OnEvent(down int64, up int64)
}

type Bandwidth interface {
	OnEvent(bandwidth int64)
}

type Logs interface {
	OnEvent(level, payload string)
}

func PollTraffic(traffic Traffic) *EventPoll {
	stopChannel := make(chan int, 1)
	ticker := time.NewTicker(time.Second)

	tick := func() {
		up, down := tunnel.DefaultManager.Now()
		traffic.OnEvent(down, up)
	}

	tick()

	go func() {
		defer close(stopChannel)
		defer log.Infoln("Traffic Poll Stopped")

		for {
			select {
			case <-stopChannel:
				return
			case <-ticker.C:
				tick()
			}
		}
	}()

	return &EventPoll{
		onStop: func() {
			stopChannel <- 0
		},
	}
}

func PollBandwidth(bandwidth Bandwidth) *EventPoll {
	stopChannel := make(chan int, 1)
	ticker := time.NewTicker(time.Second)

	tick := func() {
		s := tunnel.DefaultManager.Snapshot()
		bandwidth.OnEvent(s.DownloadTotal + s.UploadTotal)
	}

	tick()

	go func() {
		defer close(stopChannel)
		defer log.Infoln("Bandwidth Poll Stopped")

		for {
			select {
			case <-stopChannel:
				return
			case <-ticker.C:
				tick()
			}
		}
	}()

	return &EventPoll{
		onStop: func() {
			stopChannel <- 0
		},
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
