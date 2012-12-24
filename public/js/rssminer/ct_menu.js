(function () {
  "use strict";
  var RM = window.RM,
      data_api = RM.data,
      tmpls = RM.tmpls,
      notify = RM.notify,
      util = RM.util,
      to_html = util.to_html;

  var DATA_NAME = 'data-name';

  var sorting_data;

  var $ct_menu = $('#ct-menu'),
      $last_menu_ui,
      $navigation = $('#navigation'),
      $subs_list = $('#sub-list'),
      $welcome_list = $('#welcome-list'),
      $win = $(window);

  function show_folder_context_menu (e) { // hide in search.js
    $last_menu_ui = $(this).closest('.folder');
    var html = to_html(tmpls.folder_ct_menu, {});
    $ct_menu.empty().append(html).css({
      left: e.clientX,
      top: e.clientY,
      display: 'block'
    });
    return false;
  }

  function dump_and_saving_sorting () {
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
    $(RM).trigger('sub-sorted.rm', [sorting_data]);
    RM.ajax.spost('/api/subs/sort', sorting_data);
  }

  function show_item_context_menu (e) { // hide in search.js
    $last_menu_ui = $(this).closest('.item');
    var subid = parseInt($last_menu_ui.attr('data-id')),
        html = to_html(tmpls.sub_ct_menu, {
          folders: data_api.list_folder_names(subid),
          sub: data_api.get_subscription(subid)
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

  function rename_folder_name () {
    $ct_menu.hide();
    var val = $last_menu_ui.attr(DATA_NAME),
        new_name = prompt('new folder name', val);
    if(new_name) {
      $last_menu_ui.find('span').text(new_name);
      $last_menu_ui.attr(DATA_NAME, new_name);
      // $last_menu_ui
      dump_and_saving_sorting();
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
    dump_and_saving_sorting();
  }

  function unsubscribe_item () {
    var subid = $last_menu_ui.attr('data-id'),
        sub = data_api.get_subscription(subid);
    $ct_menu.hide();
    if(confirm('unsubscribe "' + sub.title + '"')) {
      data_api.unsubscribe(subid, function () {
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
          sub = data_api.get_subscription(subid);
      var html = to_html(tmpls.subs_nav, {
        groups: [{subs: [sub], group: {
          name: new_folder,
          hash: data_api.sub_hash('f_' + new_folder, 1, 'newest')
        }}]
      });
      $last_menu_ui.remove();
      $subs_list.append(html).find('img').each(util.favicon_ok);
      $subs_list.trigger('refresh.rm');
    }
    dump_and_saving_sorting();
  }

  function toggle_sub_folder (e) {
    $(this).closest('li').toggleClass('collapse');
    var collapsed = [];
    $('#sub-list li.collapse .folder').each(function (index, item) {
      collapsed.push($(item).attr(DATA_NAME));
    });
    RM.ajax.spost('/api/settings', {nav: collapsed});
    return false;
  }

  function save_vote_up (e) {
    var $feed = $(this).closest('.feed');
    save_user_vote(1, $feed);
    return false;
  }

  function save_vote_down (e) {
    var $feed = $(this).closest('.feed');
    save_user_vote(-1, $feed);
    return false;
  }

  function save_user_vote (vote, $feed) {
    var id = $feed.attr('data-id');
    if(!$feed.hasClass('sys')) {
      if(($feed.hasClass('dislike') && vote === -1)
         || ($feed.hasClass('like') && vote === 1)) {
        vote = 0;                 // reset
      }
    }
    if(id) {
      id = parseInt(id, 10);
      data_api.save_vote(id, vote, function () {
        notify.show_msg('Saved', 1000);
        if(vote === 1) {
          $feed.addClass('like').removeClass('dislike neutral sys');
        } else if(vote === -1) {
          $feed.addClass('dislike').removeClass('like neutral sys');
        } else if(vote === 0) {
          $feed.addClass('neutral sys').removeClass('like dislike');
        }
      });
    }
  }

  util.delegate_events($ct_menu, {
    'click .rename': rename_folder_name,
    'click .folder': change_folder,
    'click .new-folder': move_to_new_folder,
    'click .unsubscribe': unsubscribe_item
  });

  util.delegate_events($subs_list, {
    'click .folder i': toggle_sub_folder,
    // 'contextmenu .folder': show_folder_context_menu, // hide in search.js
    // 'contextmenu .item': show_item_context_menu,
    'click .item .icon-caret-down': show_item_context_menu,
    'click .folder .icon-caret-down': show_folder_context_menu
  });

  util.delegate_events($('#main'), {
    'click .thumbs .icon-thumbs-down': save_vote_down,
    'click .thumbs .icon-thumbs-up': save_vote_up
  });

  $subs_list.sortable({
    update: dump_and_saving_sorting,
    handle: '.folder'
  });

  $subs_list.bind('refresh.rm', function () {
    // subscription sortable within categories
    $(".rss-category").sortable({
      connectWith: ".rss-category",
      update: dump_and_saving_sorting
    });
  });
})();
