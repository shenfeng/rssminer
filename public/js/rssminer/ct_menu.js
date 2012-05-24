(function () {
  var RM = window.RM,
      data = RM.data,
      tmpls = RM.tmpls,
      notify = RM.notify,
      util = RM.util;

  var $ct_menu = $('#ct-menu'),
      $subs_list = $('#sub-list'),
      $win = $(window),
      $navigation = $('#navigation'),
      $welcome_list = $('#welcome-list'),
      $logo = $('#logo'),
      $last_menu_ui;

  function show_folder_context_menu (e) { // hide in search.js
    $last_menu_ui = $(this);
    var o = $logo.offset(),
        html = tmpls.folder_ct_menu({});
    $ct_menu.empty().append(html);
    $ct_menu.css({
      left: e.clientX - o.left,
      top: e.clientY - o.top,
      display: 'block'
    });
    return false;
  }

  function show_item_context_menu (e) { // hide in search.js
    $last_menu_ui = $(this);
    var o = $logo.offset(),
        top = e.clientY - o.top,
        left = e.clientX - o.left,
        subid = parseInt($last_menu_ui.attr('data-id')),
        html = tmpls.item_ct_menu({folders: data.list_folder_names(subid)});
    $ct_menu.empty().append(html);
    if($win.height() - e.clientY < $ct_menu.height()) {
      top -= $ct_menu.height();
    }
    $ct_menu.css({
      left: left,
      top: top,
      display: 'block'
    });
    return false;
  }

  function rename_folder_name () {
    $ct_menu.hide();            //
    var new_name = prompt('new name');
    notify.show_msg('change not implemented, soon..', 1000);
  }

  function change_folder (e) {
    notify.show_msg('change not implemented, soon..', 1000);
  }

  function unsubscribe_item () {
    notify.show_msg('change not implemented, soon..', 1000);
  }

  function move_to_new_folder () {
    $ct_menu.hide();            //
    var new_name = prompt('new folder name');
    notify.show_msg('change not implemented, soon..', 1000);
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

  function show_feed_context_menu (e) {

    return false;
  }

  util.delegate_events($ct_menu, {
    'click .rename': rename_folder_name,
    'click .folder': change_folder,
    'click .new-folder': move_to_new_folder,
    'click .unsubscribe': unsubscribe_item
  });

  util.delegate_events($subs_list, {
    'click .folder': toggle_sub_folder,
    'contextmenu .folder': show_folder_context_menu, // hide in search.js
    'contextmenu .item': show_item_context_menu
  });

  $welcome_list.bind('child_change.rm', function () { // rebind
    $navigation.find('.feed > a').click(feed_clicked);
    // middle button click does not work well with delegate
    $welcome_list.find('.feed > a').click(feed_clicked);
  });

  util.delegate_events($welcome_list, {
    // 'contextmenu .feed': show_feed_context_menu
  });
})();
