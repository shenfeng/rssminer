(function (RM) {
  var ajax = RM.ajax,
      data = RM.data,
      get_subscription = data.get_subscription;

  var MAX_SORT_ORDER = 65535,
      INIT_SORT_ORDER = 256;

  function update_sort_order (moved_id, new_before, new_cat) {
    var subs = data.get_subscriptions();
    var step = Math.min(Math.floor(MAX_SORT_ORDER / subs.length), 256);
    var moved =  get_subscription(moved_id);
    var old_idx = _.indexOf(subs, moved);
    var before = get_subscription(new_before);
    var before_idx = _.indexOf(subs, before);
    var update_cat = moved.group_name !== new_cat;

    if(update_cat) { moved.group_name = new_cat; }

    var save_data = [],
        generate_all = false,
        self = update_cat ? {g: new_cat, id: moved.id}: {id: moved.id};

    if(before_idx === -1) { // no prev element
      if(subs[0].sort_index >= 2) {
        self.o = Math.floor(subs[0].sort_index / 2);
        save_data.push(self);
      } else {
        generate_all = true;
      }
    } else if (before_idx === subs.length - 2 ) { // the last one
      if(before.sort_index + step < MAX_SORT_ORDER) {
        self.o = before.sort_index + step;
        save_data.push(self);
      } else {
        generate_all = true;
      }
    } else {
      var gap = subs[before_idx + 1].sort_index - before.sort_index;
      if( gap > 2 ) {
        self.o = before.sort_index + Math.floor(gap / 2);
        save_data.push(self);
      } else {
        generate_all = true;
      }
    }

    if (generate_all){                    // regenerate all
      var sort_index = INIT_SORT_ORDER;
      for(var i = 0; i < subs.length; i++) {
        if(old_idx !== i) {
          save_data.push({id: subs[i].id, o: sort_index});
          subs[i].sort_index = sort_index;
          sort_index += step;
          if(before_idx === i) {
            self.o = sort_index;
            save_data.push(self);
            subs[old_idx].sort_index = sort_index;
            sort_index += step;
          }
        }
      }
    }

    if(self.o) { moved.sort_index = self.o; }    // update sort_index

    // subscriptions_cache = _.sortBy(subs, function (s) {
    //   return s.sort_index;
    // });
    ajax.spost('/api/subs/sort', save_data);
  }


  function update_subs_sort_order (event, ui) {
    if(ui.sender) { // prevent be callded twice if move bettween categories
      return;
    }
    var $moved = $(ui.item),
        $before = $moved.prev(),
        moved_id = parseInt($moved.attr('data-id')),
        new_cat = $moved.closest('.rss-category').siblings('.folder').attr('data-name'),
        new_before_id = $before.length ? parseInt($before.attr('data-id')) : null;
    update_sort_order(moved_id, new_before_id, new_cat);
  }

  RM.update_subs_sort_order = update_subs_sort_order; // export
  RM.update_category_sort_order = function () {  }; // export
})(window.RM);
