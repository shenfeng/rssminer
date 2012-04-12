(function () {
  var ajax = RM.ajax,
      plot = RM.plot,
      $ = window.$;

  var $content = $("#content");

  function toggleService (e) {
    var $tr = $(this).parents('tr'),
        section = $tr.attr('data-sid'),
        command = $.trim($tr.find('.status').text()) === 'false'
          ? 'start' : 'stop';
    ajax.jpost("/api/dashboard", {which: section, command: command});
  }

  window.RM.util.delegate_events($content, {
    "click #controls button": toggleService
  });

  $("#refresh-now").click(showSettings);

  function showSettings () {
    ajax.get("/api/dashboard/stat", function (data) {
      var resp = plot.parse(data),
          html = window.Mustache.to_html(window.RM.tmpls.settings, resp);
      $("#content").empty().append(html);
      plot.plot();

      var interval = (+ $("#interval").val());
      if(interval > 0) {
        setTimeout(showSettings, interval * 1000);
      }
    });
  }

  showSettings();
})();
