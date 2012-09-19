(function () {
  var current_element = 0;

  var $tooltip = $('#tooltip'),
      $text = $tooltip.find('span');

  function on_mouseenter (ele, e) {
    var $this = $(ele),
        pos = $this.offset();

    $text.text($this.attr('data-title'));

    pos.top += $this.height() + 8;
    pos.left = e.clientX;
    $tooltip.css(pos).show();
  }

  function on_mouseleave (ele) {
    $tooltip.hide();
    // console.log('leave', $ele);
  }

  $(document).delegate('[data-title]', 'hover', function (e) {
    if(e.type === 'mouseenter') {
      on_mouseenter(this, e);
    } else {
      on_mouseleave(this);
    }
  });
})();
