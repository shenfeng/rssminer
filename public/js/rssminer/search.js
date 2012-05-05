(function () {
  var RM = window.RM,
      data = RM.data,
      tmpls = RM.tmpls,
      util = RM.util,
      $q = $('#header input'),
      to_html = window.Mustache.to_html;

  var ID = 'search-result',
      SELECTED = 'selected';

  function do_search (e) {
    var q = $q.val(),
        $selected = $('#' + ID + ' .selected');
    switch(e.which) {
    case 38:                    // up
      if($selected.length) {
        $selected.removeClass(SELECTED);
        if($selected.prev().length) {
          $selected.prev().addClass(SELECTED);
        } else {
          $('#' + ID + ' li:last').addClass(SELECTED);
        }
      } else {
        $('#' + ID + ' li:last').addClass(SELECTED);
      }
      return false;
    case 40:                    // down
      if($selected.length) {
        $selected.removeClass(SELECTED);
        if($selected.next().length) {
          $selected.next().addClass(SELECTED);
        } else {
          $('#' + ID + ' li:first').addClass(SELECTED);
        }
      } else {
        $('#' + ID + ' li:first').addClass(SELECTED);
      }
      return false;
    case 13:                    // enter
      if($selected.length) {
        location.hash = $('a', $selected).attr('href');
        $('#' + ID).remove();
        $q.val('');
        return false;
      }
    case 27:                  // esc
      hide_search_result();
      return false;
    }

    data.get_search_result(q, 15, function (result) {
      var html = to_html(tmpls.search_result, {subs: result});
      hide_search_result();
      var $result = $(html).attr('id', ID);
      $('#header .wrapper').append($result).find('img').each(function (i, img) {
        img.onerror = function () { img.src="/imgs/16px-feed-icon.png"; };
      });
      $('li', $result).mouseenter(function (e) {
        $('li', $result).removeClass(SELECTED);
        $(this).addClass(SELECTED);
      });
    });
    return false;
  }

  function hide_search_result () {
    $('#' + ID).remove();
  }

  util.delegate_events($(document), {
    'keyup #header input': do_search,
    'click #search-result a': hide_search_result
  });
})();
