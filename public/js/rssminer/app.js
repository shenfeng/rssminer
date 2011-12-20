(function () {
  var ajax = RM.ajax,
      utils = RM.util;

  function layout() {
    $("#main, #main>div").height($(window).height() - $('#head').height());
  }

  $(window).resize(_.debounce(layout, 100));

  var reseted = ["wordpress", "blogspot"],
      $left_nav = $("#left"),
      $mid = $("#mid"),
      $right = $("#right .wrapper");

  function rerender_nav (id) {
    $left_nav.empty().append(RM.render_nav());
  }

  function add_selected_cls (context, id) {
    $('.selected', context).removeClass('selected');
    $("#" + id).addClass('selected');
  }

  function rerender_mid (sub_id) {
    $mid.empty().append(RM.render_mid());
    if(sub_id) {
      add_selected_cls($left_nav, "sub-" + sub_id);
    }
  }


  function index () { }

  function showSub (id) {
    ajax.get("/api/subs/" + id, function (data) {
      if(data) {
        _FEEDS_ = data;
        rerender_mid(id);
      }
    });
  }

  function rewrite_img_src (index, img) {
    var $img = $(img),
        src = $img.attr('src'),
        hostname = utils.hostname(src);
    for(var i = 0; i < reseted.length; i++) {
      if(hostname.indexOf(reseted[i]) != -1) {
        $img.attr('src', '/p/' + src); // rewrite reseted images
        break;
      }
    }
  }

  function add_target_blank (index, a) {
    $(a).attr("target", "_blank");
  }

  function showFeed (sub_id, feed_id) {
    if(!$("#sub-" + sub_id).hasClass('selected')) {
      showSub(sub_id);
    }
    ajax.get("/api/feeds/" + feed_id, function (data) {
      if(data) {
        var $html = $(RM.render_right(data));
        $("img", $html).each(rewrite_img_src);
        $("a", $html).each(add_target_blank);
        $right.html($html);
        add_selected_cls($mid, "feed-" + feed_id);
        $("#right").scrollTop(0);
      }
    });
  }

  (function () {                        // init
    layout();
    rerender_nav();
    utils.hashRouter({
      '': index,
      'subs/:id': showSub,
      'subs/:sub_id/:feed_id':showFeed
    });
    utils.delegateEvents($right, {
      'click #controls li': function (e) {
        var $this = $(this),
            text = $.trim($this.text()).toLowerCase();
        $('#tabs .tab').removeClass('selected');
        if(text === 'rss') {
          $("#tabs .tab:first").addClass('selected');
        } else {
          $("#tabs .tab:nth(1)").addClass('selected');
        }
      }
    });
  })();

})();
