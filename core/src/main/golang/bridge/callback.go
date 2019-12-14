package bridge

type Callback interface {
}

var callback Callback = defaultCallback{}

func SetCallback(cb Callback) {
	callback = cb
}

type defaultCallback struct{}
