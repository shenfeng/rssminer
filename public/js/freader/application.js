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
  $("#search-input").autocomplete({
    source: "/api/feeds/search"
  });

  freader.init();
});
