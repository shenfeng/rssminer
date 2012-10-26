(function(){
  "use strict";
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

  function favicon_ok (idx, img) {
    img.onload = function () {
      var c = 'ficon-error';
      $(img).closest('.' + c).removeClass(c).addClass('ficon-ok');
    };
  }

  function tooltip (title, maxlength) {
    var count = 0,
        length = title.length;
    for(var i = 0; i < length; ++i) {
      if(title.charCodeAt(i) > 255) {
        count += 2;
      } else {
        count += 1;
      }
    }
    if(maxlength < count) { return title; }
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
      favicon_ok: favicon_ok,
      call_if_fn: call_if_fn,
      tooltip: tooltip,
      params: params,
      extract_data: extract_data,
      hostname: hostname
    }
  });
})();
