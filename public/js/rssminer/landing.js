(function () {
  function f_input() {
    var email = document.getElementById('email');
    try {
      if(email.value) {
        password.focus();
      } else {
        email.focus();
      }
    }catch(e) {}
  }

  login_btn.onclick = function(e) {
    if (!e) e = window.event;
    if (login.className) {
      login.className = '';
    } else {
      login.className = 'show-form';
      f_input();
    }
    if(e.preventDefault) {
      e.preventDefault();
    } else {
      e.returnValue = false;
    }
  };
  f_input();
})();
