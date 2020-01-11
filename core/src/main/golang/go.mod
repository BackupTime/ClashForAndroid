module github.com/kr328/cfa

go 1.13

require (
	github.com/Dreamacro/clash v0.0.0 // local
	github.com/go-chi/render v1.0.1
	github.com/google/go-cmp v0.3.1 // indirect
	golang.org/x/mobile v0.0.0-20191210151939-1a1fef82734d // indirect
	golang.org/x/sys v0.0.0-20191224085550-c709ea063b76
)

replace github.com/Dreamacro/clash => ./clash

replace github.com/google/netstack => github.com/comzyh/netstack v0.0.0-20191217044024-67c27819ada4
