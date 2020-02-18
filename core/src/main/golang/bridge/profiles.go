package bridge

import (
	"strings"

	"github.com/kr328/cfa/profile"
)

func ResetDnsAppend(dns string) {
	if len(dns) == 0 {
		profile.NameServersAppend = make([]string, 0)
	} else {
		profile.NameServersAppend = strings.Split(dns, ",")
	}
}

func SetDnsOverrideEnabled(enabled bool) {
	if enabled {
		profile.DnsPatch = profile.OptionalDnsPatch
	} else {
		profile.DnsPatch = nil
	}
}

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

func call(err error, callback DoneCallback) {
	if err != nil {
		callback.DoneWithError(err)
	} else {
		callback.Done()
	}
}
