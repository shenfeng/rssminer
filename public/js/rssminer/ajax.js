(function(){
  var loading = 'Loading...';
  var notif = (function() {
    var $nofity = $('<div id="notification"><p></p></div>')
          .prependTo($('body')),
        $p = $('p', $nofity),
        message,
        count = 0,
        MSG = 'message',
        ERROR = 'error';

    function msg(a, r, msg){
      if(message !== msg){
        count = 1;
        message = msg;
        $p.html(msg).removeClass(r).addClass(a);
        $nofity.css({
          marginLeft: -$p.width()/2,
          visibility: 'visible'
        });
      } else {
        count++;
      }
      // auto hide in 10s
      _.delay(_.bind(hide, null, msg), 100000);
    }

    function hide (msg){
      if(msg === message){
        count--;
      }
      if(!msg || count === 0){
        _.delay(function (){
          message = null;
          $nofity.css('visibility', 'hidden');
        }, 1000);
      }
    }
    return {
      msg: _.bind(msg, null, MSG, ERROR),
      error: _.bind(msg, null, ERROR, MSG),
      hide: hide
    };
  })();

  function handler (url, method, success) {
    return {
      type: method,
      url: url,
      success: function (result, status, xhr) {
        notif.hide(loading);
        if(typeof success === 'function') {
          var cy = xhr.getResponseHeader &&
                xhr.getResponseHeader("Content-Type");
          if(result && cy && cy.toLowerCase().indexOf('json') > 0) {
            result = JSON.parse(result);
          }
          success.apply(null, [result, status, xhr]);
        }
      },
      error: function (xhr) {
        notif.error(JSON.parse(xhr.responseText).message);
      }
    };
  }

  function get(url, success){
    notif.msg(loading);
    return $.ajax(handler(url, 'GET', success));
  }

  function jpost(url, data, success) {
    notif.msg(loading);
    if(typeof data === 'function') {
      success = data;
      data = undefined;
    }
    var o = handler(url, 'POST', success);
    o.dateType = 'json';
    o.data = JSON.stringify(data);
    o.contentType = 'application/json';
    return $.ajax(o);
  }

  window.RM = $.extend(window.RM, {
    notif: notif,
    ajax: {
      get: get,
      jpost: jpost
    }
  });
})();
