#include <pebble.h>
#include "main.h"
#include "message_handlers.h"

static void error_communicating_with_phone() {
  text_layer_set_text(text_layer, "No connection");
}

void out_sent_handler(DictionaryIterator *sent, void *context) {
  // noop
}


void out_failed_handler(DictionaryIterator *failed, AppMessageResult reason, void *context) {
  error_communicating_with_phone();
}


void in_received_handler(DictionaryIterator *received, void *context) {
  Tuple *response = dict_find(received, SET_STATE);
  if (response) {
    current_state = response->value->uint8;
    set_current_image();
  }
}

void in_dropped_handler(AppMessageResult reason, void *context) {
  error_communicating_with_phone();
}
