#include "ccall.h"

#include <pthread.h>
#include <stdlib.h>

extern void nextCcall(ccall_t *call, void **argument);
extern void finishCcall();

static void *run(void *arg) {
	(void) arg;

	while ( 1 ) {
		ccall_t call;
		void *argument;

		nextCcall(&call, &argument);

		call.function(call.context, argument);

		finishCcall();
	}

	return NULL;
}

void initialize_ccall() {
	pthread_t tid;

	if ( pthread_create(&tid, NULL, &run, NULL) < 0 )
		abort();
}
