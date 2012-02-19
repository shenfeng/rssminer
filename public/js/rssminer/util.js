(function(){

  var enable_proxy = true;      // proxy reseted site?

  setTimeout(function () {
    var img = new Image(),
        src = ["blog", "spot",".com/favicon.ico?t="].join("");
    img.onload = function () { enable_proxy = false; };
    img.src = "http://sujitpal."+ src + new Date().getTime();
  }, 300);

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
    return function (uri) {
      if(uri) { l.href = uri; return l.hostname; }
      else { return ""; }
    };
  })();

  function extractData ($ele) {
    var data = {};
    $("input, select", $ele).each(function (index, e) {
      var $input = $(e),
          name = $(e).attr('name');
      if(name) {
        data[name] = $input.val();
      }
    });
    return data;
  }

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

  function snippet(html, length){
    return html && html.replace(/<[^<>]+>/g, '')
      .replace(/\s+/g, ' ')
      .replace(/&[^&;]+;/g, '')
      .slice(0, length || 200);
  }

  var eventSplitter = /^(\S+)\s*(.*)$/;

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
  window.RM = $.extend(window.RM || {}, {
    iconError: imgError,
    util: {
      delegateEvents: delegateEvents,
      extractData: extractData,
      hostname: hostname,
      ymdate: ymdate,
      enableProxy: function () { return enable_proxy;  },
      snippet: snippet,
      // one dom ele is within another dom, or they are just the same
      within : function (child, parent) {
        return $.contains(parent, child) || parent === child;
      },
      removeClass: function (c) { return $('.' + c).removeClass(c); }
    }
  });
})();
