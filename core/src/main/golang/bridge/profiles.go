package bridge

import (
	"github.com/kr328/cfa/profile"
)

func LoadProfileFile(path, baseDir string, callback DoneCallback) {
	go func() {
		err := profile.LoadFromFile(path, baseDir)
		if err != nil {
			callback.DoneWithError(err)
		} else {
			callback.Done()
		}
	}()
}

func DownloadProfileAndCheck(url, output, baseDir string) error {
	err := profile.DownloadAndCheck(url, output, baseDir)
	if err != nil {
		return err
	}
	return nil
}

func ReadProfileAndCheck(fd int, output, baseDir string) error {
	return profile.ReadAndCheck(fd, output, baseDir)
}

func SaveProfileAndCheck(data []byte, output, baseDir string) error {
	return profile.SaveAndCheck(data, output, baseDir)
}

func MoveProfileAndCheck(source, target, baseDir string) error {
	return profile.MoveAndCheck(source, target, baseDir)
}
