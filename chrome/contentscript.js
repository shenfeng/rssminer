(function () {

  function getHost (uri) {
    var l = document.createElement("a");
    if(uri) { l.href = uri; return l.hostname; }
    else { return ""; }
  }

  function extractUrlParams () {
    var query = location.query,
        result = {};
    if(query) {
      var parts = query.split(/[?|&]/);
      for(var i = 0; i < parts.length; i++) {
        var p = parts[i];
        if(p) {
          var keyValue = p.split('=');
          if(keyValue.length === 2) {
            var v = decodeURIComponent(keyValue[1].replace("+", " "));
            result[keyValue[0]] = v;
          }
        }
      }
    }
    return result;
  }

  function uniqeSort (list, key) {
    var obj = {}, result = [];
    _.each(list, function (item) { obj[item[key]] = item; }); // uniqe
    for(var r in obj) {
      result.push(obj[r]);
    }
    result.sort(function (a, b) {
      return a[key] > b[key];   // sort asc
    });
    return result;
  }

  function getCurrentPageHost () {
    var $base = $('base'),h;
    if($base.length && $base.attr('href')) {
      h = getHost($base.attr('href'));
    } else {
      h = location.hostname;
    }
    return h;
  }

  (function () {
    $("#navbar").each(function (index, item) {
      var $item = $(item);
      if($item.find('#Navbar1').length && $item.find('iframe').length) {
        $item.remove();
      }
    });

    var adds = ['avpa.dzone.com', 'doubleclick'];

    //  remove adds
    $("a").each(function (index, a) {
      var $a = $(a), host = getHost($a.attr('href')),
          isAdd = _.any(adds, function (a) {
            return host.indexOf(a) !== -1;
          });
      if(host && isAdd) { $a.remove(); }
    });

    var blocked_jss = ['twitter', 'industrybrains', 'doubleclick',
                       "googlesyndication",
                       'linkedin', 'facebook', "doubleverify"];
    $('script').each(function (index, js) {
      var $js = $(js),
          host = getHost($js.attr('src')),
          remove = _.any(blocked_jss, function (j) {
            return host.indexOf(j) !== -1;
          });
      if(host && remove) { $js.remove();}
    });
  })();

  (function () {
    var matchies =  {
      javaworld: {
        remove: ["#d-e_col2b", "#jw-sidecar", '#id_storytools_top',
                 "#toolbar", "#footer", "#id_storytools_bottom",
                 "#bannerarea"],
        manyPages: {
          links: '#pagenum a',
          before: '#pagenum',
          selector: '#body-content-area',
          remove: ['#pagenum', ' #article_footer', '>h1',
                   '#id_storytools_top', ".byline"]
        }
      },
      dzone: {
        remove: ['#header', '.related_library_content', '#woopra_bar',
                 "#sidebar_features", ".sidebarlinks", "#sharebar"]
      }
    };

    var host = getCurrentPageHost();

    for(var match in matchies) {
      var data = matchies[match];
      if(host.indexOf(match) !== -1) {
        _.each(data.remove, function (s) { $(s).remove(); });
        var manyPages = data.manyPages;
        if(manyPages) {
          var $pages = $(manyPages.links),
              fetches = [];
          var success = _.after($pages.length, function () {
            fetches = uniqeSort(fetches, 'href');
            _.each(fetches, function (f) {
              $(manyPages.before).before(f.content);
            });
            $(manyPages.links).remove();
          });
          $pages.each(function (index, a) {
            var $a = $(a), href = $a.attr('href');
            $.get(href, function (html) {
              var $content = $(manyPages.selector, $(html));
              _.each(manyPages.remove, function (s) {
                $(s, $content).remove();
              });
              fetches.push({ href: href, content: $content });
              success();
            });
          });
        }
      }
    }
  })();
})();
