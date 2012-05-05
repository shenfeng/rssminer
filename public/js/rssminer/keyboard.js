(function () {
  var data = RM.data,
      tmpls = RM.tmpls,
      sc = 'selected',
      to_html = Mustache.to_html;

  var g_pressed = false;

  function getSelected () { return $('.welcome-list .selected'); }

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

  function closeAll () { RM.app.hideHelp(); }

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
    switch(e.which) {
    case 79:                    // o
      openSelected(); break;
    case 72:                    // h
      if(g_pressed && !e.altKey) {
        location.hash = '';
      } else if(e.altKey) { RM.app.save_vote(-1); }
      break;
    case 13:                    // ENTER,  handled by filter_subscription
      closeAll(); break;
    case 27:                    // ESC
      closeAll(); break;
    case 76:                    // l like
      if(e.altKey) { RM.app.save_vote(1); }; break;
    case 191:                   // ?
      if(e.shiftKey) { RM.app.showHelp(); } break;
    case 83:                    // s
      $('#reading-area').toggleClass('show-iframe');
      break;
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
