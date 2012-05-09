(function () {
  // document.write(Math.random());
  var tmpls = window.RM.tmpls,
      to_html = Mustache.to_html;

  chrome.tabs.getSelected(null, function(tab) {
    chrome.tabs.sendRequest(tab.id, {}, function(response) {
      if(response.type === 'rss_links' && response.data.length) {
        var links = response.data;
        var html = to_html(tmpls.rss_links, {links: links});
        $('#page-wrap').empty().append(html);

        for(var i = 0; i < links.length; i++) {
          $.get(links[i].href, function (resp) {
            console.log(resp);
          });
        }
      }
    });
  });
})();
