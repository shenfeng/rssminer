$(function () {
  
  $(".nav-tree > li").clone();

  _.each(_.range(1,20), function(i){
    // $(".nav-tree").append($(".nav-tree > li:first").clone());
  });

  _.each(_.range(1,69), function(i){
    // $("#entries").append($("#entries > li:first").clone());
  });

  $(".folder .toggle").click(function(){
    $(this).parents('.folder').toggleClass('collapsed');
  });

  $(".nav-tree a").click(function(){
    $(".nav-tree a").not(this).removeClass('selected');
    $(this).toggleClass('selected');
  });

  $(".collapsed .entry-main").click(function () {
    var $entry =  $(this).parents(".entry");
    $(".entry").not($entry).removeClass('expanded');
    $entry.toggleClass("expanded");
    $("#entries").scrollTop($entry.offset().top - $(".entry:first").offset().top);
  });

  function layout() {
    var $entries = $("#entries"),
        $nav_tree = $(".nav-tree");
    $entries.height($(window).height() - $entries.offset().top - 20);
    $nav_tree.height($(window).height() - $nav_tree.offset().top - 20);
  }

  $(window).resize(_.debounce(layout, 100));
  var keydownHandler = (function (){
      var $kb = $(".overlay, #keyboard-shortcut"),
          $current,
          set_current = function ($c) {
            $(".entry").not($c).removeClass('current');
            $c.toggleClass("current");
            var $f = $(".entry:first"),
                toTop = $c.offset().top - $f.offset().top,
                $entries = $("#entries"),
                scrollTop = $entries.scrollTop();
            if(toTop < scrollTop)
              $entries.scrollTop(toTop-$c.height());
            $current = $c;
          };
      return function(e) {
        var $first = $(".entry:first"),
            $last = $(".entry:last");
        $current || ($current = $first);
        if( $kb.is(":visible") && 
            (e.which === 27 || (e.which === 191 && e.shiftKey))){ // ? or esc
            $kb.hide();
        }
        else if(e.which === 191 && e.shiftKey ){ // ?
          $(".overlay, #keyboard-shortcut").show();
        } else if( e.which === 74) { // j
          var $before = $current.prev().length === 0 ? $first : $current.prev();
          set_current($before);
        } else if( e.which === 75) { // k
          var $next = $current.next().lenght === 0 ? $last : $current.next();
          set_current($next);
        } else if( e.which === 79) { // o
          $(".collapsed .entry-main", $current).click();
        }
      };
  })();
  $(window).keydown(keydownHandler);
  layout();
});
