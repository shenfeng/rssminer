(function () {
  var RM = window.RM,
      data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      $q = $('#header input'),
      $header = $('#header .wrapper');

  var SELECTED = 'selected';

  var $lis,
      old_q,
      has_result = false,
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

  function do_search (e) {
    var q = $.trim($q.val());
    switch(e.which) {
    case 13:                    // enter
      var $selected = $('#search-result .selected');
      if($selected.length) {
        location.hash = $('a', $selected).attr('href');
        hide_search_result();
        $q.val('');
      }
      break;
    case 27:                    // esc
      hide_search_result();
      break;
    default:
      if(q !== old_q) {
        old_q = q;
        data.get_search_result(q, 15, function (result) {
          show_search_result(result);
        });
      }
    }
  }

  function show_search_result (data) {
    var html = tmpls.search_result(data);
    hide_search_result();
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

  function hide_search_result () {
    $('#search-result').remove();
  }

  function hide_search_result_on_esc (e) {
    if(e.which === 27) {        // ESC
      old_q = "";
      hide_search_result();
    }
  }

  util.delegate_events($(document), {
    'keyup #header input': do_search,
    'click #header input': function () { do_search({}); return false; },
    'click': function () { old_q = ""; hide_search_result(); },
    'keydown #header input': navigation,
    'keyup': hide_search_result_on_esc
  });

})();
