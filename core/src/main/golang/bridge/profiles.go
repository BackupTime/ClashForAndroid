package bridge

import "github.com/kr328/cfa/profile"

func LoadProfileFile(path string) error {
	return profile.LoadFromFile(path)
}

func CheckProfileValid(profileData string) error {
	return profile.CheckValid(profileData)
}
