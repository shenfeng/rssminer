(function(){
  var mustache = window.Mustache,
      $ = window.$,
      _ = window._,
      eventSplitter = /^(\S+)\s*(.*)$/,
      JSON = window.JSON;

  function ymdate (date) {
    var d = new Date(date),
        m = d.getMonth() + 1,
        day = d.getDate();
    return [d.getFullYear(),
            m < 10 ? '0' + m : m,
            day < 10 ? '0' + day : day].join('/');
  }

  var hostname = (function () {
    var l = document.createElement("a");
    return function (uri) { l.href = uri; return 'http://' + l.hostname; };
  })();

  function interval (date) {
    var seconds = date - new Date().getTime() / 1000,
        data = {
          years: 31536000,
          month : 2592000,
          day: 86400,
          hour: 3600,
          minute: 60,
          second: 1
        };
    for(var attr in data) {
      var interval = Math.floor(seconds / data[attr]);
      if(interval > 1)
        return "in " + interval + " " +  attr + "s";
      else if (interval < -1) {
        return -interval + " " + attr + "s ago";
      }
    }
  };

  mustache.registerHelper('ymdate', ymdate);
  mustache.registerHelper('interval', interval);

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
      _.delay(_.bind(hide, this, msg), 100000);
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

  var ajax = (function(){
    var loading = 'Loading...';
    function handler(ajax){
      return ajax.success(function (){
        notif.hide(loading);
      }).error(function (xhr, status, code){
        notif.error($.parseJSON(xhr.responseText).message);
      });
    };
    function get(url){
      notif.msg(loading);
      return handler($.ajax({
        url: url
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

  function snippet(html, length){
    return html && html.replace(/<[^<>]+>/g, '')
      .replace(/\s+/g, ' ')
      .replace(/&[^&;]+;/g, '')
      .slice(0, length || 200);
  }

  function delegateEvents($ele, events) {
    for (var key in events) {
      var method = events[key],
          match = key.match(eventSplitter),
          eventName = match[1],
          selector = match[2];
      if (selector === '') {
        $ele.bind(eventName, method);
      } else {
        $ele.delegate(selector, eventName, method);
      }
    }
  }
  // export
  window.Rssminer = $.extend(window.Rssminer, {
    ajax: ajax,
    notif: notif,
    util: {
      delegateEvents: delegateEvents,
      hostname: hostname,
      ymdate: ymdate,
      snippet: snippet,
      // one dom ele is within another dom, or they are just the same
      within : function (child, parent) {
        return $.contains(parent, child) || parent === child;
      },
      removeClass: function (c) { return $('.' + c).removeClass(c); }
    }
  });
})();
