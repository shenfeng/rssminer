(function () {

  function bind_event (ele, event, handler) {
    if(ele.addEventListener) {
      ele.addEventListener(event, handler, false);
    } else {                    // ie
      ele.attachEvent('on' + event, handler);
    }
  }

  var ids = document.querySelectorAll('td.id'),
      last_id = ids[ids.length - 1],
      text = last_id.innerText || last_id.textContent;
  var next_id = parseInt(text);


  console.log(ids);

  bind_event(window, 'keydown', function (e) {
    // console.log(e);
    // .wumii-related-items
    // not work for os x
    // if(e.ctrlKey || e.altKey) {
    try {
      var start = parseInt(/\d+/.exec(location.search)[0]) || 0;
    } catch(e) {
      start = 0;
    }

    if(e.keyCode === 39) {    // right
      location.href = "/compare?start=" + next_id;
    } else if(e.keyCode === 37) {// left
      location.href = "/compare?start=" + (start - 5);
    }
    // }
  });
})();
