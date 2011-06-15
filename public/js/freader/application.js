window.$(function () {

  var freader = window.Freader,
      $ = window.$,
      _ = window._;

  function layout() {
    var $entries = $('#entries'),
        $nav_tree = $('.nav-tree');
    if($entries.length > 0) {
      $entries.height($(window).height() - $entries.offset().top - 20);
    }
    if($nav_tree.length > 0) {
      $nav_tree.height($(window).height() - $nav_tree.offset().top - 20);
    }
  }

  $(window).resize(_.debounce(layout, 100));

  (function() {
    var $form = $('#add-subscription .form'),
        $input = $('input',$form);

    $input.blur(function(){
      $form.hide();
    });
    $('#add-subscription span').click(function() {
      $form.show();
      $input.focus();
    });
    $form.keydown(function(e) {
      if(e.which === 13) {
        freader.magic.addSubscription($input.val());
        $form.hide();
        $input.val('');
      }
    });
  })();

  freader.init();
});
