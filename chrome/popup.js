(function () {
  // document.write(Math.random());

  chrome.tabs.getSelected(null, function(tab) {
    chrome.tabs.sendRequest(tab.id, {}, function(response) {
      if(response.type === 'rss_links') {
        document.write(response.data.length);
        console.log(response);
      }
    });
  });

})();
