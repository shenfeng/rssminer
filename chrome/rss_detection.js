(function () {

  var cached_rss_links;

  function send_rss_links_to_backgroud (request, sender, sendResponse) {
    if(window.top === window.self) {
      // console.log('----------------------------');
      var result = [];
      if(cached_rss_links) {
        result = cached_rss_links;
      } else {
        var links = document.querySelectorAll('link');
        for(var i = 0; i < links.length; i++) {
          var link = links[i];
          if(link.type === 'application/rss+xml' && link.href) {
            var data = { href: link.href, title: link.title };
            result.push(data);
          }
        }
        cached_rss_links = result;
      }

      var d = {type: 'rss_links', data: result};
      if(sendResponse) {
        sendResponse(d);
      } else if(result.length){
        chrome.extension.sendRequest(d);
      }
    }
  }

  // tell background page if this Document has rss, and show the icon
  send_rss_links_to_backgroud();

  chrome.extension.onRequest.addListener(send_rss_links_to_backgroud);
})();
