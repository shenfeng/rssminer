(function(){
  var eventSplitter = /^(\S+)\s*(.*)$/;

  function ymdate (date) {
    var d = new Date(date),
        m = d.getMonth() + 1,
        day = d.getDate();
    return [d.getFullYear(),
            m < 10 ? '0' + m : m,
            day < 10 ? '0' + day : day].join('/');
  }

  var hashRouter = (function () {
    // Cached regular expressions for matching named param parts and splatted
    // parts of route strings.
    var namedParam    = /:([\w\d]+)/g;
    var splatParam    = /\*([\w\d]+)/g;
    var escapeRegExp  = /[-[\]{}()+?.,\\^$|#\s]/g;

    var oldHash,
        isStarted = false,
        handles = [];

    function getFragment () {
      var hash = window.location.hash;
      return decodeURIComponent(hash.replace(/^#*/, ''));
    }

    function routeToRegExp (route) {
      route = route.replace(escapeRegExp, "\\$&")
        .replace(namedParam, "([^\/]*)")
        .replace(splatParam, "(.*?)");
      return new RegExp('^' + route + '$');
    }

    function addHandler (route, callback) {
      var regex = routeToRegExp(route);
      handles.push({regex: regex, callback: callback});
    }

    function checkUrl () {
      var current = getFragment();
      if(oldHash === current) {
        return;
      }
      oldHash = current;
      loadUrl(current);
    }

    function loadUrl (hash) {
      for(var i = 0; i < handles.length; i++) {
        var h = handles[i],
            regex = h.regex;
        if(regex.test(hash)) {
          var args = regex.exec(hash).slice(1);
          h.callback.apply(null, args);
          return true;
        }
      }
      return false;
    }

    return function (routes) {
      if(isStarted) return false;

      isStarted  = true;
      for (var r in routes) {
        addHandler(r, routes[r]);
      }

      oldHash = getFragment();
      window.onhashchange = checkUrl;
      return loadUrl(oldHash);
    };
  })();

  var hostname = (function () {
    var l = document.createElement("a");
    return function (uri) { l.href = uri; return l.hostname; };
  })();

  function interval (date) {
    var seconds = date - new Date().getTime() / 1000,
        data = {
          year: 31536000,
          month : 2592000,
          day: 86400,
          hour: 3600,
          minute: 60,
          second: 1
        };
    for(var attr in data) {
      var i = Math.floor(seconds / data[attr]);
      if(i > 1)
        return "in " + i + " " +  attr + "s";
      else if (i < -1) {
        return -i + " " + attr + "s ago";
      }
    }
  };

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

  var ajax = (function(){
    var loading = 'Loading...';

    function handler (url, method, success) {
      return {
        type: method,
        url: url,
        success: function () {
          notif.hide(loading);
          if(typeof success === 'function') {
            success.apply(null, arguments);
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

  function imgError (e) {
    e.src="/imgs/16px-feed-icon.png";
  }

  // export
  window.RM = $.extend(window.RM, {
    ajax: ajax,
    notif: notif,
    iconError: imgError,
    util: {
      delegateEvents: delegateEvents,
      hashRouter: hashRouter,
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
