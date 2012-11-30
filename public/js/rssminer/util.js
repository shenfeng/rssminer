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

  function to_html () {
    var args = _.toArray(arguments);
    if(args.length === 2) {
      args.push(window.RM.tmpls);
    }
    for (var k in _MESGS_) {
      args[1][k] = _LANG_ZH_ ? _MESGS_[k][1]: _MESGS_[k][0];
    }
    return Mustache.render.apply({}, args);
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

  function LRUCache (cachesize) {
    var entries = [];
    function get (ids) {
      if(_.isArray(ids)) {
        var m = {};
        _.each(ids, function (id) {
          var e = get(id);
          if(e) { m[id] = e[id]; }
        });
        return m;
      } else {
        var f = _.find(entries, function (e) { return e.key === ids; });
        if(f) {
          entries = _.filter(entries, function (e) { return e.key !== ids; });
          entries.unshift(f);   // move it to the first one
          var o = {};
          o[ids] = f.value;
          return o;
        }
      }
    }
    return {
      get: get,
      put: function (id, entry) {
        var o = get(id);
        if(o) {
          entries[0] = {key: id, value: entry}; // replace
        } else {
          entries.unshift({key: id, value: entry}); // put to front
          entries = entries.slice(0, cachesize);
        }
      }
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
      LRUCache: LRUCache,
      call_if_fn: call_if_fn,
      tooltip: tooltip,
      params: params,
      to_html: to_html,
      extract_data: extract_data,
      hostname: hostname
    }
  });
})();
