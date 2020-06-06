package main

/*
#include "ccall.h"

extern void initialize_ccall();
 */
import "C"
import "unsafe"

type nativeCcall C.ccall_t

type ccallContext struct {
	call   nativeCcall
	result chan struct{}
	argument unsafe.Pointer
}

var ccallQueue chan *ccallContext
var current *ccallContext

func init() {
	C.initialize_ccall()
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
func nextCcall(ccall *nativeCcall, argument *unsafe.Pointer) {
	ctx := <- ccallQueue

	current = ctx

	*ccall = ctx.call
	*argument = ctx.argument
}

//export finishCcall
func finishCcall() {
	ctx := current

	current = nil

	if ctx != nil {
		ctx.result <- struct{}{}
	}
}