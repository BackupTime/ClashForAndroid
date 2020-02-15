package main

import (
	"io/ioutil"
	"net"
	"os"

	"github.com/Dreamacro/clash/component/mmdb"
)

func main() {
	f, err := os.Open("./Country.mmdb")
	if err != nil {
		println(err)
		return
	}

	buf, err := ioutil.ReadAll(f)
	if err != nil {
		println(err)
		return
	}

	mmdb.LoadFromBytes(buf)

	c, err := mmdb.Instance().Country(net.ParseIP("114.114.114.114"))
	if err != nil {
		println(err)
		return
	}

	println(c.Country.IsoCode)
}
