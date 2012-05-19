(function(){

  var enable_proxy = true;      // proxy reseted site?

  setTimeout(function () {
    var img = new Image(),
        src = ["blog", "spot",".com/favicon.ico?t="].join("");
    img.onload = function () { enable_proxy = false; };
    img.src = "http://sujitpal."+ src + new Date().getTime();
  }, 300);

  var hostname = (function () {
    var l = document.createElement("a");
    return function (uri) {
      if(uri) { l.href = uri; return l.hostname; }
      else { return ""; }
    };
  })();

  function extract_data ($ele) {
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

  var eventSplitter = /^(\S+)\s*(.*)$/;

  function delegate_events($ele, events) {
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

  function call_if_fn (f) {
    if(typeof f === 'function') {
      f.apply(null, _.toArray(arguments).slice(1));
    }
  }

  function favicon_error (idx, img) {
    img.onerror = function () { img.src="/imgs/16px-feed-icon.png"; };
  }

  function params (p) {
    var arr = [];
    for(var k in p) {
      arr.push(encodeURIComponent(k) + "=" + encodeURIComponent(p[k]));
    }
    return arr.join('&');
  }

  // export
  window.RM = $.extend(window.RM || {}, {
    util: {
      delegate_events: delegate_events,
      favicon_error: favicon_error,
      call_if_fn: call_if_fn,
      params: params,
      extract_data: extract_data,
      hostname: hostname,
      enableProxy: function () { return enable_proxy; }
    }
  });
})();
