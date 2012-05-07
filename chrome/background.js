(function () {
  var API_SERVER = "http://127.0.0.1:9090/api/",
      RSSMINER_ICON = 'imgs/icon.png',
      RSS_ICON = 'imgs/16px-feed-icon.png';

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
    if(data) {
      xhr.send(JSON.stringify(data));
    } else {
      xhr.send();
    }
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

  function change_extension_icon (rss_links) {
    var subscribed = false;
    for(var i = 0; i < rss_links.length; i++) {
      if(user_sub_lists[rss_links[i].href]) {
        subscribed = true;
        break;
      }
    }
    if(rss_links.length) {
      chrome.browserAction.setBadgeText({text: rss_links.length + ''});
    } else {
      // clear BadgeText
      chrome.browserAction.setBadgeText({text:''});
    }
    chrome.browserAction.setIcon({
      path: rss_links.length? RSS_ICON : RSSMINER_ICON
    });
  }

  function show_rssminer_icon (request, sender, sendResponse) {
    if(request.type === 'rss_links') {
      var rss_links = request.data,
          subscribed = is_subscribed(rss_links);
      change_extension_icon(rss_links);
      // Return nothing to let the connection be cleaned up.
      sendResponse({});
    }
  }

  function get_user_subscrptions () {
    get(API_SERVER + 'subs?only_url=1', function (data) { // ok
      for(var i = 0; i < data.length; ++i) {
        user_sub_lists[data[i]] = true;
      }
    }, function (status, xhr) {
      // ignore any error
      if(status === 401) { }
    });                         // fail
  }

  // tab select change
  function tab_activated (info) {
    chrome.tabs.sendRequest(info.tabId, {}, function (response) {
      var rss_links = response.data;
      change_extension_icon(rss_links);
    });
    change_extension_icon([]);
  }

  get_user_subscrptions();      // init

  // set init icon
  chrome.extension.onRequest.addListener(show_rssminer_icon);
  // tab select change
  chrome.tabs.onActivated.addListener(tab_activated);
  // chrome.browserAction.onClicked.addListener(function (tab) {

  //   chrome.browserAction.setPopup({
  //     tabId: tab.id,
  //     popup: Math.random() + ''
  //   });

  // });
})();
