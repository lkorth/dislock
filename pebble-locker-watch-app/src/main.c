#include <pebble.h>
#include "message_handlers.h"

static Window *window;
static TextLayer *text_layer;
static BitmapLayer *image_layer;

static GBitmap *auto_image;
static GBitmap *unlocked_image;
static GBitmap *locked_image;

int current_state = 0;

void set_current_image() {
  switch(current_state) {
    case 0:
      bitmap_layer_set_bitmap(image_layer, auto_image);
      text_layer_set_text(text_layer, "Auto");
      break;
    case 1:
      bitmap_layer_set_bitmap(image_layer, unlocked_image);
      text_layer_set_text(text_layer, "Unlocked");
      break;
    case 2:
      bitmap_layer_set_bitmap(image_layer, locked_image);
      text_layer_set_text(text_layer, "Locked");
      break;
  }
}

void error_communicating_with_phone() {
  text_layer_set_text(text_layer, "No connection");
}

static void send_state_to_phone() {
  DictionaryIterator *iter;
  app_message_outbox_begin(&iter);
  Tuplet value = TupletInteger(SET_STATE, current_state);
  dict_write_tuplet(iter, &value);
  app_message_outbox_send();
}

static void up_click_handler(ClickRecognizerRef recognizer, void *context) {
  if(current_state - 1 < 0)
    current_state = 2;
  else
    current_state--;

  set_current_image();
  send_state_to_phone();
}

static void down_click_handler(ClickRecognizerRef recognizer, void *context) {
  if(current_state + 1 > 2)
    current_state = 0;
  else
    current_state++;

  set_current_image();
  send_state_to_phone();
}

static void click_config_provider(void *context) {
  window_single_click_subscribe(BUTTON_ID_UP, up_click_handler);
  window_single_click_subscribe(BUTTON_ID_DOWN, down_click_handler);
}

static void get_state_from_phone() {
  DictionaryIterator *iter;
  app_message_outbox_begin(&iter);
  Tuplet value = TupletInteger(GET_STATE, 1);
  dict_write_tuplet(iter, &value);
  app_message_outbox_send();
}

static void init(void) {
  window = window_create();
  window_set_background_color(window, GColorBlack);
  window_set_click_config_provider(window, click_config_provider);
  Layer *window_layer = window_get_root_layer(window);
  GRect bounds = layer_get_bounds(window_layer);

  auto_image = gbitmap_create_with_resource(RESOURCE_ID_AUTO_IMAGE);
  unlocked_image = gbitmap_create_with_resource(RESOURCE_ID_UNLOCKED_IMAGE);
  locked_image = gbitmap_create_with_resource(RESOURCE_ID_LOCKED_IMAGE);

  text_layer = text_layer_create((GRect) { .origin = { 0, 128 }, .size = { bounds.size.w, 40 } });
  text_layer_set_background_color(text_layer, GColorBlack);
  text_layer_set_text_color(text_layer, GColorWhite);
  text_layer_set_text(text_layer, "Loading...");
  text_layer_set_text_alignment(text_layer, GTextAlignmentCenter);
  layer_add_child(window_layer, text_layer_get_layer(text_layer));

  image_layer = bitmap_layer_create((GRect) { .origin = { 0, 0 }, .size = { bounds.size.w, 128 } });
  bitmap_layer_set_alignment(image_layer, GAlignCenter);
  layer_add_child(window_layer, bitmap_layer_get_layer(image_layer));

  app_message_register_inbox_received(in_received_handler);
  app_message_register_inbox_dropped(in_dropped_handler);
  app_message_register_outbox_sent(out_sent_handler);
  app_message_register_outbox_failed(out_failed_handler);
  app_message_open(64, 64);

  get_state_from_phone();

  window_stack_push(window, true);
}

static void deinit(void) {
  gbitmap_destroy(auto_image);
  gbitmap_destroy(unlocked_image);
  gbitmap_destroy(locked_image);
  bitmap_layer_destroy(image_layer);

  window_destroy(window);
}

int main(void) {
  init();
  app_event_loop();
  deinit();
}
