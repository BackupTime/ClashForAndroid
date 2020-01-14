package bridge

type DoneCallback interface {
	Done()
	DoneWithError(error)
}
