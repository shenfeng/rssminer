(function(){
  var $nofity = $('<div id="notification"><p></p></div>')
        .prependTo($('body')),
      $p = $('p', $nofity),
      hide_timer_id;

  var LOADING = 'Loading...',
      MSG_CLASS = 'message',
      ERROR_CLASS = 'error';

  function show_msg(msg) {
    $p.html(msg).removeClass(ERROR_CLASS).addClass(MSG_CLASS);
    $nofity.css({ marginLeft: -$p.width() / 2, visibility: 'visible' });
  }

  function show_error_msg(msg) {
    $p.html(msg).removeClass(MSG_CLASS).addClass(ERROR_CLASS);
    $nofity.css({ marginLeft: -$p.width() / 2, visibility: 'visible' });
    // auto hide in 10s
    window.setTimeout(hide_notif, 10000);
  }

  function _clear_timer () {
    if(hide_timer_id) {
      clearTimeout(hide_timer_id);
      hide_timer_id = undefined;
    }
  }

  function hide_notif() {
    _clear_timer();
    hide_timer_id = setTimeout(function () {
      $nofity.css('visibility', 'hidden');
    }, 600);
  }

  function handler (url, method, success, silent) {
    return {
      type: method,
      url: url,
      success: function (result, status, xhr) {
        if(!silent) { hide_notif(LOADING); }
        if(typeof success === 'function') {
          success.apply(null, arguments);
        }
      },
      error: function (xhr) {
        try {
          show_error_msg(JSON.parse(xhr.responseText).message);
        }
        catch(e) {
          show_error_msg(xhr.responseText);
        }
      }
    };
  }

  function get(url, success, silent) {
    if(!silent) { show_msg(LOADING); }
    _clear_timer();
    return $.ajax(handler(url, 'GET', success, silent));
  }

  function sget (url, success) {
    return get(url, success, true);
  }

  function jpost(url, data, success, silent) {
    if(!silent) {
      show_msg(LOADING);
    }
    _clear_timer();
    if(typeof data === 'function') { // shift
      silent = success;
      success = data;
      data = undefined;
    }
    var o = handler(url, 'POST', success, silent);
    o.dateType = 'json';
    o.data = JSON.stringify(data);
    o.contentType = 'application/json';
    return $.ajax(o);
  }

  function sdelete (url, success) {
    return $.ajax(handler(url, 'DELETE', success, true));
  }

  function spost (url, data, success) {
    return jpost(url, data, success, true);
  }

  window.RM = $.extend(window.RM, {
    ajax: { sget: sget, spost: spost, get: get, jpost: jpost, del: sdelete }
  });
})();
