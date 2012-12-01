(function () {
  var input = document.createElement("input");

  function placeholder () {
    var $all = $('[placeholder]'),
        CLS = 'placeholder';

    $all.unbind('.ph');

    $all.each(function (idx, input) {
      var $i = $(input);
      if($i.is(':focus')) { return; }

      var ph = $i.attr('placeholder');

      if(!$.trim($i.val())) {
        $i.addClass(CLS);
        $i.val(ph);
      }
      $i.bind('focus.ph', function (e) {
        if($i.val() === ph) {
          $i.removeClass(CLS);
          $i.val('');
        }
      });
    });
  }

  if(!('placeholder' in input)) {
    window.setInterval(placeholder, 600);
  }

})();