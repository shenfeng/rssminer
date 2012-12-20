{{#dev}}
<script>
(function () {
  if(!WebSocket) {return; }
  var TOP = '_r_s_top', conn = new WebSocket("ws://{{server-host}}/dev/ws");
  conn.onmessage = function (e) {
    localStorage.setItem(TOP, JSON.stringify([window.scrollX, window.scrollY]));
    location.reload(true);
  };

  window.onload = function () {
    if(localStorage.getItem(TOP)) {
      var d = JSON.parse(localStorage.getItem(TOP));
      window.scrollTo(d[0], d[1]);
    }
    localStorage.removeItem(TOP);
   };
   conn.onopen = function (e) {
     console.log("reload connected");
   };
})();
</script>
{{/dev}}
