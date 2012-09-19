(function () {
  // Cached regular expressions for matching named param parts and splatted
  // parts of route strings.
  var namedParam    = /:([\w\d]+)/g;
  var splatParam    = /\*([\w\d]+)/g;
  var escapeRegExp  = /[-[\]{}()+?.,\\^$|#\s]/g;

  var oldHash,
      location = window.location,
      isStarted = false,
      handles = [];

  function getFragment () {
    var hash = location.hash;
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
    if(oldHash === current) { return; }
    oldHash = current;
    loadUrl(current);
  }

  function loadUrl (hash) {
    for(var i = 0; i < handles.length; i++) {
      var h = handles[i],
          regex = h.regex;
      if(regex.test(hash)) {
        var args = regex.exec(hash).slice(1);
        for(var j = 0; j < args.length; j++) {
          if(/^\d+$/.test(args[j])) {
            args[j] = parseInt(args[j], 10); // convert to int if it's an int
          }
        }
        h.callback.apply(null, args);
        return true;
      }
    }
    return false;
  }

  function hashRouter (routes) {
    if(isStarted) return false;
    isStarted  = true;

    for (var r in routes) {
      addHandler(r, routes[r]);
    }

    oldHash = getFragment();
    window.onhashchange = checkUrl;
    return loadUrl(oldHash);
  }

  window.RM.Router = {route: hashRouter, navigate: function (hash) {
    if(hash) {
      hash = hash.replace(/^#*/, '');
      oldHash = hash;
      location.hash = hash;
    }
  }};
})();
