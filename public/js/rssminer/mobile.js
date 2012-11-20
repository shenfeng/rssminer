(function () {
  function bind (ele, event, callback) {
    ele.addEventListener(event, callback);
  }

  function css (ele, property, value) {
    var css = ';';
    if (typeof property == 'string') {
      var o = {};
      o[property] = value;
      property = o;
    }

    for (var key in property) {
      if (!property[key] && property[key] !== 0) {
        ele.style.removeProperty(key);
      } else {
        var v = property[key];
        // 'column-count', 'columns', 'font-weight',
        // 'line-height', 'opacity', 'z-index', 'zoom'
        if(typeof v == 'number') {
          v += 'px';
        }
        css += key + ':' + v + ';';
      }
    }
    ele.style.cssText += css;
  }

  var touch = {};
  var wWidth = window.innerWidth;

  function touchstart (e) {
    touch.x1 = e.touches[0].pageX;
    touch.y1 = e.touches[0].pageY;
  }

  function touchmove (e) {
    touch.x2 = e.touches[0].pageX;
    touch.y2 = e.touches[0].pageY;

    if(window.wrapper) {
      wrapper.scrollLeft += touch.x1 - touch.x2;
    }
    console.log(touch.x1 - touch.x2, touch.y1 - touch.y2);
  }

  function touchend (e) {
    console.log('end',touch.x1 - touch.x2, touch.y1 - touch.y2);
  }


  if($test_slides) {
    var $test_slides = document.getElementById('test-slides');
    css($test_slides, 'width', wWidth * 3 + 20);
    for(var i = 0; i < $test_slides.children.length; i++) {
      css($test_slides.children[i], 'width', wWidth);

    }
  }

  bind(document, 'click', function () {
    console.log('click' + window.innerWidth);
  });

  bind(document, 'touchstart', touchstart);
  bind(document, 'touchmove', touchmove);
  bind(document, 'touchend', touchend);
})();