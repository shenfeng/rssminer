(function () {

  var rss_links;                // global variable, update

  function send_request_to_content_scripts (data) {
    chrome.tabs.getSelected(null, function(tab) {
      chrome.tabs.sendRequest(tab.id, data,function(response) {
      });
    });
  }

  chrome.browserAction.onClicked.addListener(function(tab) {
    console.log(rss_links);
  });

  // chrome.windows.onFocusChanged.addListener(function () {
  //   console.log('window');
  // });

  chrome.tabs.onSelectionChanged.addListener(function () {
    // console.log('changed');
    // return;
    send_request_to_content_scripts({event: 'onSelectionChanged'});
  });

  // chrome.tabs.onUpdated.addListener(function () {
  //   // send_request_to_content_scripts({event: 'onSelectionChanged'});
  //   // console.log('-----------------------');
  // });

  chrome.extension.onRequest.addListener(
    function(request, sender, sendResponse) {
      // console.log('rss_links received', request.data);
      if(request.type === 'rss_links') {
        rss_links = request.data;
        var icon = "icon.png";
        if(rss_links && rss_links.length) {
          icon = "16px-feed-icon.png";
        }
        chrome.browserAction.setIcon({path: icon});
      }
    });
})();
