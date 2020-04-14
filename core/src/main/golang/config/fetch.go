package config

import (
	"context"
	"errors"
	"github.com/kr328/cfa/utils"
	"io/ioutil"
	"net"
	"net/http"
	"os"
	"syscall"

	"github.com/Dreamacro/clash/adapters/inbound"
	"github.com/Dreamacro/clash/component/socks5"
	"github.com/Dreamacro/clash/constant"
	"github.com/Dreamacro/clash/tunnel"
)

var ApplicationVersion = "Unknown"

const defaultFileMode = 0600

var client = &http.Client{
	Transport: &http.Transport{
		DialContext: func(ctx context.Context, network, address string) (net.Conn, error) {
			if network != "tcp" && network != "tcp4" && network != "tcp6" {
				return nil, errors.New("Unsupported network type " + network)
			}

			client, server := net.Pipe()

			tunnel.Add(inbound.NewSocket(socks5.ParseAddr(address), server, constant.HTTP, constant.TCP))

			return client, nil
		},
	},
}

func fetchRemote(url string) ([]byte, error) {
	request, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return nil, err
	}

	request.Header.Set("User-Agent", "ClashForAndroid/"+ApplicationVersion)

	response, err := client.Do(request)
	if err != nil {
		return nil, err
	}

	defer utils.CloseSilent(response.Body)

	data, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return nil, err
	}

	return data, nil
}

func fetchLocal(fd int) ([]byte, error) {
	_ = syscall.SetNonblock(fd, true)

	file := os.NewFile(uintptr(fd), "/dev/null")
	defer utils.CloseSilent(file)

	return ioutil.ReadAll(file)
}

func PullRemote(url, output, baseDir string) error {
	data, err := fetchRemote(url)
	if err != nil {
		return err
	}

	return save(data, output, baseDir)
}

func PullLocal(fd int, output, baseDir string) error {
	data, err := fetchLocal(fd)
	if err != nil {
		return err
	}

	return save(data, output, baseDir)
}

func save(data []byte, output, baseDir string) error {
	cfg, err := parseConfig(data, baseDir)
	if err != nil {
		return err
	}

	for _, v := range cfg.Providers {
		_ = v.Destroy()
	}

	return ioutil.WriteFile(output, data, defaultFileMode)
}
