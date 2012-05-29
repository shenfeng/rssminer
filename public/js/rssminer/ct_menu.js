(function () {
  var RM = window.RM,
      data = RM.data,
      tmpls = RM.tmpls,
      notify = RM.notify,
      util = RM.util;

  var SORTING_KEYS = '__sort__',
      DELAY_TIME = 1000 * 60;   // wait 1 min

  var save_timer_id,
      sorting_data;

  var $ct_menu = $('#ct-menu'),
      $last_menu_ui,
      $navigation = $('#navigation'),
      $subs_list = $('#sub-list'),
      $welcome_list = $('#welcome-list'),
      $win = $(window);

  function show_folder_context_menu (e) { // hide in search.js
    dump_saving_sorting();
    $last_menu_ui = $(this);
    var html = tmpls.folder_ct_menu({});
    $ct_menu.empty().append(html).css({
      left: e.clientX,
      top: e.clientY,
      display: 'block'
    });
    return false;
  }

  function save_sorting_data_to_save () {
    save_timer_id = undefined;
    if(localStorage) {
      localStorage.removeItem(SORTING_KEYS);
    }
    RM.ajax.spost('/api/subs/sort', sorting_data);
  }

  function dump_saving_sorting () {
    sorting_data = [];
    $('>li', $subs_list).each(function (idx, li) {
      var name = $.trim($('.folder span', li).text()),
          ids = [];
      $('.rss-category li', li).each(function (idx, item) {
        var id = parseInt($(item).attr('data-id'));
        ids.push(id);
      });
      if(!name) { name = 'null'; }
      sorting_data.push({g:name, ids: ids});
    });
    if(save_timer_id) { clearTimeout(save_timer_id); }
    if(localStorage) {
      localStorage.setItem(SORTING_KEYS, JSON.stringify(sorting_data));
    }
    $(RM).trigger('sub-sorted.rm', [sorting_data]);
    save_timer_id = setTimeout(save_sorting_data_to_save, DELAY_TIME);
  }

  function show_item_context_menu (e) { // hide in search.js
    $last_menu_ui = $(this);
    var subid = parseInt($last_menu_ui.attr('data-id')),
        html = tmpls.sub_ct_menu({
          folders: data.list_folder_names(subid),
          sub: data.get_subscription(subid)
        });
    var top = e.clientY;
    $ct_menu.empty().append(html);
    if($win.height() - e.clientY < $ct_menu.height()) {
      top -= $ct_menu.height();
    }
    $ct_menu.css({
      top: top,
      left: e.clientX,
      display: 'block'
    });
    return false;
  }

  function show_feed_context_menu (e) {
    $last_menu_ui = $(this);
    var feed = data.get_feed($last_menu_ui.attr('data-id')),
        html = tmpls.feed_ct_menu(feed);
    $ct_menu.empty().append(html).css({
      top: e.clientY,
      left: e.clientX,
      display: 'block'
    });
    return false;
  }

  function rename_folder_name () {
    $ct_menu.hide();
    var new_name = prompt('new name');
    if(new_name) {
      $last_menu_ui.find('span').text(new_name);
      dump_saving_sorting();
    }
  }

  function change_folder (e) {
    var new_folder = $.trim($(this).text());
    $('>li', $subs_list).each(function (idx, li) {
      var name = $.trim($('.folder span', li).text());
      if(name === new_folder) {
        $('.rss-category', li).prepend($last_menu_ui);
      }
    });
    dump_saving_sorting();
  }

  function unsubscribe_item () {
    var subid = $last_menu_ui.attr('data-id'),
        sub = data.get_subscription(subid);
    $ct_menu.hide();
    if(confirm('unsubscribe "' + sub.title + '"')) {
      data.unsubscribe(subid, function () {
        $last_menu_ui.remove();
        notify.show_msg('unsubscribed', 1000);
      });
    }
  }

  function move_to_new_folder () {
    $ct_menu.hide();
    var new_folder = prompt('new folder name');
    if(!new_folder) { return; }
    var find = false;
    $('>li', $subs_list).each(function (idx, li) {
      var name = $.trim($('.folder span', li).text());
      if(name === new_folder) {
        find = true;
        $('.rss-category', li).prepend($last_menu_ui);
      }
    });
    if(!find) {
      var subid = $last_menu_ui.attr('data-id'),
          sub = data.get_subscription(subid);
      var html = tmpls.subs_nav({
        groups: [{subs: [sub], group: new_folder}]
      });
      $last_menu_ui.remove();
      $subs_list.append(html).find('img').each(util.favicon_error);
      $subs_list.trigger('refresh.rm');
    }
    dump_saving_sorting();
  }

  function toggle_sub_folder (e) {
    $(this).closest('li').toggleClass('collapse');
    var collapsed = [];
    $('#sub-list li.collapse .folder').each(function (index, item) {
      collapsed.push($(item).attr('data-name'));
    });
    RM.ajax.spost('/api/settings', {nav: collapsed});
    return false;
  }

  function feed_clicked (e) {
    // Chrome works fine, firefox does not work
    // middle button, // left button with ctrl
    if((e.which === 1 && e.ctrlKey) || e.which === 2)   {
      var $a = $(this),
          feed = data.get_feed($a.parent().attr('data-id'));
      var old_link = $a.attr('href');
      $a.attr('href', feed.link);
      setTimeout(function () {
        $a.attr('href', old_link); // change it back
      }, 100);
    }
  }

  function vote_up () {
    RM.app.save_user_vote(1, $last_menu_ui);
  }

  function vote_down () {
    RM.app.save_user_vote(-1, $last_menu_ui);
  }

  $welcome_list.bind('child_change.rm', function () { // rebind
    $navigation.find('.feed > a').click(feed_clicked);
    // middle button click does not work well with delegate
    $welcome_list.find('.feed > a').click(feed_clicked);
  });

  util.delegate_events($ct_menu, {
    'click .rename': rename_folder_name,
    'click .folder': change_folder,
    'click .new-folder': move_to_new_folder,
    'click .unsubscribe': unsubscribe_item,
    'click .voteup': vote_up,
    'click .votedown': vote_down
  });

  util.delegate_events($subs_list, {
    'click .folder': toggle_sub_folder,
    'contextmenu .folder': show_folder_context_menu, // hide in search.js
    'contextmenu .item': show_item_context_menu
  });

  util.delegate_events($('#main'), {
    'contextmenu .feeds .feed': show_feed_context_menu
  });

  $subs_list.sortable({
    update: dump_saving_sorting,
    handle: '.folder'
  });

  $subs_list.bind('refresh.rm', function () {
    // subscription sortable within categories
    $(".rss-category").sortable({
      connectWith: ".rss-category",
      update: dump_saving_sorting
    });
  });
})();
