package main

/*
#include "ccall.h"

extern void initialize_ccall(int pool_size);
 */
import "C"
import (
	"runtime"
	"sync"
	"unsafe"
)

type nativeCcall C.ccall_t

type ccallContext struct {
	call   nativeCcall
	result chan struct{}
	argument unsafe.Pointer
}

var ccallQueue = make(chan *ccallContext)
var ccallMap sync.Map
var currentIndex uint64
var indexLock sync.Mutex

func init() {
	C.initialize_ccall(C.int(runtime.NumCPU()))
}

func callCcall(call nativeCcall, argument unsafe.Pointer) {
	context := &ccallContext{
		call:   call,
		result: make(chan struct{}),
		argument: argument,
	}

	ccallQueue <- context

	<- context.result
}

//export nextCcall
func nextCcall(ccall *nativeCcall, argument *unsafe.Pointer, index *uint64) {
	ctx := <- ccallQueue

	indexLock.Lock()
	currentIndex++
	*index = currentIndex
	*ccall = ctx.call
	*argument = ctx.argument
	indexLock.Unlock()

	ccallMap.Store(*index, ctx)
}

//export finishCcall
func finishCcall(index uint64) {
	ctx, ok := ccallMap.Load(index)
	if !ok {
		return
	}

	ctx.(*ccallContext).result <- struct{}{}
}