(function () {
  var RM = window.RM,
      data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      $q = $('#header input'),
      $header = $('#header .wrapper');

  var SELECTED = 'selected',
      $ct_menu = $('#ct-menu'),
      WAIT_BEFORE_SEARCH = 100;

  var $lis,
      old_q = '',
      has_result = false,
      timer_id,
      current_idx = 0;

  function select_by_index () {
    $lis.removeClass(SELECTED);
    $($lis[current_idx]).addClass(SELECTED);
  }

  function navigation (e) {
    switch(e.which) {
    case 40:                    // down
      if(has_result) {
        current_idx += 1;
        if(current_idx === $lis.length) {
          current_idx = 0;
        }
        select_by_index();
        return false;
      }
      break;
    case 38:                    // up
      if(has_result) {
        current_idx -= 1;
        if(current_idx < 0) {
          current_idx = $lis.length - 1;
        }
        select_by_index();
        return false;
      }
      break;
    }
    return true;
  }

  function search_input_keyup (e) {
    var q = $.trim($q.val());
    switch(e.which) {
    case 13:                    // enter
      var $selected = $('#search-result .selected');
      if($selected.length) {
        hide_search_context_menu();
        $q.blur();
        location.hash = $('a', $selected).attr('href');
      }
      break;
    case 27:                    // esc
      hide_search_context_menu();
      break;
    case 40:                    // ignore direction. down
    case 38:                    // up
    case 39:                    // right
    case 37:                    // left
      break;
    default:
      if(q !== old_q) {
        old_q = q;
        if(timer_id) { window.clearTimeout(timer_id); }
        timer_id = window.setTimeout(function () {
          timer_id = 0;
          do_search(q);
        }, WAIT_BEFORE_SEARCH);
      }
    }
  }

  function show_search_result (data) {
    util.add_even(data.feeds);
    util.add_even(data.subs);
    var html = tmpls.search_result(data);
    hide_search_context_menu();
    $header.append(html).find('img').each(util.favicon_error);
    $lis = $('#search-result .subs > li, #search-result .feeds > li');
    $lis.mouseenter(function (e) {
      current_idx = _.indexOf($lis, this);
      select_by_index();
    });
    has_result = $lis.length;
    if(has_result) {
      current_idx = 0;
      select_by_index();
    }
  }

  function hide_search_context_menu (e) {
    if(!e) {                    // call by others
      $('#search-result').remove();
      $ct_menu.hide();
    } else if(e.which !== 3) {  // not right click
      $('#search-result').remove();
      $ct_menu.hide();
    }
  }

  function do_search (q) {
    data.get_search_result(q, 18, function (result) {
      // if no result, wait for result
      if(result.sub_cnt || (result.feeds && result.feeds.length)) {
        show_search_result(result);
      }
    });
  }

  function hide_search_result_on_esc (e) {
    if(e.which === 27) {        // ESC
      hide_search_context_menu();
      $q.blur();
      $ct_menu.hide();
    } else if(e.which === 191 && e.shiftKey) { // key / => shift + ?
      $q.focus().click();
    }
  }

  function search_on_click () {
    $q[0].select();
    do_search($.trim($q.val()));
    return false;
  }

  util.delegate_events($(document), {
    'click #header input': search_on_click,
    'click': hide_search_context_menu,
    'keydown #header input': navigation,
    'keyup #header input': search_input_keyup,
    'keyup': hide_search_result_on_esc
  });
})();
