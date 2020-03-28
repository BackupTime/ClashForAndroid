module github.com/kr328/cfa

go 1.13

require (
	github.com/Dreamacro/clash v0.0.0 // local
	github.com/kr328/tun2socket v0.0.0-20200328151952-35732a824000 // indirect
)

replace github.com/Dreamacro/clash => ./clash

replace github.com/kr328/tun2socket v0.0.0-20200327164759-45012c372532 => /home/null/Workspace/GolangProjects/tun2socket
