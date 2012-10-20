(function () {
  var current_element = 0;

  var $tooltip = $('#tooltip'),
      $text = $tooltip.find('span');

  var timer;

  function on_mouseenter (ele, e) {
    var $this = $(ele),
        pos = $this.offset();

    $text.text($this.attr('data-title'));

    pos.top += $this.height() + 8;
    pos.left = e.clientX;
    $tooltip.css(pos).show();

    if(timer) { clearTimeout(timer); }
    timer = setTimeout(on_mouseleave, 3400);
  }

  function on_mouseleave (ele) {
    $tooltip.hide();
  }

  $(document).delegate('[data-title]', 'hover', function (e) {
    if(e.type === 'mouseenter') {
      on_mouseenter(this, e);
    } else {
      on_mouseleave(this);
    }
  });
})();
