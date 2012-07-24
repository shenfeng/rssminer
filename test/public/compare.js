(function () {

  function bind_event (ele, event, handler) {
    if(ele.addEventListener) {
      ele.addEventListener(event, handler, false);
    } else {                    // ie
      ele.attachEvent('on' + event, handler);
    }
  }
  bind_event(window, 'keydown', function (e) {
    // .wumii-related-items
    if(e.ctrlKey || e.altKey) {
      try {
        var start = parseInt(/\d+/.exec(location.search)[0]) || 0;
      } catch(e) {
        start = 0;
      }

      if(e.keyCode === 39) {    // right
        location.href = "/compare?start=" + (start + 5);
      } else if(e.keyCode === 37) {// left
        location.href = "/compare?start=" + (start - 5);
      }
    }
  });
})();
