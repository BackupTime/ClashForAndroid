package bridge

import "github.com/kr328/cfa/profile"

func LoadProfileFile(path, baseDir string) error {
	return profile.LoadFromFile(path, baseDir)
}

func DownloadProfileAndCheck(url, output string) error {
	return profile.DownloadAndCheck(url, output)
}

func SaveProfileAndCheck(data []byte, output string) error {
	return profile.SaveAndCheck(data, output)
}
