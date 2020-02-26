package profile

import (
	"context"
	"errors"
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

			go func() {
				if ctx == nil || ctx.Done() == nil {
					return
				}

				<-ctx.Done()

				client.Close()
				server.Close()
			}()

			return client, nil
		},
	},
}

func DownloadAndCheck(url, output, baseDir string) error {
	request, err := http.NewRequest("GET", url, nil)
	if err != nil {
		return err
	}

	request.Header.Set("User-Agent", "ClashForAndroid/"+ApplicationVersion)

	response, err := client.Do(request)
	if err != nil {
		return err
	}

	defer response.Body.Close()
	data, err := ioutil.ReadAll(response.Body)
	if err != nil {
		return err
	}

	return SaveAndCheck(data, output, baseDir)
}

func ReadAndCheck(fd int, output, baseDir string) error {
	syscall.SetNonblock(fd, true)

	file := os.NewFile(uintptr(fd), "/dev/null")
	defer file.Close()

	data, err := ioutil.ReadAll(file)
	if err != nil {
		return err
	}

	return SaveAndCheck(data, output, baseDir)
}

func SaveAndCheck(data []byte, output, baseDir string) error {
	cfg, err := parseConfig(data, baseDir)
	if err != nil {
		return err
	}

	for _, v := range cfg.Providers {
		v.Destroy()
	}

	return ioutil.WriteFile(output, data, defaultFileMode)
}
