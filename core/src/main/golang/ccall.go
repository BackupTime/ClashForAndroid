package main

/*
#include "ccall.h"

extern void initialize_ccall(int pool_size);
 */
import "C"
import (
	"runtime"
	"sync"
	"sync/atomic"
	"unsafe"
)

type nativeCcall C.ccall_t

type ccallContext struct {
	call   nativeCcall
	result chan struct{}
	argument unsafe.Pointer
}

var ccallQueue chan *ccallContext
var ccallMap sync.Map
var currentIndex uint64

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

	*index = atomic.AddUint64(&currentIndex, 1)
	*ccall = ctx.call
	*argument = ctx.argument

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