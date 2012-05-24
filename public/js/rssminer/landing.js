(function () {
  $('#slides').slides({
    preload: false,
    play: 5000,
    pause: 2500,
    hoverPause: true
  });

  setTimeout(function () {
    var $email = $('#email'),
        $pwd = $('#password');
    if($.trim($email.val())) {
      $pwd.focus();
    } else {
      $email.focus();
    }                           // wait for browser fill form
  }, 800);
})();
