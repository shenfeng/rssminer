$(function(){
  function layout() {
    $("#main, #main>div").height($(window).height() - $('#head').height());
  }

  $(window).resize(_.debounce(layout, 100));
  layout();
});
