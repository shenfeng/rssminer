// notification depend on dom
window.$(function(){

  var handlebars = window.Handlebars,
      $ = window.$,
      _ = window._,
      JSON = window.JSON;

  handlebars.registerHelper('ymdate', function(context, block){
    var d = new Date(context),
        m = d.getMonth() + 1,
        day = d.getDay();
    return [d.getFullYear(),
            m < 10 ? '0' + m : m,
            day < 10 ? '0' + day : day].join('/');
  });

  var notif = (function(){
    var $nofity = $('#notification'),
        $p = $('p', $nofity),
        message,
        count = 0;

    function msg(a, r, msg){
      if(message !== msg){
        count = 1;
        message = msg;
        $p.html(msg);
        $nofity.removeClass(r).addClass(a)
          .css({
            marginLeft: -$p.width()/2,
            visibility: 'visible'
          });
      } else {
        count++;
      }
      // auto hide in 10s
      _.delay(_.bind(hide, this, msg), 10000);
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
      msg: _.bind(msg, null, 'message', 'error'),
      error: _.bind(msg, null, 'error', 'message'),
      hide: hide
    };
  })();

  var ajax = (function(){
    var loading = 'Loading...';
    function handler(ajax){
      return ajax.success(function (){
        notif.hide(loading);
      }).error(function (xhr, status, code){
        notif.error($.parseJSON(xhr.responseText).message);
      });
    };
    function get(url, success){
      notif.msg(loading);
      return handler($.ajax({
        url: url,
        success: success
      }));
    }
    function jpost(url, data){
      notif.msg(loading);
      var ajax = $.ajax({
        url: url,
        type: 'POST',
        datatype: 'json',
        contentType: 'application/json',
        data: JSON.stringify(data)
      });
      return handler(ajax);
    }
    return {
      get: get,
      jpost: jpost
    };
  })();

  function snippet(html){
    return html && html.replace(/<[^<>]+>/g, '')
      .replace(/\s+/g, ' ')
      .replace(/&[^&;]+;/g, '')
      .slice(0, 200);
  }

  // export
  window.Freader = $.extend(window.Freader, {
    ajax: ajax,
    util: {
      snippet: snippet,
      removeClass: function (c) { return $('.' + c).removeClass(c); }
    }
  });
});
