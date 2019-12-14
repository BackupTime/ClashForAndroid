package bridge

import "github.com/kr328/cfa/profile"

func LoadProfileFile(path string) {
	profile.LoadFromFile(path)
}

func LoadProfileDefault() {
	profile.LoadDefault()
}
