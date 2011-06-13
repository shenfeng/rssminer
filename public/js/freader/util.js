$(function(){
  Handlebars.registerHelper('ymdate', function(context, block){
    var d = new Date(context),
        m = d.getMonth() + 1,
        day = d.getDay();
    return [d.getFullYear(),
            m < 10 ? '0' + m : m,
            day < 10 ? '0' + day : day].join('/');
  });

  var nofity = (function() {
    var $nofity = $('#notification'),
        $p = $('p',$nofity),
        message,
        count = 0;
    function msg(a, r, msg) {
      if(message !== msg){
        count = 1;
        message = msg;
        $p.html(msg);
        $nofity.removeClass(r).addClass(a)
          .css({
            marginLeft: -$p.width()/2,
            visibility: 'visible'
          });
      }else {
        count++;
      }
    }

    function hide (msg) {
      if(msg === message){
        count--;
      }
      if( !msg || count === 0) {
        _.delay( function (){
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

  var ajax = (function() {
    var loading = 'Loading...';
    function handler(a) {
      return a.success(function () {
        nofity.hide(loading);
      });
    };
    function get(url, success){
      nofity.msg(loading);
      return handler($.ajax({
        url: url,
        success: success
      }));
    }
    function jpost(url, data){
      nofity.msg(loading);
      var ajax = $.ajax({
        url: url,
        type: 'POST',
        datatype: 'json',
        contentType: 'application/json',
        data: JSON.stringify(data)
      });
      return handle(ajax);
    }
    return {
      get: get,
      jpost: jpost
    };
  })();

  function snippet(html) {
    return html && html.replace(/<[^<>]+>/g, '')
      .replace(/\s+/g, ' ')
      .replace(/&[^&;]+;/g,'')
      .slice(0,200);
  }

  // export
  window.Freader = $.extend(window.Freader, {
    util: {
      ajax: ajax,
      snippet: snippet
    }
  });
});
