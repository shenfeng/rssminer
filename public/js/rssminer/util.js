(function(){
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

  function add_even (arr) {
    if(_.isArray(arr)) {
      for(var i = 0; i < arr.length; i++) {
        if((i + 1) % 2 === 0) {
          var item = arr[i];
          if(item.cls) {
            item.cls += ' even';
          } else {
            item.cls = 'even';
          }
        }
      }
    }
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
      params: params,
      add_even: add_even,
      extract_data: extract_data,
      hostname: hostname
    }
  });
})();
