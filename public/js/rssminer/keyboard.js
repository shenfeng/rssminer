(function () {
  var data = RM.data,
      tmpls = RM.tmpls,
      sc = 'selected',
      to_html = Mustache.to_html;

  var g_pressed = false,
      is_list_show = false;

  function getSelected () { return $('.welcome-list .selected'); }

  function filter_subscription (e) {
    var $selected = $("#subs ." + sc),
        which = (e && e.which) || -1,
        $input = $('#sub-filter'), val = '';
    if(which === 39 || which === 40) {      // left & down
      if($selected.next().length) {
        $selected.removeClass(sc).next().addClass(sc);
      }
    } else if(which === 38 || which === 37) {
      if($selected.prev().length) {
        $selected.removeClass(sc).prev().addClass(sc);
      }
    } else if (which === 13) {  // enter
      if($selected.length) {
        location.hash = "read/" + $selected.attr('data-id');
      }
    } else {
      if (which >= 48 && which <= 122) { // '0' & 'z'
        val = $input.text().trim() + String.fromCharCode(which);
      } else if(which === 8) {  // backspace
        val = $input.text().trim();
        if(val) { val = val.substring(0, val.length - 1); }
      }
      val = val.toLowerCase();
      $input.text(val);
      $("#help, #subs").remove();
      var all = data.get_all_sub_titles(val),
          html = to_html(tmpls.sublist, {subs: all, value: val});
      $('body').append(html);
      $("#subs li:first-child").addClass(sc);
    }
  }

  function openSelected () {
    if($('#help, #subs').length === 0) {
      var $selected = getSelected().find('a');
      if($selected.length) {
        location.hash = $selected.attr('href').substring(1);
      }
    }
  }

  function handleEnter () {
    RM.app.hideHelp();
    openSelected();
  }

  function closeAll () { RM.app.hideHelp(); is_list_show = false; }

  function selecteNextFeed () {
    var $selected = getSelected(),
        $target = $selected.next();
    if(!$target.length) {
      var $s = $selected.siblings();
      $target = $($s[0]);       // select first
    }
    $selected.removeClass(sc);
    $target.addClass(sc);
  }

  function selectPrevFeed () {
    var $selected = getSelected(),
        $target = $selected.prev();
    if(!$target.length) {
      var $s = $selected.siblings();
      $target = $($s[$s.length - 1]); // select last
    }
    $selected.removeClass(sc);
    $target.addClass(sc);
  }

  function selectNextSection () {
    var $selected = getSelected(),
        $this = $selected.closest('.section'),
        $target = $this.next();
    if(!$target.length || !$target.hasClass('section')) {
      var $s = $this.siblings('.section');
      // if has more than one section
      if($s.length) { $target = $($s[0]); } // select first, circle
      else { $target = []; }                // emtpy
    }

    if($target.length ) {
      $selected.removeClass(sc);
      $($('.feed', $target)[0]).addClass(sc);
    }
  }

  function selectPrevSection () {
    var $selected = getSelected(),
        $this = $selected.closest('.section'),
        $target = $this.prev();
    if(!$target.length || !$target.hasClass('section')) {
      var $s = $this.siblings('.section');
      // if has more than one section
      if($s.length) { $target = $($s[$s.length - 1]); }
      else { $target = []; }           // emtpy
    }

    if($target.length ) {
      $selected.removeClass(sc);
      $($('.feed', $target)[0]).addClass(sc);
    }
  }

  function keyupHandler (e) {
    if($("#add-sub, #settings").length) {
      return;
    }
    if(is_list_show) { filter_subscription(e); }
    switch(e.which) {
    case 70:                    // f
      $('#footer').toggle(); RM.layout.reLayout(); break;
    case 79:                    // o
      if(!is_list_show) openSelected(); break;
    case 72:                    // h
      if(!is_list_show && g_pressed && !e.altKey) {
        location.hash = '';
      }
      else if(!is_list_show && e.altKey) { RM.app.save_vote(-1); }
      break;
    case 13:                    // ENTER,  handled by filter_subscription
      closeAll(); break;
    case 27:                    // ESC
      closeAll(); break;
    case 76:                    // l like
      if(!is_list_show && e.altKey) { RM.app.save_vote(1); }; break;
    case 191:                   // ?
      if(e.shiftKey) { RM.app.showHelp(); } break;
    case 83:                    // 83
      if(!is_list_show) { $('#reading-area').toggleClass('show-iframe'); }
      break;
    case 85:                    // u
      if(g_pressed && !is_list_show) {
        is_list_show = true;
        filter_subscription();
      }
      break;
    case 78:                    // n
      if(e.altKey) { RM.layout.adjust(-45); } break;
    case 80:                    // p
      if(e.altKey) { RM.layout.adjust(45); } break;
    }
    if(e.which === 71) { g_pressed = true; } // g
    else { g_pressed = false; }
  }

  function keypressHandler (e) {
    switch(e.which) {
    case 106:                    // j
      selecteNextFeed(); break;
    case 107:                    // k
      selectPrevFeed(); break;
    case 112:                   // p
      selectPrevSection(); break;
    case 110:                   // n
      selectNextSection(); break;
    }
  }

  $(document).keyup(keyupHandler).keypress(keypressHandler);

})();
