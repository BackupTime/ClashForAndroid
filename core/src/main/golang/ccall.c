#include "ccall.h"

#include <pthread.h>
#include <stdlib.h>
#include <stdint.h>

extern void nextCcall(ccall_t *call, void **argument, uint64_t *index);
extern void finishCcall();

static void *run(void *arg) {
	(void) arg;

	while ( 1 ) {
		ccall_t call;
		void *argument;
		uint64_t index;

		nextCcall(&call, &argument, &index);

		call.function(call.context, argument);

		finishCcall(index);
	}

	return NULL;
}

void initialize_ccall(int pool_size) {
	for ( int i = 0 ; i < pool_size ; i++ ) {
	    pthread_t tid = 0;

        if ( pthread_create(&tid, NULL, &run, NULL) < 0 )
                abort();
	}
}
