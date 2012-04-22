(function () {
  var API_SERVER = "http://rssminer.net/api/",
      SUBSCRIBED_ICON = 'icon.png',
      UN_SUBSCRIBED_ICON = '16px-feed-icon.png';

  // gloal cache, which link is subscribed
  var user_sub_lists = {};

  function ajax (method, url, data, success, fail) {
    if(typeof data === 'function') {
      fail = success;           // shift
      success = data;
      data = null;
    }
    var xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function () {
      if(xhr.readyState === 4) {
        if(xhr.status === 200 && typeof success === 'function') {
          success(JSON.parse(xhr.responseText));
        } else if(typeof fail === 'function') {
          fail(xhr.status, xhr);
        }
      }
    };
    xhr.open(method, url, true);
    try {
      xhr.setRequestHeader("X-Requested-With","XMLHttpRequest");
    } catch(_){}
    if(data) { xhr.send(JSON.stringify(data)); } else { xhr.send(); }
    return xhr;
  }

  function post (url, data, success, fail) {
    return ajax('POST', url, data, success, fail);
  }

  function get (url, success, fail) {
    return ajax('GET', url, success, fail);
  }

  function is_subscribed (rss_links) {
    for(var i = 0; i < rss_links.length; i++) {
      if(user_sub_lists[rss_links[i].href]) {
        return true;
      }
    }
    return false;
  }

  function pageaction_clicked (tab) {
    chrome.tabs.sendRequest(tab.id, {}, function (response) {
      var rss_links = response.data;
      if(!is_subscribed(rss_links)) {
        var href = rss_links[0].href;
        post(API_SERVER + 'subs/add',{link: href}, function (data) {
          user_sub_lists[href] = true;
          show_icon(tab.id, true);
        }, function (status, xhr) {
          // TODO guide user logined or regester
        });
      }
    });
  }

  function show_icon (tabid, subscribed) {
    chrome.pageAction.setIcon({
      path: subscribed? SUBSCRIBED_ICON : UN_SUBSCRIBED_ICON,
      tabId: tabid
    });
    chrome.pageAction.show(tabid);
  }

  function show_rssminer_icon (request, sender, sendResponse) {
    if(request.type === 'rss_links') {
      var rss_links = request.data,
          subscribed = is_subscribed(rss_links);
      show_icon(sender.tab.id, subscribed);
      // Return nothing to let the connection be cleaned up.
      sendResponse({});
    }
  }

  function get_user_subscrptions () {
    get(API_SERVER + 'subs', function (data) { // ok
      for(var i = 0; i < data.length; ++i) {
        user_sub_lists[data[i]] = true;
      }
    }, function (status, xhr) {
      // ignore any error
      if(status === 401) { }
    });                         // fail
  }

  chrome.pageAction.onClicked.addListener(pageaction_clicked);
  chrome.extension.onRequest.addListener(show_rssminer_icon);
  get_user_subscrptions();      // init

})();
