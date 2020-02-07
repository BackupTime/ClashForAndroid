package bridge

import (
	"github.com/kr328/cfa/profile"
)

func LoadProfileFile(path, baseDir string, callback DoneCallback) {
	go func() {
		call(profile.LoadFromFile(path, baseDir), callback)
	}()
}

func DownloadProfileAndCheck(url, output, baseDir string, callback DoneCallback) {
	go func() {
		call(profile.DownloadAndCheck(url, output, baseDir), callback)
	}()
}

func ReadProfileAndCheck(fd int, output, baseDir string, callback DoneCallback) {
	go func() {
		call(profile.ReadAndCheck(fd, output, baseDir), callback)
	}()
}

func SaveProfileAndCheck(data []byte, output, baseDir string, callback DoneCallback) {
	go func() {
		call(profile.SaveAndCheck(data, output, baseDir), callback)
	}()
}

func MoveProfileAndCheck(source, target, baseDir string, callback DoneCallback) {
	go func() {
		call(profile.MoveAndCheck(source, target, baseDir), callback)
	}()
}

func call(err error, callback DoneCallback) {
	if err != nil {
		callback.DoneWithError(err)
	} else {
		callback.Done()
	}
}
