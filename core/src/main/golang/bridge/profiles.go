package bridge

import (
	"github.com/kr328/cfa/profile"
	"sync"
)

var mutex sync.Mutex

func LoadProfileFile(path, baseDir string, callback DoneCallback) {
	go func() {
		mutex.Lock()
		defer mutex.Unlock()

		err := profile.LoadFromFile(path, baseDir)
		if err != nil {
			callback.DoneWithError(err)
		} else {
			callback.Done()
		}
	}()
}

func DownloadProfileAndCheck(url, output, baseDir string, callback DoneCallback) {
	go func() {
		mutex.Lock()
		defer mutex.Unlock()

		err := profile.DownloadAndCheck(url, output, baseDir)
		if err != nil {
			callback.DoneWithError(err)
		} else {
			callback.Done()
		}
	}()
}

func SaveProfileAndCheck(data []byte, output, baseDir string, callback DoneCallback) {
	go func() {
		mutex.Lock()
		defer mutex.Unlock()

		err := profile.SaveAndCheck(data, output, baseDir)
		if err != nil {
			callback.DoneWithError(err)
		} else {
			callback.Done()
		}
	}()
}

func MoveProfileAndCheck(source, target, baseDir string, callback DoneCallback) {
	go func() {
		mutex.Lock()
		defer mutex.Unlock()

		err := profile.MoveAndCheck(source, target, baseDir)
		if err != nil {
			callback.DoneWithError(err)
		} else {
			callback.Done()
		}
	}()
}
