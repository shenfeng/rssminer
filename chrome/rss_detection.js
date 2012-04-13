(function () {

  function send_rss_links_to_backgroud () {
    if(window.top === window.self) {
      // console.log('----------------------------');
      var links = document.querySelectorAll('link'),
          result = [];
      for(var i = 0; i < links.length; i++) {
        var link = links[i];
        if(link.type === 'application/rss+xml' && link.href) {
          var data = { href: link.href, title: link.title };
          result.push(data);
        }
      }
      var d = {type: 'rss_links', data: result};
      chrome.extension.sendRequest(d);
    }
  }

  // if(window.top === window.self) {
  send_rss_links_to_backgroud();
  // }

  chrome.extension.onRequest.addListener(function () {
    send_rss_links_to_backgroud();
    // console.log('---------------------');
  });
})();
